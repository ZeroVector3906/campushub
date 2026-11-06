param([switch]$SkipTests)
. "$PSScriptRoot\env.ps1"
Set-Location -LiteralPath $ProjectRoot
Push-Location frontend
try {
    if (!(Test-Path -LiteralPath node_modules)) { Invoke-Checked { npm ci --no-audit --no-fund } }
    if (!$SkipTests) { Invoke-Checked { npm test } }
    Invoke-Checked { npm run build }
} finally { Pop-Location }
if ($SkipTests) { Invoke-Checked { mvn -B -f backend\pom.xml -DskipTests clean package } }
else { Invoke-Checked { mvn -B -f backend\pom.xml clean verify } }
Write-Host '构建完成：backend\target\campushub-1.0.0.jar（包含前端）'
