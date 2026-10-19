"""Build matching Lucide SVG nodes for H5 and a small vector icon font for APP.

Requires the project's lucide-static devDependency and Python fonttools.
Run from code/APP: python tests/build-ui-icons.py
"""
from pathlib import Path
import json
import shutil
import xml.etree.ElementTree as ET
from fontTools import subset

root = Path(__file__).resolve().parent.parent
source = root / 'node_modules/lucide-static'
names = {
    'arrowUp': 'arrow-up', 'arrowDown': 'arrow-down', 'calendar': 'calendar-days',
    'camera': 'camera', 'check': 'check', 'chevron': 'chevron-right',
    'clock': 'clock-3', 'close': 'x', 'down': 'chevron-down', 'edit': 'square-pen',
    'eraser': 'eraser', 'eye': 'eye', 'eyeOff': 'eye-off', 'filter': 'filter',
    'layers': 'layers', 'list': 'list', 'lock': 'lock-keyhole', 'logout': 'log-out',
    'menu': 'menu', 'microphone': 'mic', 'note': 'file-text', 'people': 'users-round',
    'plus': 'plus', 'qr': 'qr-code', 'reset': 'rotate-ccw', 'scan': 'scan-line',
    'search': 'search', 'settings': 'settings', 'spark': 'sparkles', 'tag': 'tag',
    'tipInfo': 'info', 'tipSuccess': 'circle-check', 'tipError': 'circle-alert',
    'trash': 'trash-2', 'user': 'user-round', 'userMinus': 'user-round-minus',
    'userPlus': 'user-round-plus',
}
points = json.loads((source / 'font/codepoints.json').read_text())
glyphs = {name: chr(points[icon]) for name, icon in names.items()}
target = root / 'static/fonts'
target.mkdir(exist_ok=True)
font = subset.load_font(str(source / 'font/lucide.ttf'), subset.Options())
selection = subset.Subsetter()
selection.populate(unicodes=[ord(char) for char in glyphs.values()])
selection.subset(font)
subset.save_font(font, str(target / 'xiaoxing-ui.ttf'), subset.Options())
shutil.copyfile(source / 'LICENSE', target / 'LUCIDE-LICENSE.txt')
(root / 'core/ui-icons.uts').write_text(
    '// Generated from Lucide. Rebuild with tests/build-ui-icons.py.\n'
    + 'const glyphs: UTSJSONObject = ' + json.dumps(glyphs, indent=2) + ';\n'
    + "export function uiIconGlyph(name: string): string { return (glyphs[name] as string) ?? (glyphs['note'] as string); }\n"
    + "export function uiIconColor(tone: string): string {\n"
    + "  const colors: UTSJSONObject = {blue:'#3267ed',purple:'#8a63d8',ink:'#243550',green:'#19ad88',red:'#ef6675',white:'#ffffff',muted:'#8e9ab0',violet:'#885bdd',amber:'#d99b37'};\n"
    + "  return (colors[tone] as string) ?? '#8e9ab0';\n}\n",
    encoding='utf-8')
nodes = {}
for name, icon in names.items():
    tree = ET.fromstring((source / 'icons' / (icon + '.svg')).read_text())
    nodes[name] = [{'tag': child.tag.split('}')[-1], 'attrs': child.attrib} for child in tree]
(root.parent / 'front/src/page/smart-reminder/lucideIcons.js').write_text(
    '// Generated from lucide-static; only icons used by the mobile UI are included.\n'
    + 'export const lucideIcons = ' + json.dumps(nodes, indent=2) + ';\n', encoding='utf-8')
shutil.copyfile(source / 'LICENSE', root.parent / 'front/public/LUCIDE-LICENSE.txt')
print(f'Built {len(names)} vector icons; APP font { (target / "xiaoxing-ui.ttf").stat().st_size } bytes')
