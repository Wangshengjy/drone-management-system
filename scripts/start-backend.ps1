# 启动 Spring Boot（需 JDK 17 + Maven）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$javaHome = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
$mvn = Join-Path $root "tools\apache-maven-3.9.9\bin\mvn.cmd"

if (-not (Test-Path $javaHome)) {
  Write-Host "未找到 JDK 17: $javaHome" -ForegroundColor Red
  Write-Host "请安装 Microsoft OpenJDK 17，或修改本脚本中的 javaHome。"
  exit 1
}
if (-not (Test-Path $mvn)) {
  Write-Host "未找到 Maven: $mvn" -ForegroundColor Yellow
  Write-Host "请下载 apache-maven-3.9.9-bin.zip 解压到 tools\apache-maven-3.9.9，或安装 Maven 并加入 PATH，然后执行: mvn -f backend/pom.xml spring-boot:run"
  exit 1
}

$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;" + $env:Path
Set-Location (Join-Path $root "backend")
& $mvn -q spring-boot:run
