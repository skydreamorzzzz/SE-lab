param([Parameter(ValueFromRemainingArguments=$true)][string[]]$AppArgs)
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$taskTools = Join-Path (Split-Path -Parent (Split-Path -Parent $projectRoot)) '.tools'
$taskJdk = Get-ChildItem $taskTools -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
$taskJava = if ($taskJdk) { Join-Path $taskJdk.FullName 'bin/java.exe' } else { 'java' }
if (!(Test-Path (Join-Path $projectRoot 'target/marketplace-1.0.0.jar'))) {
    throw '请先执行 mvn package 或 .\mvn-local.ps1 package'
}
if (!$env:MARKET_ADMIN_USER) { $env:MARKET_ADMIN_USER = Read-Host '首次建立管理员时填写用户名（已有管理员可直接回车）' }
if ($env:MARKET_ADMIN_USER -and !$env:MARKET_ADMIN_PASSWORD) {
    $env:MARKET_ADMIN_PASSWORD = Read-Host '管理员密码（至少8位，含大小写、数字、特殊字符；已存在账户不会改密）' -MaskInput
}
Push-Location $projectRoot
try {
    & $taskJava -jar target/marketplace-1.0.0.jar @AppArgs
} finally {
    Pop-Location
    Remove-Item Env:MARKET_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
