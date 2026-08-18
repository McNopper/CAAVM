# debug-launch.ps1 - launches eclipse-cpp in the background with the OSGi console,
# queries the opencode bundles' state (ss/diag), captures the platform log, then
# shuts it down. Uses a throwaway workspace so the user's real workspace is untouched.
param(
    [int]$Port = 14923,
    [int]$UpSeconds = 35
)
$ErrorActionPreference = "Continue"
$eclipse = "C:\eclipse-cpp\eclipsec.exe"
$ocTemp  = Join-Path $env:TEMP "opencode"
$ws      = Join-Path $ocTemp "eclipse-ws"
$log     = Join-Path $ocTemp "eclipse-console.log"

if (-not (Test-Path $eclipse)) { throw "eclipsec.exe not found at $eclipse" }
New-Item -ItemType Directory -Path $ocTemp -Force | Out-Null
Remove-Item $log -ErrorAction SilentlyContinue
Remove-Item "$log.err" -ErrorAction SilentlyContinue
if (-not (Test-Path $ws)) { New-Item -ItemType Directory -Path $ws -Force | Out-Null }

# warn if an eclipse is already running (would lock the shared configuration)
$existing = Get-Process -Name eclipse,eclipsec -ErrorAction SilentlyContinue
if ($existing) {
    Write-Host "[debug] WARNING: eclipse already running (pid(s): $($existing.Id -join ',')). Close it for a clean check." -ForegroundColor Red
}

Write-Host "[debug] launching eclipse-cpp (console port $Port, workspace $ws)..." -ForegroundColor Cyan
$proc = Start-Process -FilePath $eclipse `
    -ArgumentList "-console","$Port","-consoleLog","-data",$ws `
    -WindowStyle Hidden -PassThru -RedirectStandardOutput $log -RedirectStandardError "$log.err"
Write-Host "[debug] eclipse pid=$($proc.Id)"

function Read-Stream($stream, [int]$seconds) {
    # line-based read (ASCII) so multi-line console output is captured reliably
    try { $stream.ReadTimeout = 800 } catch { }
    $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::ASCII)
    $sb = New-Object System.Text.StringBuilder
    $deadline = (Get-Date).AddSeconds($seconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $line = $reader.ReadLine()
            if ($null -ne $line) { [void]$sb.AppendLine($line) }
        } catch { Start-Sleep -Milliseconds 150 }
    }
    return $sb.ToString()
}

function Invoke-Osgi($client, $cmd, [int]$wait = 3) {
    $stream = $client.GetStream()
    $w = New-Object System.IO.StreamWriter($stream)
    $w.AutoFlush = $true
    $w.WriteLine($cmd)
    return Read-Stream $stream $wait
}

$connected = $false
$attempts = 0
for ($i = 0; $i -lt 150; $i++) {
    Start-Sleep -Seconds 1
    try {
        $test = New-Object System.Net.Sockets.TcpClient
        $test.Connect("127.0.0.1", $Port)
        $test.Close()
        $connected = $true; $attempts = $i; break
    } catch { }
}
Write-Host "[debug] console port up=$connected after ${attempts}s" -ForegroundColor Cyan

if ($connected) {
    # let the workbench finish starting (and the opencode bundles start) before querying
    Write-Host "[debug] waiting 35s for workbench to settle..." -ForegroundColor Cyan
    Start-Sleep -Seconds 35

    $client = New-Object System.Net.Sockets.TcpClient("127.0.0.1", $Port)
    [void](Read-Stream $client.GetStream() 3)  # drain banner/prompt

    Write-Host "`n========== control: ss org.eclipse.ui (proves console works) ==========" -ForegroundColor Yellow
    Write-Host (Invoke-Osgi $client "ss org.eclipse.ui" 6)

    Write-Host "========== ss opencode ==========" -ForegroundColor Yellow
    Write-Host (Invoke-Osgi $client "ss opencode" 6)

    Write-Host "========== diag com.opencode.ide.ui ==========" -ForegroundColor Yellow
    Write-Host (Invoke-Osgi $client "diag com.opencode.ide.ui" 6)
    $client.Close()
}

Write-Host "[debug] letting workbench run ${UpSeconds}s to capture startup logs..." -ForegroundColor Cyan
Start-Sleep -Seconds $UpSeconds

try { Stop-Process -Id $proc.Id -Force -ErrorAction Stop; Write-Host "[debug] stopped eclipse pid=$($proc.Id)" }
catch { Write-Host "[debug] stop failed: $_" }
try { $proc.Descendants() | ForEach-Object { Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue } } catch {}

Write-Host "`n========== platform log: opencode/error/perspective lines ==========" -ForegroundColor Yellow
if (Test-Path $log) {
    Get-Content $log -ErrorAction SilentlyContinue |
        Select-String -Pattern "opencode|ERROR|could not|Unresolved|missing requirement|requires '|perspective|org.eclipse.ui" -CaseSensitive:$false |
        Select-Object -First 80 | ForEach-Object { Write-Host $_.Line }
}
$errLog = "$log.err"
if ((Test-Path $errLog) -and (Get-Item $errLog).Length -gt 0) {
    Write-Host "`n========== stderr (.err) ==========" -ForegroundColor Yellow
    Get-Content $errLog | Select-Object -First 30 | ForEach-Object { Write-Host $_ }
}
