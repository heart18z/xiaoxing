$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    $downloadClasspath = (Get-Content 'output/stage1-classpath.txt' -Raw).Trim().Replace('\', '/') + ';code/back/target/classes;output/stage1-h2.jar'
    New-Item -ItemType Directory -Force 'output/download-page' | Out-Null
    $sources = @('service/AppReleaseService','service/DownloadPageService','config/DownloadPageSchema','controller/DownloadPageController') | ForEach-Object { 'code/back/src/main/java/org/springblade/modules/smartreminder/' + $_ + '.java' }
    $compile = '-encoding UTF-8 -parameters --release 17 -cp "' + $downloadClasspath + '" -d output/download-page ' + ($sources -join ' ') + ' scripts/DownloadPageRegression.java scripts/AppReleaseRegression.java'
    [IO.File]::WriteAllText((Join-Path (Get-Location) 'output/download-page-javac.args'), $compile, [Text.UTF8Encoding]::new($false))
    & javac '@output/download-page-javac.args'
    if ($LASTEXITCODE -ne 0) { throw 'Download page compilation failed' }
    foreach ($test in @('DownloadPageRegression','AppReleaseRegression')) {
        $run = '-cp "output/download-page;' + $downloadClasspath + '" ' + $test
        [IO.File]::WriteAllText((Join-Path (Get-Location) 'output/download-page-run.args'), $run, [Text.UTF8Encoding]::new($false))
        & java '@output/download-page-run.args'
        if ($LASTEXITCODE -ne 0) { throw ($test+' failed') }
    }
} finally { Pop-Location }
