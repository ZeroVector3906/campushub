param([switch]$Stop)
. "$PSScriptRoot\env.ps1"
$PgBin = Join-Path $ProjectRoot '.tools\pgsql\bin'
$PgData = Join-Path $ProjectRoot '.runtime\pgdata'
if (!(Test-Path -LiteralPath "$PgBin\pg_ctl.exe")) { throw '请先运行 .\scripts\setup.ps1 下载本地 PostgreSQL，或自行配置 DB_URL。' }
if ($Stop) { & "$PgBin\pg_ctl.exe" -D $PgData stop -m fast; exit $LASTEXITCODE }
New-Item -ItemType Directory -Force (Join-Path $ProjectRoot '.runtime') | Out-Null
if (!(Test-Path -LiteralPath "$PgData\PG_VERSION")) {
    $PassFile = Join-Path $ProjectRoot '.runtime\initdb-password'
    [IO.File]::WriteAllText($PassFile, $env:DB_PASSWORD, [Text.UTF8Encoding]::new($false))
    try { Invoke-Checked { & "$PgBin\initdb.exe" -D $PgData -U campushub -A scram-sha-256 "--pwfile=$PassFile" --encoding=UTF8 --locale=C } }
    finally { Remove-Item -LiteralPath $PassFile -Force -ErrorAction SilentlyContinue }
}
& "$PgBin\pg_ctl.exe" -D $PgData status *> $null
if ($LASTEXITCODE -ne 0) { Invoke-Checked { & "$PgBin\pg_ctl.exe" -D $PgData -l (Join-Path $ProjectRoot '.runtime\postgres.log') -o '-p 54329 -h 127.0.0.1' -w start } }
$env:PGPASSWORD = $env:DB_PASSWORD
try {
    $Exists = & "$PgBin\psql.exe" -h 127.0.0.1 -p 54329 -U campushub -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='campushub'"
    if ($Exists -ne '1') { Invoke-Checked { & "$PgBin\createdb.exe" -h 127.0.0.1 -p 54329 -U campushub campushub } }
    $TestExists = & "$PgBin\psql.exe" -h 127.0.0.1 -p 54329 -U campushub -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='campushub_test'"
    if ($TestExists -ne '1') { Invoke-Checked { & "$PgBin\createdb.exe" -h 127.0.0.1 -p 54329 -U campushub campushub_test } }
} finally { Remove-Item Env:PGPASSWORD -ErrorAction SilentlyContinue }
Write-Host 'PostgreSQL 已就绪：127.0.0.1:54329（campushub / campushub_test）'
