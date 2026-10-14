$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $requestClasspath = 'output/social131;output/account130;' + (Get-Content 'output/stage1-classpath.txt' -Raw).Trim().Replace('\', '/') + ';code/back/target/classes;output/jakarta.mail-2.0.3.jar;output/stage1-h2.jar'
    New-Item -ItemType Directory -Force 'output/applications134' | Out-Null
    $sources = @('service/SmartReminderService') | ForEach-Object { 'code/back/src/main/java/org/springblade/modules/smartreminder/' + $_ + '.java' }
    $compile = '-encoding UTF-8 -parameters --release 17 -cp "' + $requestClasspath + '" -d output/applications134 ' + ($sources -join ' ') + ' scripts/FriendApplications134Regression.java scripts/Social131Regression.java'
    [IO.File]::WriteAllText((Join-Path (Get-Location) 'output/applications134-javac.args'), $compile, [Text.UTF8Encoding]::new($false))
    & javac '@output/applications134-javac.args'
    if ($LASTEXITCODE -ne 0) { throw 'Applications compilation failed' }
    foreach ($test in @('FriendApplications134Regression','Social131Regression')) {
        $run = '-cp "output/applications134;' + $requestClasspath + '" ' + $test
        [IO.File]::WriteAllText((Join-Path (Get-Location) 'output/applications134-run.args'), $run, [Text.UTF8Encoding]::new($false))
        & java '@output/applications134-run.args'
        if ($LASTEXITCODE -ne 0) { throw ($test+' failed') }
    }
} finally { Pop-Location }
