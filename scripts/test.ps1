. "$PSScriptRoot\env.ps1"
Set-Location -LiteralPath $ProjectRoot
& "$PSScriptRoot\database.ps1"
Push-Location frontend
try { Invoke-Checked { npm test }; Invoke-Checked { npm run build } } finally { Pop-Location }
Invoke-Checked { mvn -B -f backend\pom.xml clean verify }
