$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (!(Test-Path 'output/stage1-classpath.txt')) {
        throw 'Generate the backend test dependency classpath in output/stage1-classpath.txt first (including spring-test and Mockito).'
    }
    $testClasspath = (Get-Content 'output/stage1-classpath.txt' -Raw).Trim().Replace('\', '/') + ';code/back/target/classes'
    New-Item -ItemType Directory -Force 'output/login-cookie-regression' | Out-Null
    $compileArguments = '-encoding UTF-8 --release 17 -cp "' + $testClasspath + '" -d output/login-cookie-regression code/back/src/main/java/org/springblade/common/filter/CookieSecureFilter.java code/back/src/main/java/org/springblade/modules/auth/handler/BackendCompatibilityExceptionHandler.java scripts/LoginCookieRegression.java'
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/login-cookie-javac.args'), $compileArguments, (New-Object System.Text.UTF8Encoding($false)))
    & javac '@output/login-cookie-javac.args'
    if ($LASTEXITCODE -ne 0) { throw 'Login cookie regression compilation failed' }
    $runArguments = '-cp "output/login-cookie-regression;' + $testClasspath + '" LoginCookieRegression'
    [System.IO.File]::WriteAllText((Join-Path (Get-Location) 'output/login-cookie-java.args'), $runArguments, (New-Object System.Text.UTF8Encoding($false)))
    & java '@output/login-cookie-java.args'
    if ($LASTEXITCODE -ne 0) { throw 'Login cookie regression failed' }
} finally {
    Pop-Location
}
