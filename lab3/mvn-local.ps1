param([Parameter(ValueFromRemainingArguments=$true)][string[]]$MavenArgs)
$taskRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$toolsRoot = Join-Path (Split-Path -Parent (Split-Path -Parent $taskRoot)) '.tools'
$jdkPath = Get-ChildItem $toolsRoot -Directory -Filter 'jdk-*' | Select-Object -First 1
$mavenPath = Join-Path $toolsRoot 'apache-maven-3.9.11/bin/mvn.cmd'
if ($jdkPath -and (Test-Path $mavenPath)) {
    $env:JAVA_HOME = $jdkPath.FullName
    & $mavenPath "-Dmaven.repo.local=$toolsRoot/m2" @MavenArgs
    exit $LASTEXITCODE
}
& mvn @MavenArgs
exit $LASTEXITCODE
