﻿﻿﻿$ErrorActionPreference = "Continue"

function Stop-Port {
    param(
        [int]$Port,
        [string]$ServiceName
    )
    try {
        $processes = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
        if ($processes) {
            Write-Host "停止 $ServiceName (端口 $Port)..." -ForegroundColor Yellow
            foreach ($processId in $processes) {
                Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
                Write-Host "  已终止进程 PID: $processId" -ForegroundColor Green
            }
            Start-Sleep -Seconds 1
        } else {
            Write-Host "$ServiceName (端口 $Port) 未运行" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "停止 $ServiceName 时出错: $_" -ForegroundColor Red
    }
}

Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  摄影通项目一键停止脚本" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Stop-Port -Port 5173 -ServiceName "Vue frontend"
Stop-Port -Port 8080 -ServiceName "Java backend"
Stop-Port -Port 8002 -ServiceName "chroma-service"
Stop-Port -Port 8001 -ServiceName "vector-service"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  所有服务已停止!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
