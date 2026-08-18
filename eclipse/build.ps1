# build.ps1 - thin wrapper around the Maven Wrapper that ensures a valid JAVA_HOME.
# The system JAVA_HOME may be missing or point to a non-existent JRE; this resolves
# a real JDK (registry first, then common install dirs) before invoking mvnw.cmd.
# All arguments are forwarded to Maven, e.g.:  .\build.ps1 clean verify
$ErrorActionPreference = "Stop"

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\java.exe")) { return $env:JAVA_HOME.Trim() }
    $result = $null
    foreach ($key in @("HKLM:\SOFTWARE\JavaSoft\JDK", "HKLM:\SOFTWARE\JavaSoft\Java Development Kit")) {
        if ($result) { break }
        try {
            foreach ($k in (Get-ChildItem $key -ErrorAction Stop)) {
                $home = (Get-ItemProperty $k.PSPath -Name JavaHome -ErrorAction SilentlyContinue).JavaHome
                if ($home -and (Test-Path "$home\bin\java.exe")) { $result = $home; break }
            }
        } catch { }
    }
    if (-not $result) {
        foreach ($glob in @("C:\Program Files\Java\jdk-*", "C:\Program Files\Eclipse Adoptium\jdk-*", "C:\Program Files\Microsoft\jdk-*")) {
            if ($result) { break }
            foreach ($m in (Get-Item $glob -ErrorAction SilentlyContinue)) {
                if (Test-Path "$($m.FullName)\bin\java.exe") { $result = $m.FullName; break }
            }
        }
    }
    if ($result) { return $result.Trim() }
    return $null
}

$jdk = Resolve-JavaHome
if (-not $jdk) { throw "No JDK found. Set JAVA_HOME to a JDK (>=17) containing bin\java.exe." }
$env:JAVA_HOME = $jdk
Write-Host "[build] JAVA_HOME = $jdk" -ForegroundColor DarkGray

& (Join-Path $PSScriptRoot "mvnw.cmd") @args
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
