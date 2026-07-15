﻿﻿﻿$projectDir = $PSScriptRoot
$ErrorActionPreference = "Continue"

function Kill-Port {
    param([int]$Port)
    $pids = (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique
    if ($pids) {
        Write-Host "Kill port $Port..." -ForegroundColor Yellow
        $pids | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 2
    }
}

function Wait-Service {
    param([string]$Url, [string]$Name, [int]$Timeout=60)
    $start = Get-Date
    Write-Host "  Waiting for $Name..." -ForegroundColor Gray
    while ($true) {
        try {
            $r = Invoke-RestMethod -Uri $Url -TimeoutSec 3 -ErrorAction SilentlyContinue
            if ($r) { Write-Host "  $Name ready" -ForegroundColor Green; return }
        } catch {}
        if (((Get-Date) - $start).TotalSeconds -ge $Timeout) { Write-Host "  $Name timeout, continue..." -ForegroundColor Yellow; return }
        Start-Sleep -Seconds 3
    }
}

function Import-UserEnvVars {
    $vars = @("QWEN_API_KEY", "DB_URL", "DB_USERNAME", "DB_PASSWORD", "OPENAI_API_KEY", "OPENAI_BASE_URL")
    $loaded = 0
    foreach ($name in $vars) {
        $val = [Environment]::GetEnvironmentVariable($name, "User")
        if ($val -and $val.Length -gt 0) {
            Set-Item -Path "env:$name" -Value $val
            $mask = if ($val.Length -le 6) { "***" } else { $val.Substring(0, 6) + "..." }
            Write-Host "  Loaded $name = $mask" -ForegroundColor DarkCyan
            $loaded++
        }
    }
    if ($loaded -gt 0) {
        Write-Host "  Total $loaded user env vars injected into current process" -ForegroundColor DarkCyan
    } else {
        Write-Host "  No user env vars found (run Set-Item env:QWEN_API_KEY 'sk-...' manually for this session)" -ForegroundColor Yellow
    }
}

Write-Host "=== Starting Sheyingtong Services ===" -ForegroundColor Cyan

Write-Host "Loading user environment variables..." -ForegroundColor Yellow
Import-UserEnvVars

Write-Host "Cleaning ports..." -ForegroundColor Yellow
Kill-Port 8001; Kill-Port 8002; Kill-Port 8080; Kill-Port 5173

Write-Host "`n[1/4] Starting vector-service (8001)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "$projectDir\vector-service\main.py" -WorkingDirectory "$projectDir\vector-service" -WindowStyle Normal -PassThru
Wait-Service "http://localhost:8001/health" "vector-service" 120

Write-Host "`n[2/4] Starting chroma-service (8002)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "$projectDir\chroma-service\main.py" -WorkingDirectory "$projectDir\chroma-service" -WindowStyle Normal -PassThru
Wait-Service "http://localhost:8002/collections" "chroma-service" 30

Write-Host "`n[3/4] Starting Java backend (8080)..." -ForegroundColor Green
Start-Process -FilePath "cmd" -ArgumentList "/c", "mvn", "spring-boot:run" -WorkingDirectory "$projectDir\backend" -WindowStyle Normal -PassThru
Wait-Service "http://localhost:8080/api/auth/login" "backend" 120

Write-Host "`n[4/4] Starting Vue frontend (5173)..." -ForegroundColor Green
Start-Process -FilePath "cmd" -ArgumentList "/c", "npm", "run", "dev" -WorkingDirectory "$projectDir\frontend" -WindowStyle Normal -PassThru

Write-Host "`n=== All services started ===" -ForegroundColor Cyan
Write-Host "`nServices:" -ForegroundColor White
Write-Host "  vector:   http://localhost:8001" -ForegroundColor Gray
Write-Host "  chroma:   http://localhost:8002" -ForegroundColor Gray
Write-Host "  backend:  http://localhost:8080" -ForegroundColor Gray
Write-Host "  frontend: http://localhost:5173" -ForegroundColor Gray
Write-Host "`nPress any key to exit..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")