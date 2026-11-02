#!/usr/bin/env python3
"""Build the real arm64 WidgetKit extension on macOS; activate HBuilder cloud embedding only after success.
python3 build-extension.py --profile /path/to/LiveActivity.mobileprovision
No certificates, private keys or provisioning profiles belong in Git.
"""
from pathlib import Path
import argparse, json, platform, plistlib, shutil, subprocess, tempfile

root = Path(__file__).resolve().parents[2]
plugin = root / 'uni_modules/xiaoxing-native/utssdk/app-ios'
name = 'XiaoxingLiveActivityExtension'
bundle = 'com.dfyj.xiaoxing.LiveActivity'

def run(*args):
    return subprocess.check_output(args, text=True).strip()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--profile', type=Path, required=True)
    args = parser.parse_args()
    if platform.system() != 'Darwin':
        raise SystemExit('This extension requires macOS + Xcode. No embedding configuration has been changed.')
    profile = args.profile.expanduser().resolve(strict=True)
    provisioning = plistlib.loads(subprocess.check_output(['security', 'cms', '-D', '-i', str(profile)]))
    app_id = provisioning.get('Entitlements', {}).get('application-identifier', '')
    if app_id.split('.', 1)[-1] != bundle:
        raise SystemExit('Provisioning profile must target ' + bundle)
    manifest = json.loads((root / 'manifest.json').read_text())
    sdk = run('xcrun', '--sdk', 'iphoneos', '--show-sdk-path')
    with tempfile.TemporaryDirectory(prefix='xiaoxing-live-') as temporary:
        product = Path(temporary) / (name + '.appex')
        product.mkdir()
        subprocess.run(['xcrun', '--sdk', 'iphoneos', 'swiftc', '-sdk', sdk,
            '-target', 'arm64-apple-ios16.2', '-swift-version', '5', '-O', '-parse-as-library',
            '-application-extension', '-module-name', name,
            '-framework', 'ActivityKit', '-framework', 'WidgetKit', '-framework', 'SwiftUI', '-framework', 'Foundation',
            '-Xlinker', '-rpath', '-Xlinker', '/usr/lib/swift',
            str(plugin / 'XiaoxingReminderAttributes.swift'), str(Path(__file__).with_name('XiaoxingLiveActivity.swift')),
            '-o', str(product / name)], check=True)
        info = {'CFBundleIdentifier': bundle, 'CFBundleExecutable': name, 'CFBundleName': name,
            'CFBundleDisplayName': '小醒提醒', 'CFBundlePackageType': 'XPC!', 'CFBundleInfoDictionaryVersion': '6.0',
            'CFBundleVersion': str(manifest['versionCode']), 'CFBundleShortVersionString': manifest['versionName'],
            'MinimumOSVersion': '16.2', 'CFBundleSupportedPlatforms': ['iPhoneOS'], 'UIDeviceFamily': [1, 2],
            'NSExtension': {'NSExtensionPointIdentifier': 'com.apple.widgetkit-extension'}}
        (product / 'Info.plist').write_bytes(plistlib.dumps(info))
        # Ad-hoc signature permits local inspection; HBuilder signs with the extension profile and app certificate.
        subprocess.run(['codesign', '--force', '--sign', '-', str(product)], check=True)
        destination = plugin / 'Plugins' / product.name
        shutil.copytree(product, destination, dirs_exist_ok=True)
    resources = root / 'nativeResources/ios'
    resources.mkdir(parents=True, exist_ok=True)
    saved = resources / 'xiaoxing-live.mobileprovision'
    if saved.resolve() != profile:
        shutil.copy2(profile, saved)
    configuration = resources / 'ios-extension.json'
    existing = json.loads(configuration.read_text()) if configuration.exists() else {}
    existing[name+'.appex'] = {'identifier': bundle, 'profile': saved.name,
        'plists': {'CFBundleVersion': str(manifest['versionCode']), 'CFBundleShortVersionString': manifest['versionName']}}
    configuration.write_text(json.dumps(existing, ensure_ascii=False, indent=2)+'\n')
    print('Built extension and configured HBuilder embedding. Create a custom base or signed IPA and verify on iPhone.')

if __name__ == '__main__':
    main()
