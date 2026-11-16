# 启动 Vite 开发服务器（需已安装 Node.js，且 PATH 中有 node/npm）
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location (Join-Path $root "frontend")
if (-not (Get-Command node -ErrorAction SilentlyContinue)) {
  Write-Host "未找到 node。请安装 Node.js LTS 后重新打开终端，或检查 PATH。" -ForegroundColor Red
  Write-Host "推荐：winget install OpenJS.NodeJS.LTS"
  exit 1
}
if (-not (Test-Path "node_modules")) {
  npm install
}
npm run dev
