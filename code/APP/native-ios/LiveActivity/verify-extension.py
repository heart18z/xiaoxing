#!/usr/bin/env python3
"""Structural checks for this arm64 WidgetKit bundle; not App Store validation."""
from pathlib import Path
import argparse
import json
import plistlib
import struct


def inspect(bundle):
    info = plistlib.loads((bundle / 'Info.plist').read_bytes())
    errors = []
    capabilities = info.get('UIRequiredDeviceCapabilities', [])
    arm64 = ('arm64' in capabilities if isinstance(capabilities, list)
             else capabilities.get('arm64') is True)
    if not arm64:
        errors.append('90502: Info.plist must require arm64')
    if info.get('NSExtension', {}).get('NSExtensionPointIdentifier') != 'com.apple.widgetkit-extension':
        errors.append('Not a WidgetKit extension')
    binary = (bundle / info['CFBundleExecutable']).read_bytes()
    header = struct.unpack_from('<8I', binary)
    if header[0] != 0xfeedfacf or header[1] != 0x100000c or header[3] != 2:
        raise ValueError('Expected a thin arm64 iPhoneOS executable')
    entry = None
    symtab = None
    text = None
    text_size = 0
    platform = None
    offset = 32
    for _ in range(header[4]):
        cmd, size = struct.unpack_from('<II', binary, offset)
        if size < 8 or offset + size > len(binary):
            raise ValueError('Invalid Mach-O load command')
        if cmd == 0x80000028:
            entry = struct.unpack_from('<Q', binary, offset + 8)[0]
        elif cmd == 2:
            symtab = struct.unpack_from('<4I', binary, offset + 8)
        elif cmd == 0x32:
            platform = struct.unpack_from('<I', binary, offset + 8)[0]
        elif cmd == 0x19 and binary[offset+8:offset+24].split(b'\0')[0] == b'__TEXT':
            text = struct.unpack_from('<4Q', binary, offset + 24)
            count = struct.unpack_from('<I', binary, offset + 64)[0]
            for i in range(count):
                section = offset + 72 + i * 80
                if binary[section:section+16].split(b'\0')[0] == b'__text':
                    text_size = struct.unpack_from('<Q', binary, section + 40)[0]
        offset += size
    symbols = {}
    if symtab:
        start, count, strings, length = symtab
        for i in range(count):
            index, kind, _, _, address = struct.unpack_from('<IBBHQ', binary, start + i * 16)
            name = binary[strings+index:strings+length].split(b'\0')[0].decode(errors='replace')
            if name in ('_main', '_NSExtensionMain') and not kind & 0xe0:
                symbols[name] = {'kind': kind, 'address': address}
    if platform != 2:
        errors.append('Expected iPhoneOS platform, not simulator/macOS')
    if entry is None or text is None or text_size == 0:
        errors.append('Missing entry point or executable text')
    elif '_main' in symbols and symbols['_main']['address'] == text[0] + entry - text[2]:
        errors.append('90898: LC_MAIN still points to the ordinary _main entry')
    if '_NSExtensionMain' not in symbols:
        errors.append('90898: no NSExtensionMain symbol; rebuild using the Xcode extension target')
    if errors:
        raise ValueError('; '.join(errors))
    return {'bundle': info['CFBundleIdentifier'], 'version': info['CFBundleShortVersionString'],
            'build': info['CFBundleVersion'], 'arm64Required': arm64,
            'entryOffset': entry, 'symbols': symbols, 'textBytes': text_size,
            'limitation': 'Structural checks only; cloud signing, Apple validation and iPhone runtime still required'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('bundle', type=Path)
    args = parser.parse_args()
    try:
        print(json.dumps(inspect(args.bundle), ensure_ascii=False, indent=2))
    except (ValueError, OSError, KeyError, struct.error) as error:
        parser.exit(1, str(error) + '\n')
