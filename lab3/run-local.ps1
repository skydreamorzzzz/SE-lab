param(
    [switch]$SetupSuperAdmin,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$AppArgs
)
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$taskTools = Join-Path (Split-Path -Parent (Split-Path -Parent $projectRoot)) '.tools'
$taskJdk = Get-ChildItem $taskTools -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
$taskJava = if ($taskJdk) { Join-Path $taskJdk.FullName 'bin/java.exe' } else { 'java' }
if (!(Test-Path (Join-Path $projectRoot 'target/marketplace-1.0.0.jar'))) {
    throw '请先执行 mvn package 或 .\mvn-local.ps1 package'
}
if ($SetupSuperAdmin) {
    if (!$env:MARKET_SUPER_ADMIN_USER) { $env:MARKET_SUPER_ADMIN_USER = Read-Host '首次超级管理员用户名' }
    if ($env:MARKET_SUPER_ADMIN_USER -and !$env:MARKET_SUPER_ADMIN_PASSWORD) {
        $env:MARKET_SUPER_ADMIN_PASSWORD = Read-Host '超级管理员密码（含大小写、数字、特殊字符）' -MaskInput
    }
} elseif (!$env:MARKET_ADMIN_USER) {
    $env:MARKET_ADMIN_USER = Read-Host '首次管理员用户名（已有账户可回车跳过）'
}
if (!$SetupSuperAdmin -and $env:MARKET_ADMIN_USER -and !$env:MARKET_ADMIN_PASSWORD) {
    $env:MARKET_ADMIN_PASSWORD = Read-Host '管理员密码（已存在账户不会改密）' -MaskInput
}
Push-Location $projectRoot
try {
    & $taskJava -jar target/marketplace-1.0.0.jar @AppArgs
} finally {
    Pop-Location
    Remove-Item Env:MARKET_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:MARKET_SUPER_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
