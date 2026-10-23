#requires -Version 7.4
param(
    [ValidateSet('Start', 'Install', 'Verify')][string]$Action = 'Start',
    [string]$Apk,
    [string]$TestClass
)
$ErrorActionPreference = 'Stop'
$project = Split-Path $PSScriptRoot -Parent
$env:ANDROID_HOME = 'D:\Android\Sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:ANDROID_AVD_HOME = Join-Path $env:USERPROFILE '.android\avd'
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot'
$adb = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
$emulator = Join-Path $env:ANDROID_HOME 'emulator\emulator.exe'
$avd = 'Shared_Pixel7_API35'
$serial = 'emulator-5554'
$evidence = Join-Path $project 'qa\current'
New-Item -ItemType Directory -Force $evidence | Out-Null

function Invoke-Adb {
    & $adb -s $serial @args
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $args" }
}

if (!(Test-Path $adb) -or !(Test-Path $emulator)) { throw 'Shared Android SDK is missing: D:\Android\Sdk' }
$avdConfig = Join-Path $env:ANDROID_AVD_HOME "$avd.avd\config.ini"
if (!(Test-Path $avdConfig) -or (Get-Content $avdConfig) -notcontains 'hw.keyboard=yes') {
    throw 'The shared AVD must have hw.keyboard=yes. Enable its host keyboard and restart the existing device.'
}
$devices = & $adb devices
if ($devices -match "^$serial\s") {
    $name = & $adb -s $serial emu avd name
    if ($name -notcontains $avd) { throw "Port 5554 belongs to a different device; expected $avd" }
} else {
    if ((& $emulator -list-avds) -notcontains $avd) { throw "Shared AVD is missing: $avd. Do not create a temporary device." }
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $avd, '-port', '5554', '-gpu', 'host', '-no-snapshot-load') -WindowStyle Hidden -RedirectStandardOutput (Join-Path $evidence 'emulator.log') -RedirectStandardError (Join-Path $evidence 'emulator-error.log') | Out-Null
}
$deadline = (Get-Date).AddMinutes(3)
do {
    $booted = & $adb -s $serial shell getprop sys.boot_completed 2>$null
    if ($booted -eq '1') { break }
    Start-Sleep -Seconds 2
} while ((Get-Date) -lt $deadline)
if ($booted -ne '1') { throw 'Shared emulator did not finish booting within 3 minutes.' }
Write-Host "Using $avd ($serial) from $env:ANDROID_HOME"
if ($Action -eq 'Start') { return }

if ($Action -eq 'Install') {
    if (!$Apk) { $Apk = Join-Path $project 'app\build\outputs\apk\review\app-review.apk' }
    if (!(Test-Path -LiteralPath $Apk)) { throw "APK does not exist: $Apk" }
    Invoke-Adb install -r $Apk
    Invoke-Adb shell am start -n com.refuge.next.compose/com.refuge.next.MainActivity
    return
}

Push-Location $project
try {
    & .\gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleReview
    if ($LASTEXITCODE -ne 0) { throw 'Build, lint, or unit tests failed.' }
    # Direct instrumentation preserves the user's login and lets us collect
    # screenshots before a Gradle connected-test task can uninstall the app.
    Invoke-Adb install -r 'app\build\outputs\apk\debug\app-debug.apk'
    Invoke-Adb install -r 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
    $testArgs = @('shell', 'am', 'instrument', '-w', '-r')
    if ($TestClass) { $testArgs += @('-e', 'class', $TestClass) }
    $testArgs += 'com.refuge.next.compose.test/androidx.test.runner.AndroidJUnitRunner'
    $result = Invoke-Adb @testArgs | Tee-Object -FilePath (Join-Path $evidence 'instrumentation.txt')
    # PowerShell 7.4+ preserves the binary native stdout stream. Export only
    # test evidence; never collect shared preferences or other account files.
    & $adb -s $serial exec-out run-as com.refuge.next.compose tar -C files -cf - qa > (Join-Path $evidence 'device-evidence.tar')
    if ($LASTEXITCODE -ne 0) { throw 'Could not export device evidence.' }
    & tar.exe -xf (Join-Path $evidence 'device-evidence.tar') -C $evidence
    if ($LASTEXITCODE -ne 0) { throw 'Could not extract device evidence.' }
    if (($result -join "`n") -notmatch 'OK \(\d+ tests?\)' -or ($result -join "`n") -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
        throw 'Instrumented tests failed; see qa/current/instrumentation.txt'
    }
    Write-Host 'Automated checks passed. Inspect captures and report any skipped live-account checks before delivery.'
} finally {
    Pop-Location
}
