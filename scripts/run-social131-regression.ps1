$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $testClasspath = 'output/account130;' + (Get-Content 'output/stage1-classpath.txt' -Raw).Trim().Replace('\', '/') + ';code/back/target/classes;output/jakarta.mail-2.0.3.jar;output/stage1-h2.jar'
    New-Item -ItemType Directory -Force 'output/social131' | Out-Null
    $sources = @('service/SmartReminderService','push/PushOutbox','push/PushSchema','push/PushDeliveryWorker') | ForEach-Object { 'code/back/src/main/java/org/springblade/modules/smartreminder/' + $_ + '.java' }
    $compile = '-encoding UTF-8 -parameters --release 17 -cp "' + $testClasspath + '" -d output/social131 ' + ($sources -join ' ') + ' scripts/Social131Regression.java scripts/PushRegression.java scripts/FriendRemarkRegression.java'
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/social131-javac.args'), $compile, (New-Object System.Text.UTF8Encoding($false)))
    & javac '@output/social131-javac.args'
    if ($LASTEXITCODE -ne 0) { throw 'Social regression compilation failed' }
    foreach ($test in @('Social131Regression','org.springblade.modules.smartreminder.push.PushRegression','FriendRemarkRegression')) {
        $run = '-cp "output/social131;' + $testClasspath + '" ' + $test
        [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/social131-run.args'), $run, (New-Object System.Text.UTF8Encoding($false)))
        & java '@output/social131-run.args'
        if ($LASTEXITCODE -ne 0) { throw ($test + ' failed') }
    }
} finally { Pop-Location }
