param([switch]$NoBuild,[switch]$ExternalDatabase)
. "$PSScriptRoot\env.ps1"
Set-Location -LiteralPath $ProjectRoot
$PidFile = Join-Path $ProjectRoot '.runtime\app.pid'
if (Test-Path -LiteralPath $PidFile) {
    $ExistingPid = [int]([IO.File]::ReadAllText($PidFile).Trim())
    $Existing = Get-CimInstance Win32_Process -Filter "ProcessId=$ExistingPid" -ErrorAction SilentlyContinue
    if ($Existing -and $Existing.CommandLine -like '*campushub-1.0.0.jar*') { Write-Host 'CampusHub 已在运行：http://localhost:8080'; exit 0 }
}
if (!$ExternalDatabase) { & "$PSScriptRoot\database.ps1" }
if (!$NoBuild) { & "$PSScriptRoot\build.ps1" }
$JarPath = Join-Path $ProjectRoot 'backend\target\campushub-1.0.0.jar'
if (!(Test-Path -LiteralPath $JarPath)) { throw '尚未构建，请先运行 .\scripts\build.ps1' }
New-Item -ItemType Directory -Force .runtime | Out-Null
$Listener = Get-NetTCPConnection -State Listen -LocalPort 8080 -ErrorAction SilentlyContinue
if ($Listener) { throw '8080 端口已被占用，请先停止原服务再启动 CampusHub。' }
# Run a separate copy so Windows does not lock Maven's target JAR during later builds.
$RuntimeJar = Join-Path $ProjectRoot '.runtime\campushub-1.0.0.jar'
Copy-Item -LiteralPath $JarPath -Destination $RuntimeJar -Force
$JavaPath = (Get-Command java.exe).Source
$Process = Start-Process -FilePath $JavaPath -ArgumentList @('-Xms64m','-Xmx320m','-jar',('"'+$RuntimeJar+'"')) -WorkingDirectory $ProjectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $ProjectRoot '.runtime\app.log') -RedirectStandardError (Join-Path $ProjectRoot '.runtime\app-error.log')
[IO.File]::WriteAllText($PidFile,[string]$Process.Id)
for ($Attempt=0;$Attempt -lt 60;$Attempt++) {
    if ($Process.HasExited) { throw '启动失败，请查看 .runtime\app.log 和 app-error.log' }
    try { $Response = Invoke-RestMethod 'http://localhost:8080/api/health' -TimeoutSec 2; if ($Response.status -eq 'UP') { Write-Host 'CampusHub 已启动：http://localhost:8080'; Write-Host '学生：student / CampusHub@2026；管理员：admin / CampusHub@2026（仅演示模式）'; exit 0 } } catch {}
    Start-Sleep -Seconds 1
}
throw '等待启动超时，请查看 .runtime\app.log'
