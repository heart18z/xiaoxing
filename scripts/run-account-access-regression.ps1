$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $testClasspath = (Get-Content 'output/stage1-classpath.txt' -Raw).Trim().Replace('\', '/') + ';code/back/target/classes;output/jakarta.mail-2.0.3.jar;output/stage1-h2.jar'
    New-Item -ItemType Directory -Force 'output/account130' | Out-Null
    $sources = @(
        'auth/service/BladeUserDetailService', 'smartreminder/config/RegistrationMailSchema',
        'smartreminder/controller/AccountAccessController', 'smartreminder/controller/SmartReminderController',
        'smartreminder/service/AccountAccessService', 'smartreminder/service/AiConfigService',
        'smartreminder/service/EmailVerificationService', 'smartreminder/service/RegistrationMailService',
        'smartreminder/service/SmartReminderService', 'smartreminder/support/AppRoleGuard'
    ) | ForEach-Object { 'code/back/src/main/java/org/springblade/modules/' + $_ + '.java' }
    $compile = '-encoding UTF-8 -parameters --release 17 -cp "' + $testClasspath + '" -d output/account130 ' + ($sources -join ' ') + ' scripts/AccountAccessRegression.java scripts/RegistrationMailRegression.java'
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/account130-all-javac.args'), $compile, (New-Object System.Text.UTF8Encoding($false)))
    & javac '@output/account130-all-javac.args'
    if ($LASTEXITCODE -ne 0) { throw 'Account access regression compilation failed' }
    foreach ($test in @('AccountAccessRegression','RegistrationMailRegression')) {
        $run = '-cp "output/account130;' + $testClasspath + '" ' + $test
        [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/account130-run.args'), $run, (New-Object System.Text.UTF8Encoding($false)))
        & java '@output/account130-run.args'
        if ($LASTEXITCODE -ne 0) { throw ($test + ' failed') }
    }
} finally { Pop-Location }
