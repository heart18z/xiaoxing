$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (!(Test-Path 'output/stage1-classpath.txt') -or !(Test-Path 'output/stage1-h2.jar')) {
        throw 'Required: Maven dependency:build-classpath output in output/stage1-classpath.txt and H2 2.2.224 in output/stage1-h2.jar. Build code/back first.'
    }
    $previousClasspath = $env:CLASSPATH
    try {
        $env:CLASSPATH = (Get-Content 'output/stage1-classpath.txt' -Raw).Trim() + ';code/back/target/classes;output/stage1-h2.jar'
        & java '-Dfile.encoding=UTF-8' scripts/Stage1Regression.java
        if ($LASTEXITCODE -ne 0) { throw 'Stage 1 regression failed' }
    } finally { $env:CLASSPATH = $previousClasspath }
} finally { Pop-Location }
