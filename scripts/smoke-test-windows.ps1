param(
    [string]$MinecraftVersion = "26.2",
    [int]$StartupTimeoutSeconds = 240
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$repositoryDirectory = Split-Path -Parent $PSScriptRoot
$serverDirectory = Join-Path $repositoryDirectory "build\windows-smoke-server"
$pluginDirectory = Join-Path $serverDirectory "plugins"
$paperJar = Join-Path $serverDirectory "paper.jar"
$serverLog = Join-Path $serverDirectory "logs\latest.log"
$consoleLog = Join-Path $serverDirectory "windows-smoke-console.log"
$database = Join-Path $pluginDirectory "AllAndOnlyChests\data\challenge.db"
$startScript = Join-Path $PSScriptRoot "start-test-server.bat"

function Stop-ServerProcess {
    param(
        [System.Diagnostics.Process]$Process
    )

    if ($null -eq $Process) {
        return
    }

    try {
        if ($Process.HasExited) {
            return
        }
    } catch [System.InvalidOperationException] {
        return
    }

    try {
        $Process.StandardInput.WriteLine("stop")
        $Process.StandardInput.Flush()
    } catch {
        Write-Warning "The stop command could not be sent: $_"
    }

    if (-not $Process.WaitForExit(60000)) {
        Write-Warning "Paper did not stop within 60 seconds; terminating the process tree."
        $Process.Kill($true)
        $Process.WaitForExit()
    }
}

if (Test-Path $serverDirectory) {
    Remove-Item $serverDirectory -Recurse -Force
}

New-Item $pluginDirectory -ItemType Directory -Force | Out-Null

$pluginJar = Get-ChildItem (Join-Path $repositoryDirectory "build\libs\AllAndOnlyChests-*.jar") |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

if ($null -eq $pluginJar) {
    throw "No built plugin JAR was found. Run gradlew.bat clean test build first."
}

$nativeEntries = & jar tf $pluginJar.FullName |
    Select-String -SimpleMatch "org/sqlite/native/Windows/x86_64/sqlitejdbc.dll"

if ($null -eq $nativeEntries) {
    throw "The plugin JAR does not contain the Windows x64 SQLite driver."
}

Copy-Item $pluginJar.FullName (Join-Path $pluginDirectory $pluginJar.Name)

$headers = @{
    "User-Agent" = "AllAndOnlyChests-CI/0.1 (https://github.com/kisimediaDE/mc-all-and-only-chest)"
}
$buildsUrl = "https://fill.papermc.io/v3/projects/paper/versions/$MinecraftVersion/builds"
$builds = Invoke-RestMethod -Uri $buildsUrl -Headers $headers
$stableBuild = $builds |
    Where-Object { $_.channel -eq "STABLE" } |
    Select-Object -First 1

if ($null -eq $stableBuild) {
    throw "No stable Paper build is available for Minecraft $MinecraftVersion."
}

$downloadUrl = $stableBuild.downloads.'server:default'.url
if ([string]::IsNullOrWhiteSpace($downloadUrl)) {
    throw "The Paper downloads service returned no server download URL."
}

Write-Host "Downloading Paper $MinecraftVersion build $($stableBuild.id)..."
Invoke-WebRequest -Uri $downloadUrl -Headers $headers -OutFile $paperJar

Set-Content (Join-Path $serverDirectory "eula.txt") "eula=true" -Encoding ascii
@(
    "online-mode=false"
    "spawn-protection=0"
    "view-distance=2"
    "simulation-distance=2"
) | Set-Content (Join-Path $serverDirectory "server.properties") -Encoding ascii

$processInfo = [System.Diagnostics.ProcessStartInfo]::new()
$processInfo.FileName = $env:ComSpec
$processInfo.Arguments = "/d /q /c `"`"$startScript`"`""
$processInfo.WorkingDirectory = $repositoryDirectory
$processInfo.UseShellExecute = $false
$processInfo.RedirectStandardInput = $true
$processInfo.RedirectStandardOutput = $true
$processInfo.RedirectStandardError = $true
$processInfo.CreateNoWindow = $true

$serverProcess = [System.Diagnostics.Process]::new()
$serverProcess.StartInfo = $processInfo
$standardOutput = $null
$standardError = $null
$previousTestServerDirectory = $env:AOC_TEST_SERVER_DIR

try {
    $env:AOC_TEST_SERVER_DIR = $serverDirectory
    Write-Host "Starting Paper through scripts\start-test-server.bat..."
    if (-not $serverProcess.Start()) {
        throw "The Windows start script could not be launched."
    }

    $standardOutput = $serverProcess.StandardOutput.ReadToEndAsync()
    $standardError = $serverProcess.StandardError.ReadToEndAsync()

    $deadline = [DateTime]::UtcNow.AddSeconds($StartupTimeoutSeconds)
    $paperReady = $false
    $pluginEnabled = $false

    while ([DateTime]::UtcNow -lt $deadline) {
        if (Test-Path $serverLog) {
            $logContent = Get-Content $serverLog -Raw
            $paperReady = $logContent -match 'Done \(.+\)! For help, type "help"'
            $pluginEnabled = $logContent -match "All and Only Chests enabled with"

            if ($logContent -match "Error occurred while enabling AllAndOnlyChests" -or
                $logContent -match "Could not load 'plugins\\AllAndOnlyChests") {
                throw "Paper reported an error while loading AllAndOnlyChests."
            }

            if ($paperReady -and $pluginEnabled) {
                break
            }
        }

        if ($serverProcess.HasExited) {
            throw "Paper exited before the smoke test completed (exit code $($serverProcess.ExitCode))."
        }

        Start-Sleep -Seconds 1
    }

    if (-not $paperReady) {
        throw "Paper did not reach the ready state within $StartupTimeoutSeconds seconds."
    }
    if (-not $pluginEnabled) {
        throw "The AllAndOnlyChests activation message was not found."
    }
    if (-not (Test-Path $database)) {
        throw "The SQLite database was not created at $database."
    }
    if ((Get-Item $database).Length -eq 0) {
        throw "The created SQLite database is empty."
    }

    Write-Host "Windows smoke test passed: Paper is ready, the plugin is enabled, and SQLite is initialized."
} finally {
    Stop-ServerProcess -Process $serverProcess

    if ($null -ne $standardOutput -and $null -ne $standardError) {
        $consoleContent = @(
            $standardOutput.GetAwaiter().GetResult()
            $standardError.GetAwaiter().GetResult()
        ) -join [Environment]::NewLine
        Set-Content $consoleLog $consoleContent -Encoding utf8
        Write-Host $consoleContent
    }

    $serverProcess.Dispose()
    $env:AOC_TEST_SERVER_DIR = $previousTestServerDirectory
}
