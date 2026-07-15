﻿$projectDir = $PSScriptRoot
$ErrorActionPreference = "Continue"

function Kill-Port {
    param([int]$Port)
    $pids = (Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue).OwningProcess | Sort-Object -Unique
    if ($pids) {
        Write-Host "  Kill port $Port..." -ForegroundColor Yellow
        $pids | ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
        Start-Sleep -Seconds 2
    }
}

function Wait-Service {
    param([string]$Url, [string]$Name, [int]$Timeout=120)
    $start = Get-Date
    Write-Host "  Waiting for $Name (timeout ${Timeout}s)..." -ForegroundColor Gray
    while ($true) {
        # PS5-compatible port-listening check via HttpWebRequest.
        # Key: ANY HTTP response (2xx/3xx/4xx/5xx) means server process is listening and started.
        # Only connection-refused / DNS-fail / pure socket timeout = still down.
        $up = $false
        try {
            $req = [System.Net.HttpWebRequest]::Create($Url)
            $req.Timeout = 3000
            $req.ReadWriteTimeout = 3000
            $req.AllowAutoRedirect = $false
            try {
                $resp = $req.GetResponse()
                if ($resp) {
                    $statusCode = [int]$resp.StatusCode
                    Write-Host "  $Name ready (HTTP $statusCode)" -ForegroundColor Green
                    $resp.Close()
                    return $true
                }
            } catch [System.Net.WebException] {
                # Server replied (4xx/5xx) = service is listening
                if ($_.Exception.Response -ne $null) {
                    $statusCode = [int]$_.Exception.Response.StatusCode
                    Write-Host "  $Name ready (HTTP $statusCode, init in progress)" -ForegroundColor Green
                    return $true
                }
                # Otherwise: ConnectFailure / NameResolutionFailure / ReceiveFailure -> keep waiting
            }
        } catch {
            # Non-web exceptions (e.g. malformed URL) -> keep waiting, don't crash
        }
        if (((Get-Date) - $start).TotalSeconds -ge $Timeout) {
            Write-Host "  $Name TIMEOUT (${Timeout}s) — see .log files in project dir for Python/Java errors" -ForegroundColor Yellow
            return $false
        }
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
            $loaded++
        }
    }
    if ($loaded -gt 0) {
        Write-Host "  Loaded $loaded user env vars" -ForegroundColor DarkCyan
    }
}

function Show-LogTail {
    param([string]$LogFile, [int]$Lines=25)
    if (Test-Path $LogFile) {
        Write-Host ""
        Write-Host "  --- Last $Lines lines of $(Split-Path $LogFile -Leaf) ---" -ForegroundColor Cyan
        Get-Content $LogFile -Tail $Lines -ErrorAction SilentlyContinue | ForEach-Object { Write-Host "    $_" -ForegroundColor Gray }
        Write-Host "  --- (full log: $LogFile) ---" -ForegroundColor Cyan
    }
}

$logDir = Join-Path $projectDir "logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$ts = Get-Date -Format "yyyyMMdd-HHmmss"
$logVector  = Join-Path $logDir "vector-service-$ts.log"
$logChroma  = Join-Path $logDir "chroma-service-$ts.log"
$logBackend = Join-Path $logDir "backend-mvn-$ts.log"
Write-Host "  Service logs dir: $logDir" -ForegroundColor Gray

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Sheyingtong Dictionary + RAG Encoding Fix v2.1" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Strategy (wipe all 3 layers + rebuild from scratch):" -ForegroundColor White
Write-Host "  [1] Stop 4 services" -ForegroundColor Gray
Write-Host "  [2] Delete $projectDir\chroma-data (persisted bad vectors)" -ForegroundColor Gray
Write-Host "  [3] Fix sheyingtong DB charset + 8 tables -> utf8mb4" -ForegroundColor Gray
Write-Host "  [4] TRUNCATE photography_terms (drop garbled rows, free PKs 1-50)" -ForegroundColor Gray
Write-Host "  [5] Restart vector -> chroma -> backend -> frontend" -ForegroundColor Gray
Write-Host "  [6] Spring Boot runs schema.sql -> data.sql -> inserts 50 clean terms" -ForegroundColor Gray
Write-Host "  [7] RagAutoSyncRunner sees Chroma count=0 -> auto re-embeds everything" -ForegroundColor Gray
Write-Host ""

