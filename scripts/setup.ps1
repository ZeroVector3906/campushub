. "$PSScriptRoot\env.ps1"
Set-Location -LiteralPath $ProjectRoot
New-Item -ItemType Directory -Force .tools,.runtime | Out-Null
if (!$TaskJdk) {
    Invoke-Checked { curl.exe -fL --retry 3 -o .tools\jdk21.zip 'https://api.adoptium.net/v3/binary/latest/21/ga/windows/x64/jdk/hotspot/normal/eclipse' }
    Invoke-Checked { tar.exe -xf .tools\jdk21.zip -C .tools }
}
if (!(Test-Path -LiteralPath '.tools\pgsql\bin\pg_ctl.exe')) {
    Invoke-Checked { curl.exe -fL --retry 3 -o .tools\postgresql.zip 'https://get.enterprisedb.com/postgresql/postgresql-17.6-1-windows-x64-binaries.zip' }
    Invoke-Checked { tar.exe -xf .tools\postgresql.zip -C .tools }
}
if (!(Get-Command mvn -ErrorAction SilentlyContinue) -and !(Test-Path '.tools\apache-maven-3.9.16\bin\mvn.cmd')) {
    Invoke-Checked { curl.exe -fL --retry 3 -o .tools\maven.zip 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip' }
    Expand-Archive -LiteralPath .tools\maven.zip -DestinationPath .tools -Force
}
. "$PSScriptRoot\env.ps1"
if (!(Get-Command node -ErrorAction SilentlyContinue)) { throw '请先安装 Node.js 22.12+ 或 24 LTS：https://nodejs.org/' }
& "$PSScriptRoot\database.ps1"
Push-Location frontend
try { Invoke-Checked { npm ci --no-audit --no-fund } } finally { Pop-Location }
Write-Host '环境准备完成。运行 .\scripts\start.ps1 启动完整网站。'
