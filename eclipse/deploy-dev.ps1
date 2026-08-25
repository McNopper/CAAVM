# deploy-dev.ps1 - fast development loop: copies the freshly built plugin JARs
# into the Eclipse CDT dropins folder, so you can test changes without building
# a p2 repo and without PDE installed.
#
# Usage:
#   .\build.ps1 clean verify     # build first
#   .\deploy-dev.ps1             # copy JARs to dropins
#   # then restart the Eclipse install (default C:\eclipse-cpp; -EclipseRoot / ECLIPSE_HOME to override)
param([string]$EclipseRoot = $(if ($env:ECLIPSE_HOME) { $env:ECLIPSE_HOME } else { "C:\eclipse-cpp" }))
$ErrorActionPreference = "Stop"

$root      = $PSScriptRoot
$bundleDir = Join-Path $root "bundles"
$dropins   = Join-Path $EclipseRoot "dropins\opencode-ide"
$plugins   = Join-Path $dropins "plugins"

if (-not (Test-Path -LiteralPath (Join-Path $EclipseRoot "dropins"))) {
    throw "$EclipseRoot\dropins not found. Is Eclipse CDT installed there? (default C:\eclipse-cpp; pass -EclipseRoot or set ECLIPSE_HOME to override)"
}

# wipe the whole opencode-ide dropin so p2 never sees stale versions/layouts
if (Test-Path -LiteralPath $dropins) {
    Remove-Item -LiteralPath $dropins -Recurse -Force
}
New-Item -ItemType Directory -Path $plugins -Force | Out-Null

# p2 recognizes a dropin subfolder that has a plugins/ (and optional features/) layout.
# Derived from the source tree rather than hardcoded, so a newly added bundle is
# never silently left undeployed. Test fragments are not runtime plugins.
$bundles = Get-ChildItem -LiteralPath $bundleDir -Directory |
    Where-Object { $_.Name -notlike "*.tests" } |
    Select-Object -ExpandProperty Name |
    Sort-Object
if (-not $bundles) {
    throw "No bundles found under $bundleDir."
}
foreach ($b in $bundles) {
    $jar = Get-ChildItem (Join-Path $bundleDir "$b\target\$b-*.jar") -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'sources' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) {
        throw "No built JAR found for $b. Run .\build.ps1 clean verify first."
    }
    Copy-Item -LiteralPath $jar.FullName -Destination $plugins -Force
    Write-Host "[deploy-dev] $($jar.Name) -> $plugins" -ForegroundColor Green
}

Write-Host ""
Write-Host "Done. (Re)start $EclipseRoot to load the plugins." -ForegroundColor Cyan
Write-Host "If views/perspective don't update, run eclipse once with -clean" -ForegroundColor DarkGray
Write-Host "(add '-clean' on its own line near the top of $EclipseRoot\eclipse.ini, then remove it after one launch)." -ForegroundColor DarkGray
