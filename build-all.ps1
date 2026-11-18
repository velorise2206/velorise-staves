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

function Test-SharedClassParity {
    # 1. Verify no duplicate class files between 1.21.1/common and 1.21.1/{neoforge,fabric}
    $commonJavaDir = Join-Path $Root "1.21.1\common\src\main\java"
    $nfJavaDir     = Join-Path $Root "1.21.1\neoforge\src\main\java"
    $fbJavaDir     = Join-Path $Root "1.21.1\fabric\src\main\java"

    if (Test-Path $commonJavaDir) {
        Get-ChildItem -Path $commonJavaDir -Filter "*.java" -Recurse | ForEach-Object {
            $rel = $_.FullName.Substring($commonJavaDir.Length).TrimStart('\', '/')
            if (Test-Path (Join-Path $nfJavaDir $rel)) {
                throw "Duplicate class in both 1.21.1/common and 1.21.1/neoforge: $rel"
            }
            if (Test-Path (Join-Path $fbJavaDir $rel)) {
                throw "Duplicate class in both 1.21.1/common and 1.21.1/fabric: $rel"
            }
        }
    }

    # 2. Verify cross-version pure Java parity between 1.21.1/common and 1.20.1/forge
    $crossVersionClasses = @(
        "audio\CharacterVoiceManager.java",
        "client\CharacterVoiceScreen.java",
        "client\VeloriseConfigScreen.java",
        "compat\jade\VeloriseStavesJadePlugin.java",
        "element\ElectrifiedWaterManager.java",
        "element\Element.java",
        "element\ElementalReaction.java",
        "element\FurinaManager.java",
        "element\GaugeState.java",
        "element\PlayerGenshinState.java",
        "manager\TemporaryWebManager.java",
        "mixin\GuiGraphicsMixin.java",
        "mixin\HumanoidModelMixin.java",
        "mixin\ItemInHandRendererMixin.java"
    )
    foreach ($rel in $crossVersionClasses) {
        $cmFile = Join-Path $Root "1.21.1\common\src\main\java\com\velorise\staves\$rel"
        $fgFile = Join-Path $Root "1.20.1\forge\src\main\java\com\velorise\staves\$rel"
        if ((Test-Path $cmFile) -and (Test-Path $fgFile)) {
            $h1 = (Get-FileHash $cmFile -Algorithm SHA256).Hash
            $h2 = (Get-FileHash $fgFile -Algorithm SHA256).Hash
            if ($h1 -ne $h2) {
                Write-Warning "Cross-version parity notice: $rel differs between 1.21.1/common and 1.20.1/forge"
            }
        }
    }
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
            cmd.exe /c "gradlew.bat clean build --no-daemon"
        } else {
            cmd.exe /c "gradlew.bat build --no-daemon"
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

        if (-not [string]::IsNullOrWhiteSpace($ArtifactPattern)) {
            Get-ChildItem -Path $OutputDirectory -Filter $ArtifactPattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
            Get-ChildItem -Path $ModsRoot -Filter $ArtifactPattern -File -ErrorAction SilentlyContinue | Remove-Item -Force
        }

        foreach ($jar in $producedJars) {
            $dest = Join-Path $OutputDirectory $jar.Name
            Copy-Item -Path $jar.FullName -Destination $dest -Force
            Write-Host "Copied: $($jar.Name) -> $OutputDirectory" -ForegroundColor Green
        }

        $tlauncherNeoForgeMods = if (-not [string]::IsNullOrWhiteSpace($env:VELORISE_COPY_TO_MODS)) {
            $env:VELORISE_COPY_TO_MODS
        } elseif (-not [string]::IsNullOrWhiteSpace($env:APPDATA)) {
            Join-Path $env:APPDATA ".minecraft\versions\neoforge 1.21.1\mods"
        } else {
            $null
        }
        if ($DisplayName -eq "NeoForge 1.21.1" -and $tlauncherNeoForgeMods -and (Test-Path $tlauncherNeoForgeMods)) {
            Get-ChildItem -Path $tlauncherNeoForgeMods -Filter "*velorise_staves*.jar" -File -ErrorAction SilentlyContinue | Remove-Item -Force
            foreach ($jar in $producedJars) {
                Copy-Item -Path $jar.FullName -Destination $tlauncherNeoForgeMods -Force
                Write-Host "Deployed to TLauncher NeoForge: $($jar.Name) -> $tlauncherNeoForgeMods" -ForegroundColor Green
            }
        }
    }
    finally {
        Pop-Location
    }
}

try {
    Test-SharedClassParity
    Invoke-Build -DisplayName "Forge 1.20.1" -Folder "1.20.1\forge" -JavaEnvVar "JAVA17_HOME" -ArtifactPattern "*forge*.jar"
    Invoke-Build -DisplayName "NeoForge 1.21.1" -Folder "1.21.1\neoforge" -JavaEnvVar "JAVA21_HOME" -ArtifactPattern "*neoforge*.jar"
    Invoke-Build -DisplayName "Fabric 1.21.1" -Folder "1.21.1\fabric" -JavaEnvVar "JAVA21_HOME" -ArtifactPattern "*fabric*.jar"

    Write-Host "`n==================================================" -ForegroundColor Green
    Write-Host " ALL BUILDS COMPLETED SUCCESSFULLY" -ForegroundColor Green
    Write-Host " Mod artifacts copied to: $OutputDirectory" -ForegroundColor Green
    Write-Host "==================================================" -ForegroundColor Green
}
finally {
    $env:JAVA_HOME = $OriginalJavaHome
    $env:Path = $OriginalPath
}
