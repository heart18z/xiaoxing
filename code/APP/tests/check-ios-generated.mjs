import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

// Run after the iOS resource build: a JS mock cannot prove UTS callback retention.
const code = readFileSync(new URL('../unpackage/dist/build/app-ios/app-service.js', import.meta.url), 'utf8');
for (const [name, keepAlive] of [['onStreamEventByJs', true], ['nativeCallByJs', false]]) {
  const proxy = code.split('\n').find(line => line.includes(`name: "${name}"`));
  assert.ok(proxy, `${name} must be emitted by the iOS UTS compiler`);
  assert.match(proxy, new RegExp(`keepAlive: ${keepAlive}\\b`), `${name} has the wrong callback lifetime`);
}
console.log('PASS iOS generated bridge: persistent stream listener, one-shot ordinary calls');
