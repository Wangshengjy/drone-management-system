param(
  [int]$Port = 8080
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$backendDir = Join-Path $repoRoot 'backend'
$java = 'C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot\bin\java.exe'
$classPath = @(
  (Join-Path $backendDir 'target\verify-classes')
  (Join-Path $backendDir 'src\main\resources')
  (Join-Path $backendDir 'target\jar-extract\BOOT-INF\lib\*')
) -join ';'

Set-Location $backendDir
& $java '-cp' $classPath 'com.drone.collab.DroneCollabApplication' "--server.port=$Port"
