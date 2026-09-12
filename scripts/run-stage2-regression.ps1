$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
$previousClasspath = $env:CLASSPATH
try {
    if (!(Test-Path 'output/stage1-classpath.txt') -or !(Test-Path 'output/stage1-h2.jar')) {
        throw 'Required: Maven dependency classpath in output/stage1-classpath.txt and H2 2.2.224 in output/stage1-h2.jar. Compile code/back first.'
    }
    New-Item -ItemType Directory -Force output/stage2-test-classes | Out-Null
    $env:CLASSPATH = (Get-Content output/stage1-classpath.txt -Raw).Trim() + ';code/back/target/classes;output/stage1-h2.jar'
    & javac -encoding UTF-8 -d output/stage2-test-classes scripts/Stage1Regression.java scripts/Stage2Regression.java
    if ($LASTEXITCODE -ne 0) { throw 'Regression compilation failed' }
    $env:CLASSPATH += ';output/stage2-test-classes'
    & java '-Dfile.encoding=UTF-8' Stage2Regression
    if ($LASTEXITCODE -ne 0) { throw 'Stage 2 regression failed' }
} finally {
    $env:CLASSPATH = $previousClasspath
    Pop-Location
}
