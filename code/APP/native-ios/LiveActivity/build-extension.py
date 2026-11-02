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

def write_project(directory):
    # Use Xcode's app-extension product type so Swift and linker settings are
    # selected together. A plain swiftc executable is not a WidgetKit extension.
    objects = {}
    def add(key, isa, **values):
        objects[key] = {'isa': isa, **values}
        return key
    sources = []
    references = []
    for i, filename in enumerate(['XiaoxingReminderAttributes.swift', 'XiaoxingLiveActivity.swift']):
        ref = add(f'F{i:023d}', 'PBXFileReference', lastKnownFileType='sourcecode.swift', path=filename, sourceTree='<group>')
        references.append(ref)
        sources.append(add(f'B{i:023d}', 'PBXBuildFile', fileRef=ref))
    avatar = add('A0000000000000000000000C', 'PBXFileReference', lastKnownFileType='image.png', path='live-activity-avatar.png', sourceTree='<group>')
    references.append(avatar)
    avatar_build = add('A0000000000000000000000D', 'PBXBuildFile', fileRef=avatar)
    resources = add('A0000000000000000000000E', 'PBXResourcesBuildPhase', buildActionMask=2147483647, files=[avatar_build], runOnlyForDeploymentPostprocessing=0)
    product = add('A00000000000000000000001', 'PBXFileReference', explicitFileType='wrapper.app-extension', path=name+'.appex', sourceTree='BUILT_PRODUCTS_DIR')
    products = add('A00000000000000000000002', 'PBXGroup', children=[product], name='Products', sourceTree='<group>')
    group = add('A00000000000000000000003', 'PBXGroup', children=references+[products], sourceTree='<group>')
    phase = add('A00000000000000000000004', 'PBXSourcesBuildPhase', buildActionMask=2147483647, files=sources, runOnlyForDeploymentPostprocessing=0)
    frameworks = add('A00000000000000000000005', 'PBXFrameworksBuildPhase', buildActionMask=2147483647, files=[], runOnlyForDeploymentPostprocessing=0)
    settings = {'PRODUCT_NAME': name, 'PRODUCT_BUNDLE_IDENTIFIER': bundle,
        'SDKROOT': 'iphoneos', 'SUPPORTED_PLATFORMS': 'iphoneos', 'ARCHS': 'arm64',
        'IPHONEOS_DEPLOYMENT_TARGET': '16.2', 'SWIFT_VERSION': '5.0',
        'APPLICATION_EXTENSION_API_ONLY': 'YES', 'SKIP_INSTALL': 'YES',
        'INFOPLIST_FILE': 'Info.plist', 'GENERATE_INFOPLIST_FILE': 'NO',
        'SWIFT_OPTIMIZATION_LEVEL': '-O', 'CODE_SIGNING_ALLOWED': 'NO',
        'STRIP_INSTALLED_PRODUCT': 'NO', 'COPY_PHASE_STRIP': 'NO',
        'OTHER_LDFLAGS': ['$(inherited)', '-e', '_NSExtensionMain'],
        'LD_RUNPATH_SEARCH_PATHS': ['$(inherited)', '@executable_path/Frameworks', '@executable_path/../../Frameworks']}
    config = add('A00000000000000000000006', 'XCBuildConfiguration', buildSettings=settings, name='Release')
    configs = add('A00000000000000000000007', 'XCConfigurationList', buildConfigurations=[config], defaultConfigurationIsVisible=0, defaultConfigurationName='Release')
    project_config = add('A00000000000000000000008', 'XCBuildConfiguration', buildSettings={}, name='Release')
    project_configs = add('A00000000000000000000009', 'XCConfigurationList', buildConfigurations=[project_config], defaultConfigurationIsVisible=0, defaultConfigurationName='Release')
    target = add('A0000000000000000000000A', 'PBXNativeTarget', buildConfigurationList=configs,
        buildPhases=[phase, frameworks, resources], buildRules=[], dependencies=[], name=name,
        productName=name, productReference=product, productType='com.apple.product-type.app-extension')
    project = add('A0000000000000000000000B', 'PBXProject', buildConfigurationList=project_configs,
        compatibilityVersion='Xcode 14.0', developmentRegion='en', hasScannedForEncodings=0,
        knownRegions=['en', 'Base'], mainGroup=group, productRefGroup=products,
        projectDirPath='', projectRoot='', targets=[target])
    def encode(value):
        if isinstance(value, dict):
            return '{' + ''.join(json.dumps(str(k))+ ' = '+encode(v)+';\n' for k,v in value.items()) + '}'
        if isinstance(value, list):
            return '(' + ','.join(encode(v) for v in value) + ')'
        return json.dumps(value)
    path = directory / (name + '.xcodeproj')
    path.mkdir()
    (path/'project.pbxproj').write_text('// !$*UTF8*$!\n'+encode({'archiveVersion':1, 'classes':{}, 'objectVersion':56, 'objects':objects, 'rootObject':project}), encoding='utf-8')
    return path

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
    with tempfile.TemporaryDirectory(prefix='xiaoxing-live-') as temporary:
        directory = Path(temporary)
        shutil.copy2(plugin/'XiaoxingReminderAttributes.swift', directory/'XiaoxingReminderAttributes.swift')
        shutil.copy2(Path(__file__).with_name('XiaoxingLiveActivity.swift'), directory/'XiaoxingLiveActivity.swift')
        shutil.copy2(root/'static/avatars/assistant/A3.png', directory/'live-activity-avatar.png')
        project = write_project(directory)
        info = {'CFBundleIdentifier': bundle, 'CFBundleExecutable': name, 'CFBundleName': name,
            'CFBundleDisplayName': '小醒提醒', 'CFBundlePackageType': 'XPC!', 'CFBundleInfoDictionaryVersion': '6.0',
            'CFBundleVersion': str(manifest['versionCode']), 'CFBundleShortVersionString': manifest['versionName'],
            'MinimumOSVersion': '16.2', 'CFBundleSupportedPlatforms': ['iPhoneOS'], 'UIDeviceFamily': [1, 2],
            'UIRequiredDeviceCapabilities': ['arm64'],
            'NSExtension': {'NSExtensionPointIdentifier': 'com.apple.widgetkit-extension'}}
        (directory/'Info.plist').write_bytes(plistlib.dumps(info))
        subprocess.run(['plutil', '-lint', str(project/'project.pbxproj'), str(directory/'Info.plist')], check=True)
        products = directory/'products'
        subprocess.run(['xcodebuild', '-project', str(project), '-target', name,
            '-configuration', 'Release', '-sdk', 'iphoneos', 'build',
            'CODE_SIGNING_ALLOWED=NO', 'CONFIGURATION_BUILD_DIR='+str(products)], check=True)
        product = products/(name+'.appex')
        if not (product/'live-activity-avatar.png').is_file():
            raise SystemExit('Missing Live Activity avatar resource in built extension')
        # Reject the previous hand-built executable before it can overwrite a good bundle.
        subprocess.run(['python3', str(Path(__file__).with_name('verify-extension.py')), str(product)], check=True)
        subprocess.run(['codesign', '--force', '--sign', '-', str(product)], check=True)
        subprocess.run(['codesign', '--verify', '--strict', str(product)], check=True)
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
        'plists': {'CFBundleVersion': str(manifest['versionCode']), 'CFBundleShortVersionString': manifest['versionName'],
            'UIRequiredDeviceCapabilities': ['arm64']}}
    configuration.write_text(json.dumps(existing, ensure_ascii=False, indent=2)+'\n')
    print('Built extension and configured HBuilder embedding. Create a custom base or signed IPA and verify on iPhone.')

if __name__ == '__main__':
    main()
