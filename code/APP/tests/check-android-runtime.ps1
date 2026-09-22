param([string]$HBuilderRoot = 'D:/HBuilder X')
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path $PSScriptRoot -Parent
$generated = Join-Path $appRoot 'unpackage/dist/build/app-android/.uniappx/android/src/index.kt'
$source = Get-Content -Raw -LiteralPath $generated
$runtimeImports = [regex]::Matches($source, '(?m)^import io\.dcloud\.uts\.[^\r\n]+') | ForEach-Object {$_.Value}
$encryption = [regex]::Match($source, '(?s)fun encryptPassword\(.*?(?=\r?\nfun login\()').Value
if (!$encryption) {throw 'Build Android resources first: encryptPassword is missing.'}
if (![regex]::IsMatch($source, 'UTSAndroid\.`typeof`')) {throw 'Generated response type checks are missing.'}
$probe = @'
val SM2_PUBLIC_KEY = "fixture"
fun randomHex(count: Number): String = "fixture"
fun sm2Encrypt(text: String, key: String, nonce: String): String = text
fun responseType(value: Any?): String = UTSAndroid.`typeof`(value)
'@
$output = Join-Path $appRoot 'output'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$probeFile = Join-Path $output 'AndroidRuntimeProbe.kt'
# Keep actual generated aliases: an alias can hide the name used by typeof.
($runtimeImports -join "`n") + "`n" + $probe + "`n" + $encryption | Set-Content -Encoding utf8 -LiteralPath $probeFile
$classPath = ((Get-ChildItem -LiteralPath (Join-Path $HBuilderRoot 'plugins/uniapp-runextension/lib2') -Filter '*.jar').FullName -join ';')
$compiler = Join-Path $HBuilderRoot 'plugins/uniapp-runextension/kotlinc/bin/kotlinc.bat'
$plugin = Join-Path $HBuilderRoot 'plugins/uniapp-uts-v1/node_modules/@dcloudio/uni-uts-v1/lib/kotlin/lib/uts-kotlin-compiler-plugin.jar'
& $compiler $probeFile -classpath $classPath "-Xplugin=$plugin" -d (Join-Path $output 'android-runtime-probe.jar')
if ($LASTEXITCODE -ne 0) {throw 'Generated Android runtime import/type-check/dispatcher compilation failed.'}
Write-Output 'PASS: generated runtime imports, typeof and login dispatchers compile together.'
