# Local one-command start: Student Dashboard (RuoYi-Vue backend + Vite frontend)
# Usage:  powershell -ExecutionPolicy Bypass -File scripts\local-start.ps1
# ASCII only on purpose: PowerShell 5.1 reads non-BOM .ps1 as ANSI and chokes on Chinese.
$ErrorActionPreference = 'Continue'
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
Set-Location $root

# ---- config (password lives in .local/local.env, which is gitignored) ----
$cfg = @{}
$envFile = Join-Path $root '.local\local.env'
if (Test-Path $envFile) {
  Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') { $cfg[$matches[1]] = $matches[2].Trim() }
  }
}
if (-not $cfg['DB_PASSWORD']) {
  $sec = Read-Host 'MySQL root password'
  $cfg['DB_PASSWORD'] = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($sec))
}

function Test-Port([int]$p) {
  try {
    $c = New-Object Net.Sockets.TcpClient
    $c.Connect('127.0.0.1', $p)
    $c.Close()
    return $true
  } catch { return $false }
}

# ---- 1) Redis ----
if (Test-Port 6379) {
  Write-Host '[1/3] Redis      : already running (6379)'
} else {
  $redis = Join-Path $root '.tools\redis\redis-server.exe'
  if (-not (Test-Path $redis)) { Write-Host '[1/3] Redis binary missing: .tools\redis\redis-server.exe'; exit 1 }
  Start-Process $redis -ArgumentList '--port', '6379' -WorkingDirectory (Split-Path $redis -Parent) -WindowStyle Minimized
  Start-Sleep -Seconds 2
  Write-Host '[1/3] Redis      : started (6379)'
}

# ---- 2) Backend ----
if (Test-Port 8080) {
  Write-Host '[2/3] Backend    : already running (8080)'
} else {
  $jar = Join-Path $root 'backend\ruoyi-admin\target\ruoyi-admin.jar'
  if (-not (Test-Path $jar)) {
    Write-Host '[2/3] Jar missing. Build it first:'
    Write-Host '      .tools\apache-maven-3.9.9\bin\mvn.cmd -f backend\pom.xml -DskipTests package'
    exit 1
  }
  New-Item -ItemType Directory -Force (Join-Path $root '.local\logs'), (Join-Path $root '.local\upload') | Out-Null
  $env:SPRING_PROFILES_ACTIVE = 'druid,docker'
  $env:DB_HOST = '127.0.0.1'
  $env:DB_PORT = '3306'
  $env:DB_NAME = if ($cfg['DB_NAME']) { $cfg['DB_NAME'] } else { 'education_db' }
  $env:DB_USER = if ($cfg['DB_USER']) { $cfg['DB_USER'] } else { 'root' }
  $env:DB_PASSWORD = $cfg['DB_PASSWORD']
  $env:DB_USE_SSL = 'false'
  $env:REDIS_HOST = '127.0.0.1'
  $env:REDIS_PORT = '6379'
  $env:REDIS_PASSWORD = ''
  $env:TOKEN_SECRET = 'local-dev-only-please-change-0123456789'
  $env:LOG_PATH = Join-Path $root '.local\logs'
  $env:RUOYI_PROFILE = Join-Path $root '.local\upload'
  # child process inherits the env vars above
  Start-Process powershell -ArgumentList '-NoExit', '-Command', ('java -jar "{0}"' -f $jar) -WorkingDirectory $root
  Write-Host '[2/3] Backend    : starting in a new window...'
}

# ---- 3) Frontend ----
if (Test-Port 80) {
  Write-Host '[3/3] Frontend   : already running (80)'
} else {
  Start-Process powershell -ArgumentList '-NoExit', '-Command', 'npm run dev' -WorkingDirectory (Join-Path $root 'frontend')
  Write-Host '[3/3] Frontend   : starting in a new window (vite)...'
}

# ---- wait then open the browser ----
Write-Host 'Waiting for 8080 + 80 ...'
$ready = $false
for ($i = 0; $i -lt 60; $i++) {
  if ((Test-Port 8080) -and (Test-Port 80)) { $ready = $true; break }
  Start-Sleep -Seconds 2
}
if ($ready) {
  Write-Host ''
  Write-Host 'READY  ->  http://127.0.0.1/'
  Write-Host 'Login  ->  stu_chen / admin123   (student portal, lands on /student)'
  Write-Host '           admin    / admin123   (RuoYi console, management side)'
  Write-Host '           teacher_li / admin123 (teacher workspace)'
  Start-Process 'http://127.0.0.1/'
} else {
  Write-Host 'NOT READY. Check the backend window, or .local\logs\sys-error.log'
  exit 1
}
