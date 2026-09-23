param([string]$HBuilderRoot = 'D:/HBuilder X')
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path $PSScriptRoot -Parent
$generated = Join-Path $appRoot 'unpackage/dist/build/app-android/.uniappx/android/src'
$native = Join-Path $appRoot 'uni_modules/xiaoxing-native/utssdk/app-android'
$output = Join-Path $appRoot 'output'
New-Item -ItemType Directory -Force -Path $output | Out-Null
# Resource builds emit page Kotlin but leave plugin adapters to the cloud build.
# This adapter mirrors the four UTS exports for a local whole-page type check.
# Native implementation files are compiled directly; this does not create an APK.
$bridge = @'
package uts.sdk.modules.xiaoxingNative
import io.dcloud.uts.*
fun secureRead(key: String): String = XiaoxingAndroid.read(UTSAndroid.getAppContext()!!, key)
fun secureWrite(key: String, value: String): Boolean = XiaoxingAndroid.write(UTSAndroid.getAppContext()!!, key, value)
fun randomHex(length: Number): String = XiaoxingAndroid.random(length.toInt())
fun nativeCall(action: String, input: String, callback: (String) -> Unit) {
 val activity = UTSAndroid.getUniActivity()
 if (activity == null) { callback("{}"); return }
 if (action == "files.pick") XiaoxingAndroid.pick(activity, callback)
 else if (action.startsWith("audio.")) XiaoxingRecorder.call(activity, action, input, callback)
 else if (action.startsWith("update.")) XiaoxingUpdates.call(activity, action, input, callback)
 else if (action.startsWith("stream.")) XiaoxingStream.call(activity, action, input, callback)
 else XiaoxingNotifications.call(activity, action, input, callback)
}
'@
$bridgeFile = Join-Path $output 'NativeCompileBridge.kt'
[IO.File]::WriteAllText($bridgeFile, $bridge, [Text.UTF8Encoding]::new($false))
$files = @((Get-ChildItem -Recurse -LiteralPath $generated -Filter '*.kt').FullName)
if ($files.Count -lt 10) { throw 'Build Android resources first.' }
$files += @((Get-ChildItem -LiteralPath $native -Filter '*.kt').FullName)
$files += $bridgeFile
$arguments = [Collections.Generic.List[string]]::new()
foreach ($file in $files) { $arguments.Add('"' + $file.Replace('\','/') + '"') }
$classpath = ((Get-ChildItem -LiteralPath (Join-Path $HBuilderRoot 'plugins/uniapp-runextension/lib2') -Filter '*.jar').FullName -join ';')
$arguments.Add('-classpath')
$arguments.Add('"' + $classpath.Replace('\','/') + '"')
$plugin = Join-Path $HBuilderRoot 'plugins/uniapp-uts-v1/node_modules/@dcloudio/uni-uts-v1/lib/kotlin/lib/uts-kotlin-compiler-plugin.jar'
$arguments.Add('"-Xplugin=' + $plugin.Replace('\','/') + '"')
$arguments.Add('-d')
$arguments.Add('"' + (Join-Path $output 'android-generated-check.jar').Replace('\','/') + '"')
$argumentFile = Join-Path $output 'android-generated-check.args'
[IO.File]::WriteAllText($argumentFile, ($arguments -join "`n"), [Text.UTF8Encoding]::new($false))
& (Join-Path $HBuilderRoot 'plugins/uniapp-runextension/kotlinc/bin/kotlinc.bat') "@$argumentFile"
if ($LASTEXITCODE -ne 0) { throw 'Generated Android Kotlin type check failed.' }
Write-Output 'PASS: all generated pages, shared Kotlin and native Android implementations compile.'
