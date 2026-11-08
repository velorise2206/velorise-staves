[CmdletBinding()]
param(
    [switch]$Clean
)

$ErrorActionPreference = 'Stop'
$Root = $PSScriptRoot
$WorkspaceParent = Split-Path -Parent $Root.TrimEnd('\', '/')
$ModsRoot = Join-Path $WorkspaceParent 'mods'
$OutputDirectory = Join-Path $ModsRoot 'velorise_staves'

$OriginalJavaHome = $env:JAVA_HOME
$OriginalPath = $env:Path

function Use-JavaHome([string]$VariableName, [string]$Label) {
    $candidate = [Environment]::GetEnvironmentVariable($VariableName)

    if ([string]::IsNullOrWhiteSpace($candidate)) {
        if ($VariableName -eq 'JAVA21_HOME' -and (Test-Path 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot\bin\java.exe')) {
            $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot'
        } elseif ($VariableName -eq 'JAVA17_HOME' -and (Test-Path 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot\bin\java.exe')) {
            $candidate = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
        }
    }

    if (-not [string]::IsNullOrWhiteSpace($candidate)) {
        if (-not (Test-Path (Join-Path $candidate 'bin\java.exe'))) {
            throw "$VariableName points to an invalid JDK directory: $candidate"
        }

        $env:JAVA_HOME = $candidate
        $env:Path = "$(Join-Path $candidate 'bin');$OriginalPath"
        Write-Host "Using $Label from ${VariableName}: $candidate"
        return
    }

    if (-not [string]::IsNullOrWhiteSpace($OriginalJavaHome)) {
        $env:JAVA_HOME = $OriginalJavaHome
        $env:Path = "$(Join-Path $OriginalJavaHome 'bin');$OriginalPath"
        Write-Host "${VariableName} is not set; using current JAVA_HOME for ${Label}: $OriginalJavaHome" -ForegroundColor Yellow
        return
    }

    $env:JAVA_HOME = $null
    $env:Path = $OriginalPath
    Write-Host "${VariableName} and JAVA_HOME are not set; Gradle will use java.exe from PATH for ${Label}." -ForegroundColor Yellow
}

function Invoke-Build(
    [string]$DisplayName,
    [string]$Folder,
    [string]$JavaEnvVar,
    [string]$ArtifactPattern
) {
    Write-Host "`n==================================================" -ForegroundColor Cyan
    Write-Host " Building $DisplayName" -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Cyan

    $projectDir = Join-Path $Root $Folder
    if (-not (Test-Path $projectDir)) {
        Write-Host "Skipping ${DisplayName}: directory '$Folder' does not exist yet." -ForegroundColor DarkGray
        return
    }

    Use-JavaHome -VariableName $JavaEnvVar -Label $DisplayName

    Push-Location $projectDir
    try {
        if ($Clean) {
            cmd.exe /c "gradlew.bat clean build"
        } else {
            cmd.exe /c "gradlew.bat build"
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Build failed for $DisplayName with exit code $LASTEXITCODE"
        }

        $libsDir = Join-Path $projectDir 'build\libs'
        if (-not (Test-Path $libsDir)) {
            Write-Warning "No build/libs directory found for $DisplayName"
            return
        }

        $producedJars = Get-ChildItem -Path $libsDir -Filter '*.jar' -File |
            Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*-javadoc.jar' -and $_.Name -notlike '*-dev.jar' }

        if (-not (Test-Path $OutputDirectory)) {
            New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
        }

        # Clean up old versions matching pattern in destination and root mods folder
        if (-not [string]::IsNullOrWhiteSpace($ArtifactPattern)) {
            Get-ChildItem -Path $OutputDirectory -Filter $ArtifactPattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
            Get-ChildItem -Path $ModsRoot -Filter $ArtifactPattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
        }

        foreach ($jar in $producedJars) {
            $dest = Join-Path $OutputDirectory $jar.Name
            Copy-Item -Path $jar.FullName -Destination $dest -Force
            Write-Host "Copied: $($jar.Name) -> $OutputDirectory" -ForegroundColor Green
        }
    }
    finally {
        Pop-Location
    }
}

try {
    Invoke-Build -DisplayName "Forge 1.20.1" -Folder "forge-1.20.1" -JavaEnvVar "JAVA17_HOME" -ArtifactPattern "*forge*.jar"
    Invoke-Build -DisplayName "NeoForge 1.21.1" -Folder "neoforge-1.21.1" -JavaEnvVar "JAVA21_HOME" -ArtifactPattern "*neoforge*.jar"
    Invoke-Build -DisplayName "Fabric 1.21.1" -Folder "fabric-1.21.1" -JavaEnvVar "JAVA21_HOME" -ArtifactPattern "*fabric*.jar"

    Write-Host "`n==================================================" -ForegroundColor Green
    Write-Host " ALL BUILDS COMPLETED SUCCESSFULLY" -ForegroundColor Green
    Write-Host " Mod artifacts copied to: $OutputDirectory" -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Green
}
finally {
    $env:JAVA_HOME = $OriginalJavaHome
    $env:Path = $OriginalPath
}