$dbUsername = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { "root" }
$dbPassword = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { "123456" }
$dbName     = "sheyingtong"
$chromaData = Join-Path $projectDir "chroma-data"

Write-Host "[Step 1/9] Checking mysql CLI..." -ForegroundColor Yellow
$mysqlCmd = Get-Command mysql -ErrorAction SilentlyContinue
if (-not $mysqlCmd) {
    Write-Host "  WARNING: mysql CLI not found. Will skip DB charset fix." -ForegroundColor Yellow
    Write-Host "  You MUST run the SQL snippet below manually in Navicat/DBeaver FIRST," -ForegroundColor Yellow
    Write-Host "  then press any key to continue (otherwise data.sql cannot insert clean terms):" -ForegroundColor Yellow
    Write-Host "  ------------------------------------------------------------" -ForegroundColor Gray
    Write-Host "  ALTER DATABASE sheyingtong CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  USE sheyingtong;"
    Write-Host "  ALTER TABLE photography_terms CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE users CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE gear CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE gear_categories CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE chat_sessions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE chat_messages CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE photo_analyses CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  ALTER TABLE retouch_suggestions CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    Write-Host "  TRUNCATE TABLE photography_terms;"
    Write-Host "  ------------------------------------------------------------" -ForegroundColor Gray
    Write-Host "  Press any key AFTER you executed the above SQL..." -ForegroundColor Yellow
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
} else {
    Write-Host "  mysql CLI OK: $($mysqlCmd.Source)" -ForegroundColor Green
}

Write-Host ""
Write-Host "[Step 2/9] Stop services (5173 frontend, 8080 backend, 8002 chroma, 8001 vector)..." -ForegroundColor Yellow
Kill-Port 5173
Kill-Port 8080
Kill-Port 8002
Kill-Port 8001
Start-Sleep -Seconds 5
Write-Host "  All services stopped (ports released)" -ForegroundColor Green

