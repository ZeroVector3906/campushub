$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$TaskJdk = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot '.tools') -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
if ($TaskJdk) { $env:JAVA_HOME = $TaskJdk.FullName; $env:PATH = "$($TaskJdk.FullName)\bin;$env:PATH" }
if (Test-Path (Join-Path $ProjectRoot '.tools\apache-maven-3.9.16\bin')) { $env:PATH = "$(Join-Path $ProjectRoot '.tools\apache-maven-3.9.16\bin');$env:PATH" }
$env:DB_URL = if ($env:DB_URL) { $env:DB_URL } else { 'jdbc:postgresql://localhost:54329/campushub' }
$env:DB_USERNAME = if ($env:DB_USERNAME) { $env:DB_USERNAME } else { 'campushub' }
$env:DB_PASSWORD = if ($env:DB_PASSWORD) { $env:DB_PASSWORD } else { 'campushub_local_dev' }
$env:SEED_DEMO = if ($env:SEED_DEMO) { $env:SEED_DEMO } else { 'true' }
function Invoke-Checked { param([scriptblock]$Action) & $Action; if ($LASTEXITCODE -ne 0) { throw "命令执行失败，退出码：$LASTEXITCODE" } }
