# build-product.ps1
param(
    [string]$Platform = "windows"
)

# Locate the repository root
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
if ([string]::IsNullOrEmpty($ScriptDir)) {
    $ScriptDir = Get-Location
}
Set-Location $ScriptDir

Write-Host "============================================================"
Write-Host "Starting Headless RCP Build of EVO via PowerShell..."
Write-Host "============================================================"
Write-Host "Target Platform: $Platform"

# Locate Maven Wrapper
$Mvnw = Join-Path $ScriptDir "mvnw"
if ($IsWindows -or $env:OS -like "*Windows*") {
    $Mvnw = Join-Path $ScriptDir "mvnw.cmd"
}

if (-not (Test-Path $Mvnw)) {
    Write-Error "ERROR: Maven Wrapper not found at $Mvnw"
    exit 1
}

# Determine profile
$Profile = "-Pwindows"
if ($Platform -eq "linux") {
    $Profile = "-Plinux"
} elseif ($Platform -eq "all") {
    $Profile = "-Pall-platforms"
}

Write-Host "Executing build via Maven Wrapper with profile: $Profile..."
if ($IsWindows -or $env:OS -like "*Windows*") {
    & $Mvnw clean verify -DskipTests $Profile
} else {
    & sh $Mvnw clean verify -DskipTests $Profile
}

$BuildExitCode = $LASTEXITCODE
if ($BuildExitCode -ne 0) {
    Write-Error "ERROR: Headless RCP Build FAILED with exit code $BuildExitCode"
    exit $BuildExitCode
}

# Create release folder
$ReleaseDir = Join-Path $ScriptDir "release"
if (-not (Test-Path $ReleaseDir)) {
    New-Item -ItemType Directory -Path $ReleaseDir | Out-Null
}

$ProdFound = $false

# Locate generated products
$WinZip = Join-Path $ScriptDir "eu.kalafatic.evolution.repository/target/products/evolution-win32.win32.x86_64.zip"
$LinuxTar = Join-Path $ScriptDir "eu.kalafatic.evolution.repository/target/products/evolution-linux.gtk.x86_64.tar.gz"

if (Test-Path $WinZip) {
    Copy-Item $WinZip (Join-Path $ReleaseDir "EVO-win-x64.zip") -Force
    Write-Host "SUCCESS: Published Windows x64 package: release/EVO-win-x64.zip"
    $ProdFound = $true
}

if (Test-Path $LinuxTar) {
    Copy-Item $LinuxTar (Join-Path $ReleaseDir "EVO-linux-x64.tar.gz") -Force
    Write-Host "SUCCESS: Published Linux x64 package: release/EVO-linux-x64.tar.gz"
    $ProdFound = $true
}

if (-not $ProdFound) {
    # Check for any zip or tar.gz in targets (wildcard search)
    $ZipFiles = Get-ChildItem -Path (Join-Path $ScriptDir "eu.kalafatic.evolution.repository/target/products") -Filter "*.zip" -Recurse -ErrorAction SilentlyContinue
    $TarFiles = Get-ChildItem -Path (Join-Path $ScriptDir "eu.kalafatic.evolution.repository/target/products") -Filter "*.tar.gz" -Recurse -ErrorAction SilentlyContinue

    foreach ($file in $ZipFiles) {
        Copy-Item $file.FullName (Join-Path $ReleaseDir "EVO-win-x64.zip") -Force
        Write-Host "SUCCESS: Published Windows x64 package: release/EVO-win-x64.zip"
        $ProdFound = $true
    }
    foreach ($file in $TarFiles) {
        Copy-Item $file.FullName (Join-Path $ReleaseDir "EVO-linux-x64.tar.gz") -Force
        Write-Host "SUCCESS: Published Linux x64 package: release/EVO-linux-x64.tar.gz"
        $ProdFound = $true
    }
}

if (-not $ProdFound) {
    Write-Error "ERROR: Product was built but final packaged archive files could not be located."
    exit 1
}

# Generate checksums.txt
Write-Host "Generating checksums.txt..."
$ChecksumsFile = Join-Path $ReleaseDir "checksums.txt"
if (Test-Path $ChecksumsFile) {
    Remove-Item $ChecksumsFile -Force
}

if (Test-Path (Join-Path $ReleaseDir "EVO-win-x64.zip")) {
    $hash = Get-FileHash -Path (Join-Path $ReleaseDir "EVO-win-x64.zip") -Algorithm SHA256
    "$($hash.Hash.ToLower())  EVO-win-x64.zip" | Out-File -FilePath $ChecksumsFile -Append -Encoding ascii
}

if (Test-Path (Join-Path $ReleaseDir "EVO-linux-x64.tar.gz")) {
    $hash = Get-FileHash -Path (Join-Path $ReleaseDir "EVO-linux-x64.tar.gz") -Algorithm SHA256
    "$($hash.Hash.ToLower())  EVO-linux-x64.tar.gz" | Out-File -FilePath $ChecksumsFile -Append -Encoding ascii
}

Write-Host "============================================================"
Write-Host "Headless RCP Build and Packaging COMPLETED successfully."
Write-Host "============================================================"
exit 0
