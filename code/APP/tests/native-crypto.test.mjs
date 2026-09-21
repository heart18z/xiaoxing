import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import { dependency, root } from './loader.mjs';

// UTS treats Array(16) as [16], and indexed writes cannot grow an array.
// Model that difference explicitly: ordinary Node tests otherwise hide the crash.
function NativeArray(...items) {
  return new Proxy(items, {
    set(target, key, value) {
      if (/^\d+$/.test(String(key)) && Number(key) >= target.length)
        throw new RangeError(`Index ${key} out of bounds for length ${target.length}`);
      return Reflect.set(target, key, value);
    },
  });
}

async function nativeCrypto() {
  const { outputFiles } = await dependency('esbuild').build({
    entryPoints: [root + 'core/sm2.uts'],
    bundle: true,
    write: false,
    platform: 'node',
    format: 'cjs',
    loader: { '.uts': 'ts' },
  });
  const context = { module: { exports: {} }, Array: NativeArray };
  vm.runInNewContext(outputFiles[0].text, context);
  return context.module.exports;
}

test('native array semantics permit crypto startup, SM3 blocks and SM2 login encryption', async () => {
  const crypto = await nativeCrypto();
  const reference = dependency('sm-crypto');
  for (const text of ['', 'abc', 'abcd'.repeat(16), '小醒密码🔔'.repeat(20)]) {
    const digest = crypto.bytesHex(crypto.sm3([...Buffer.from(text)]));
    assert.equal(digest, reference.sm3(text));
  }
  const pair = reference.sm2.generateKeyPairHex();
  for (const password of ['123456', '小醒密码🔔test']) {
    const ciphertext = crypto.sm2Encrypt(
      password,
      pair.publicKey,
      '1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef',
    );
    assert.equal(reference.sm2.doDecrypt(ciphertext, pair.privateKey, 0), password);
  }
});
