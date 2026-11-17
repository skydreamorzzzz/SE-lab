param(
    [switch]$SetupSuperAdmin,
    [Parameter(ValueFromRemainingArguments=$true)][string[]]$AppArgs
)
# Windows PowerShell 5.1 requires a UTF-8 BOM for the Chinese prompts below.
# AsSecureString is available in both Windows PowerShell 5.1 and PowerShell 7.
function Read-MarketPassword {
    param([string]$Prompt)
    $passwordValue = Read-Host -Prompt $Prompt -AsSecureString
    $passwordPointer = [IntPtr]::Zero
    try {
        $passwordPointer = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($passwordValue)
        return [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    } finally {
        if ($passwordPointer -ne [IntPtr]::Zero) {
            [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
        }
        $passwordValue.Dispose()
    }
}
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
        $env:MARKET_SUPER_ADMIN_PASSWORD = Read-MarketPassword '超级管理员密码（含大小写、数字、特殊字符）'
    }
} elseif (!$env:MARKET_ADMIN_USER) {
    $env:MARKET_ADMIN_USER = Read-Host '首次管理员用户名（已有账户可回车跳过）'
}
if (!$SetupSuperAdmin -and $env:MARKET_ADMIN_USER -and !$env:MARKET_ADMIN_PASSWORD) {
    $env:MARKET_ADMIN_PASSWORD = Read-MarketPassword '管理员密码（已存在账户不会改密）'
}
Push-Location $projectRoot
try {
    & $taskJava -jar target/marketplace-1.0.0.jar @AppArgs
} finally {
    Pop-Location
    Remove-Item Env:MARKET_ADMIN_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:MARKET_SUPER_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
