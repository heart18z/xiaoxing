import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { dependency, root } from './loader.mjs';

function files(dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const file = path.join(dir, entry.name);
    return entry.isDirectory() ? files(file) : file.endsWith('.uvue') ? [file] : [];
  });
}

test('native button content is text only so icon controls cannot silently disappear', () => {
  const invalid = [];
  for (const file of [...files(root + 'components'), ...files(root + 'pages')]) {
    const { descriptor } = dependency('@vue/compiler-sfc').parse(fs.readFileSync(file, 'utf8'));
    function walk(node) {
      if (node.tag === 'button' && node.children?.some(child => child.type === 1))
        invalid.push(path.relative(root, file) + ':' + node.loc.start.line);
      for (const child of node.children || []) walk(child);
    }
    walk(descriptor.template.ast);
  }
  assert.deepEqual(invalid, [], 'uni-app x native buttons do not support child components');
});
