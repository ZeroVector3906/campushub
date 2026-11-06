. "$PSScriptRoot\env.ps1"
Set-Location -LiteralPath $ProjectRoot
$ReleasePath = Join-Path $ProjectRoot 'release'
New-Item -ItemType Directory -Force $ReleasePath | Out-Null
$ZipPath = Join-Path $ReleasePath 'CampusHub-source.zip'
if (Test-Path -LiteralPath $ZipPath) { Remove-Item -LiteralPath $ZipPath -Force }
$Archive=[IO.Compression.ZipFile]::Open($ZipPath,[IO.Compression.ZipArchiveMode]::Create)
try {
    $Files = @()
    foreach ($Folder in @('backend\src','frontend\src','frontend\public','scripts','docs')) { $Files += Get-ChildItem -LiteralPath (Join-Path $ProjectRoot $Folder) -File -Recurse }
    foreach ($FileName in @('README.md','.gitignore','.dockerignore','.env.example','Dockerfile','compose.yml','render.yaml','backend\pom.xml','frontend\package.json','frontend\package-lock.json','frontend\tsconfig.json','frontend\vite.config.ts','frontend\index.html')) { $Files += Get-Item -LiteralPath (Join-Path $ProjectRoot $FileName) }
    foreach ($File in $Files) {
        $Relative=[IO.Path]::GetRelativePath($ProjectRoot,$File.FullName).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($Archive,$File.FullName,$Relative,[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $Archive.Dispose() }
Copy-Item -LiteralPath (Join-Path $ProjectRoot 'backend\target\campushub-1.0.0.jar') -Destination (Join-Path $ReleasePath 'CampusHub-1.0.0.jar') -Force
Get-ChildItem -LiteralPath $ReleasePath -File | Select-Object Name,Length
