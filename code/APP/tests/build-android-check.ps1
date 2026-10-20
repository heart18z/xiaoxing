param([string]$HBuilderRoot = 'D:/HBuilder X')
$ErrorActionPreference = 'Stop'
$appRoot = Split-Path $PSScriptRoot -Parent
$pluginRoot = Join-Path $HBuilderRoot 'plugins/uniapp-cli-vite'
$node = Join-Path $HBuilderRoot 'plugins/node/node.exe'
$cli = Join-Path $pluginRoot 'node_modules/@dcloudio/vite-plugin-uni/bin/uni.js'
foreach ($required in @($node, $cli)) {
  if (!(Test-Path -LiteralPath $required)) { throw "Missing HBuilderX compiler: $required" }
}
$output = Join-Path $appRoot 'output'
New-Item -ItemType Directory -Force -Path $output | Out-Null
$packagePath = (Join-Path $pluginRoot 'package.json').Replace('\', '/') | ConvertTo-Json -Compress
$sharedPath = (Join-Path $pluginRoot 'node_modules/@vue/shared/dist/shared.esm-bundler.js').Replace('\', '/') | ConvertTo-Json -Compress
$config = @"
import {createRequire} from 'node:module';
const nativeRequire=createRequire($packagePath);
const uni=nativeRequire('@dcloudio/vite-plugin-uni');
export default {plugins:[uni.default({vueOptions:{compiler:nativeRequire('@vue/compiler-sfc')}})],resolve:{alias:{'@vue/shared':$sharedPath}}};
"@
$configFile = Join-Path $output 'android-check-vite.config.mjs'
[IO.File]::WriteAllText($configFile, $config, [Text.UTF8Encoding]::new($false))
$buildEnv = @{
  HX_APP_ROOT = $HBuilderRoot
  UNI_INPUT_DIR = $appRoot
  UNI_OUTPUT_DIR = (Join-Path $appRoot 'unpackage/dist/build/app-android')
  UNI_PLATFORM = 'app'
  UNI_APP_PLATFORM = 'android'
  UNI_UTS_PLATFORM = 'app-android'
  UNI_APP_X = 'true'
  NODE_ENV = 'production'
}
$previous = @{}
foreach ($name in $buildEnv.Keys) {
  $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
  [Environment]::SetEnvironmentVariable($name, $buildEnv[$name], 'Process')
}
Push-Location -LiteralPath $appRoot
try {
  & $node $cli build -p app --config $configFile
  if ($LASTEXITCODE -ne 0) { throw 'Android resource generation failed.' }
  # Resource generation alone does not type-check the generated page Kotlin.
  & (Join-Path $PSScriptRoot 'check-android-generated.ps1') -HBuilderRoot $HBuilderRoot
} finally {
  Pop-Location
  foreach ($name in $buildEnv.Keys) {
    [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
  }
}
