param([switch]$Database)
. "$PSScriptRoot\env.ps1"
$PidFile = Join-Path $ProjectRoot '.runtime\app.pid'
if (Test-Path -LiteralPath $PidFile) {
    $AppPid = [int]([IO.File]::ReadAllText($PidFile).Trim())
    $AppProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$AppPid" -ErrorAction SilentlyContinue
    if ($AppProcess -and $AppProcess.CommandLine -like '*campushub-1.0.0.jar*') { Stop-Process -Id $AppPid; Write-Host 'CampusHub 已停止' }
    Remove-Item -LiteralPath $PidFile -Force
}
if ($Database) { & "$PSScriptRoot\database.ps1" -Stop }
