$ErrorActionPreference = "Continue"

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

# RabbitMQ（docker compose 起的 broker 一并收掉）
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Write-Host "停止 RabbitMQ (docker compose)..." -ForegroundColor Yellow
    try {
        docker compose down
        Write-Host "  RabbitMQ 已停止" -ForegroundColor Green
    } catch {
        Write-Host "  docker compose down 失败（可能 broker 未由本脚本启动）" -ForegroundColor Yellow
    }
} else {
    Write-Host "docker 未安装，跳过 RabbitMQ 停止" -ForegroundColor Gray
}

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  所有服务已停止!" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor Yellow
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