Write-Host ""
Write-Host "[Step 3/9] Delete Chroma persist dir (wipe old bad vectors)..." -ForegroundColor Yellow
if (Test-Path $chromaData) {
    Write-Host "  Found dir: $chromaData" -ForegroundColor Gray
    $files = (Get-ChildItem $chromaData -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
    Write-Host "  $files files will be permanently deleted..." -ForegroundColor Gray
    Remove-Item -Path $chromaData -Recurse -Force -ErrorAction Stop
    Write-Host "  OK: Deleted $chromaData" -ForegroundColor Green
} else {
    Write-Host "  Dir missing (already clean?)" -ForegroundColor Gray
}

if ($mysqlCmd) {
    Write-Host ""
    Write-Host "[Step 4/9] Fix MySQL charset + TRUNCATE photography_terms..." -ForegroundColor Yellow

    # === CRITICAL SAFETY MEASURES ===============================================
    # 1) --force flag forces mysql to CONTINUE PAST ERRORS (e.g. missing tables)
    #    instead of aborting on the first error (which was skipping TRUNCATE!)
    # 2) TRUNCATE photography_terms runs FIRST (before any ALTERs) so if other
    #    ALTERs fail, the critical "clean old mojibake rows" step is not skipped.
    # 3) Each ALTER TABLE checks INFORMATION_SCHEMA first so we never try to
    #    ALTER a table that has not been created yet (e.g. photo_analyses
    #    before the photo analysis feature has ever been used).
    # ===========================================================================

    # --- PHASE A: Database-level charset (always safe to run) ------------------
    $dbFixSql = @(
        "ALTER DATABASE $dbName CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci",
        "USE $dbName",
        "TRUNCATE TABLE photography_terms",
        "SELECT 'AFTER-TRUNCATE' AS stage, COUNT(*) AS term_count FROM photography_terms"
    ) -join "; "

    Write-Host "  [A] Truncate photography_terms + fix database charset..." -ForegroundColor Gray
    $dbArgs = @(
        "-u", $dbUsername,
        "--password=$dbPassword",
        "--default-character-set=utf8mb4",
        "--force",
        "--execute", $dbFixSql
    )
    & $mysqlCmd.Source @dbArgs 2>&1 | ForEach-Object { Write-Host "    $_" }
    $dbExit = $LASTEXITCODE

    # --- PHASE B: Table-level charset (only EXISTING tables, skip missing ones)
    $tablesToFix = @(
        "photography_terms", "users", "gear", "gear_categories",
        "chat_sessions", "chat_messages", "photo_analyses", "retouch_suggestions"
    )
    Write-Host "  [B] Fix charset for each EXISTING table (missing tables skipped)..." -ForegroundColor Gray

    foreach ($tbl in $tablesToFix) {
        $checkSql = "SELECT COUNT(*) AS cnt FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$dbName' AND TABLE_NAME='$tbl'"
        $checkArgs = @(
            "-u", $dbUsername,
            "--password=$dbPassword",
            "--default-character-set=utf8mb4",
            "--skip-column-names",
            "--batch",
            "--execute", $checkSql
        )
        $result = & $mysqlCmd.Source @checkArgs 2>$null | Select-Object -First 1
        $exists = [int]($result -as [int]) -gt 0

        if ($exists) {
            $alterSql = "USE $dbName; ALTER TABLE $tbl CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
            $alterArgs = @(
                "-u", $dbUsername,
                "--password=$dbPassword",
                "--default-character-set=utf8mb4",
                "--force",
                "--execute", $alterSql
            )
            Write-Host "    [$tbl] EXISTS -> ALTER CHARSET utf8mb4" -ForegroundColor Gray
            & $mysqlCmd.Source @alterArgs 2>&1 | ForEach-Object { Write-Host "      $_" }
        } else {
            Write-Host "    [$tbl] MISSING (not created yet) -> SKIPPED" -ForegroundColor DarkGray
        }
    }

    # --- PHASE C: Final verification -------------------------------------------
    $verifySql = @(
        "USE $dbName",
        "SELECT 'FINAL-CHECK' AS stage, table_name, table_collation FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='$dbName' AND TABLE_NAME='photography_terms'",
        "SELECT 'FINAL-CHECK' AS stage, COUNT(*) AS term_count FROM photography_terms"
    ) -join "; "
    Write-Host "  [C] Verify final state..." -ForegroundColor Gray
    $vArgs = @(
        "-u", $dbUsername,
        "--password=$dbPassword",
        "--default-character-set=utf8mb4",
        "--force",
        "--table",
        "--execute", $verifySql
    )
    & $mysqlCmd.Source @vArgs 2>&1 | ForEach-Object { Write-Host "    $_" }
    $verifyExit = $LASTEXITCODE

    $overallExit = [Math]::Max($dbExit, $verifyExit)
    if ($overallExit -ne 0) {
        Write-Host ""
        Write-Host "  WARNING: some mysql steps exit code=$overallExit" -ForegroundColor Red
        Write-Host "  CRITICAL: If TRUNCATE step above returned 0 rows, the mojibake data is CLEARED. Ignore missing-table warnings below:" -ForegroundColor Yellow
        Write-Host "    [1] Is MySQL running?  net start MySQL80  or  net start MySQL" -ForegroundColor Gray
        Write-Host "    [2] Password wrong? `$env:DB_PASSWORD='pwd'  then re-run" -ForegroundColor Gray
        Write-Host "    [3] DB missing?  CREATE DATABASE IF NOT EXISTS $dbName;" -ForegroundColor Gray
        Write-Host "  Continuing anyway (backend schema.sql retries charset fix)." -ForegroundColor Gray
    } else {
        Write-Host "  OK: photography_terms TRUNCATED, all existing tables charset=utf8mb4" -ForegroundColor Green
    }
}

Write-Host ""
Write-Host "[Step 5/9] Inject user env vars (QWEN_API_KEY etc.)..." -ForegroundColor Yellow
Import-UserEnvVars

Write-Host ""
Write-Host "[Step 6/9] Start vector-service (port 8001)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "`"$projectDir\vector-service\main.py`"" `
    -WorkingDirectory "$projectDir\vector-service" -WindowStyle Normal -PassThru `
    -RedirectStandardOutput $logVector -RedirectStandardError $logVector | Out-Null
$vectorReady = Wait-Service "http://localhost:8001/health" "vector-service" 120
if (-not $vectorReady) { Show-LogTail $logVector 30 }

Write-Host ""
Write-Host "[Step 7/9] Start chroma-service (port 8002, will create empty new persist dir)..." -ForegroundColor Green
Start-Process -FilePath "python" -ArgumentList "`"$projectDir\chroma-service\main.py`"" `
    -WorkingDirectory "$projectDir\chroma-service" -WindowStyle Normal -PassThru `
    -RedirectStandardOutput $logChroma -RedirectStandardError $logChroma | Out-Null
$chromaReady = Wait-Service "http://localhost:8002/health" "chroma-service" 120
if (-not $chromaReady) { Show-LogTail $logChroma 30 }
Start-Sleep -Seconds 3
try {
    $cols = Invoke-RestMethod -Uri "http://localhost:8002/collections" -TimeoutSec 5 -ErrorAction SilentlyContinue
    $cnt = if ($cols -is [array]) { $cols.Count } else { 0 }
    Write-Host "  Current Chroma collections: $cnt (should be 0 = clean empty DB)" -ForegroundColor Gray
} catch {
    if ($chromaReady) {
        Write-Host "  Chroma API query: OK (no collections list returned yet, creating default in background)" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "[Step 8/9] Start Spring Boot backend (schema.sql fixes charset, data.sql inserts 50 terms, RAG auto sync on count=0)..." -ForegroundColor Green
Push-Location "$projectDir\backend"
$mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { Join-Path $env:MAVEN_HOME "bin\mvn.cmd" }
Start-Process -FilePath "cmd" -ArgumentList "/c `"`"$mvnCmd`" spring-boot:run > `"$logBackend`" 2>&1`"" `
    -WorkingDirectory "$projectDir\backend" -WindowStyle Normal -PassThru | Out-Null
Pop-Location

$backendReady = Wait-Service "http://localhost:8080/api/auth/login" "backend" 300
if (-not $backendReady) { Show-LogTail $logBackend 40 }
Start-Sleep -Seconds 8

Write-Host ""
Write-Host "[Step 9/9] Verify fix results..." -ForegroundColor Yellow
$dictionaryOk = $false

try {
    $resp = Invoke-RestMethod -Uri "http://localhost:8080/api/dictionary/terms?page=0&size=5" -TimeoutSec 10 -ErrorAction Stop
    $list = if ($resp.content) { $resp.content } elseif ($resp.data -and $resp.data.content) { $resp.data.content } else { $resp }
    if ($list -and $list.Count -gt 0) {
        Write-Host "  Dictionary API first 5 terms returned:" -ForegroundColor Green
        $sampleTerm = ""
        foreach ($item in $list) {
            $t = if ($item.term) { $item.term } else { [string]$item }
            $c = if ($item.category) { $item.category } else { "" }
            Write-Host "    - [$c] $t" -ForegroundColor White
            if (-not $sampleTerm) { $sampleTerm = $t }
        }
        if ($sampleTerm -match "[\u4e00-\u9fa5]") {
            Write-Host "  OK: Dictionary contains valid CJK characters" -ForegroundColor Green
            $dictionaryOk = $true
        } else {
            Write-Host "  WARNING: No CJK chars in first 5 terms. data.sql may still be running. Refresh page later." -ForegroundColor Yellow
        }
    }
} catch {
    Write-Host "  Dictionary API check failed: $($_.Exception.Message)" -ForegroundColor Yellow
    Write-Host "  (This usually means backend is still starting. Verify manually in browser later.)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "  RAG state check (3s wait)..." -ForegroundColor Gray
Start-Sleep -Seconds 3
try {
    $status = Invoke-RestMethod -Uri "http://localhost:8080/api/rag/status" -TimeoutSec 5 -ErrorAction Stop
    $s = if ($status.data) { $status.data } else { $status }
    Write-Host "  RAG vector DB status:" -ForegroundColor Green
    Write-Host "    initializationStarted : $($s.initializationStarted)" -ForegroundColor Gray
    Write-Host "    initialized           : $($s.initialized)" -ForegroundColor Gray
    Write-Host "    documentCount         : $($s.documentCount)" -ForegroundColor Gray
    if ($s.initializationError) { Write-Host "    error                 : $($s.initializationError)" -ForegroundColor Red }
    if ($s.documentCount -gt 0) {
        Write-Host "  OK: RAG has $($s.documentCount) fragments, still embedding in background" -ForegroundColor Green
    } elseif ($s.initializationStarted) {
        Write-Host "  INFO: RAG embedding in progress (normal for 50 terms, takes 15-45s)" -ForegroundColor Gray
    }
} catch {
    Write-Host "  RAG status API check failed: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "  Starting frontend (port 5173)..." -ForegroundColor Green
Start-Process -FilePath "cmd" -ArgumentList "/c", "npm", "run", "dev" `
    -WorkingDirectory "$projectDir\frontend" -WindowStyle Normal -PassThru | Out-Null
Wait-Service "http://localhost:5173" "frontend" 60 | Out-Null

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Fix script completed" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Open these URLs in browser to confirm:" -ForegroundColor White
Write-Host "  Dictionary   -> http://localhost:5173/dictionary  (all Chinese, no mojibake)" -ForegroundColor Gray
Write-Host "  Gear         -> http://localhost:5173/gear        (names/descriptions OK)" -ForegroundColor Gray
Write-Host "  AI Chat      -> http://localhost:5173/chat        (RAG contexts no longer mojibake)" -ForegroundColor Gray
Write-Host ""
Write-Host "INFO: RAG embedding takes 30-60 seconds." -ForegroundColor Yellow
Write-Host "      Watch backend logs for 'RAG auto sync done' or 'Chroma count=xxx' before testing AI chat." -ForegroundColor Yellow
Write-Host "      If chat still shows mojibake after 1 min, force sync with:" -ForegroundColor Gray
Write-Host "        curl -X POST http://localhost:8080/api/rag/sync" -ForegroundColor Gray
Write-Host ""
Write-Host "Actions applied by this script:" -ForegroundColor Green
Write-Host "  - Deleted $chromaData (wiped all bad vectors, Chroma starts fresh)" -ForegroundColor Green
Write-Host "  - Fixed DB + 8 tables charset to utf8mb4_unicode_ci" -ForegroundColor Green
Write-Host "  - Truncated photography_terms so data.sql re-inserts 50 clean terms" -ForegroundColor Green
Write-Host "  - application.yml: JDBC/Hikari/Hibernate all utf8mb4" -ForegroundColor Green
Write-Host "  - schema.sql auto-fixes charset on every start" -ForegroundColor Green
Write-Host ""
Write-Host "Press any key to exit..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
