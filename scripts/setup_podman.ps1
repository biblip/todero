param (
    [string]$InstallDir,
    [string]$ContainerType = "linux"
)

if (-not $InstallDir) {
    Write-Host "Usage: .\setup_podman.ps1 -InstallDir <directory> [-ContainerType linux|windows]"
    exit 1
}

New-Item -ItemType Directory -Path $InstallDir -Force | Out-Null

$PodmanURL = "https://github.com/containers/podman/releases/latest/download/podman-remote-release-windows_amd64.zip"
$ZipFile = "$InstallDir\podman.zip"

function Download-WithReadableProgress($url, $outputPath) {
    $request = [System.Net.WebRequest]::Create($url)
    $response = $request.GetResponse()
    $totalSize = $response.ContentLength
    $responseStream = $response.GetResponseStream()

    $fileStream = [System.IO.File]::OpenWrite($outputPath)

    $bufferSize = 1MB
    $buffer = New-Object byte[] $bufferSize
    $downloadedBytes = 0
    $prevPercent = -1
    $totalMB = [math]::Round($totalSize / 1MB, 2)

    Write-Host "Downloading Podman... ($totalMB MB total)"

    while (($read = $responseStream.Read($buffer, 0, $bufferSize)) -gt 0) {
        $fileStream.Write($buffer, 0, $read)
        $downloadedBytes += $read
        $currentMB = [math]::Round($downloadedBytes / 1MB, 2)
        $percentComplete = [math]::Floor(($downloadedBytes / $totalSize) * 100)

        if ($percentComplete -ne $prevPercent) {
            Write-Progress -Activity "Downloading Podman..." `
                           -Status "$percentComplete% Complete ($currentMB MB of $totalMB MB)" `
                           -PercentComplete $percentComplete
            $prevPercent = $percentComplete
        }
    }

    $fileStream.Close()
    $responseStream.Close()
    $response.Close()

    Write-Progress -Activity "Downloading Podman..." -Completed
    Write-Host "Download complete! ($totalMB MB downloaded)"
}

Download-WithReadableProgress -url $PodmanURL -outputPath $ZipFile

# Extract downloaded files
Expand-Archive -Path $ZipFile -DestinationPath $InstallDir -Force

# Dynamically find extracted Podman folder
$PodmanFolder = Get-ChildItem -Path $InstallDir -Directory |
                Where-Object { $_.Name -match "^PODMAN-\d+\.\d+\.\d+$" } |
                Select-Object -First 1 -ExpandProperty FullName

if (-not $PodmanFolder) {
    Write-Host "Error: Podman folder not found."
    exit 1
}

# Locate podman.exe
$PodmanBinary = Get-ChildItem -Path "$PodmanFolder\usr\bin\podman.exe" -File |
                Select-Object -First 1 -ExpandProperty FullName

if (-not $PodmanBinary) {
    Write-Host "Error: podman.exe not found."
    exit 1
}

# Move files to stable directory
$StableBinDir = "$InstallDir\bin"
New-Item -ItemType Directory -Path $StableBinDir -Force | Out-Null
Move-Item -Path "$PodmanFolder\usr\bin\*" -Destination $StableBinDir -Force

# Cleanup
Remove-Item -Path $PodmanFolder -Recurse -Force

Write-Host "Podman installed to $StableBinDir"

# Configure Podman for container type
if ($ContainerType -ne "linux" -and $ContainerType -ne "windows") {
    $ContainerType = Read-Host "Choose container type (linux/windows)"
}

if ($ContainerType -eq "linux") {
    Write-Host "Configuring Podman for Linux containers..."
    Start-Process -FilePath "$StableBinDir\podman.exe" -ArgumentList "machine init" -NoNewWindow -Wait
    Start-Process -FilePath "$StableBinDir\podman.exe" -ArgumentList "machine start" -NoNewWindow
}
else {
    Write-Host "Configuring Podman for Windows containers..."
    Write-Host "Ensure your system supports Windows containers."
}

Write-Host "Podman setup complete! Test it:"
Write-Host "`"$StableBinDir\podman.exe run --rm alpine echo 'Hello from Podman'`""

#exit 0  # Ensures the script exits cleanly
