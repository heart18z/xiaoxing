import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { webcrypto } from 'node:crypto';
import { notificationRoute } from '../src/native/notificationRoute.mjs';
test('notification opens only its authenticated recipient event', () => {
  assert.equal(notificationRoute({ recipientUserId: '123', eventId: '9223372036854775807' }, '123'), '/app/event/9223372036854775807');
  for (const user of ['', '124', null]) assert.equal(notificationRoute({ recipientUserId: '123', eventId: '99' }, user), null);
});
test('untrusted notification data cannot navigate arbitrary URLs or overflow IDs', () => {
  for (const eventId of ['https://evil.example', '../settings', '0', '-1', '123/../../me', '1?x=2', '12345678901234567890']) assert.equal(notificationRoute({ recipientUserId: '123', eventId }, '123'), null);
});
test('native app bundles assets and never configures a remote WebView server', () => {
  const config = JSON.parse(readFileSync('capacitor.config.json', 'utf8'));
  assert.equal(config.appId, 'com.dfyj.xiaoxing'); assert.equal(config.webDir, 'dist-native');
  assert.equal(config.server.url, undefined); assert.equal(config.server.allowNavigation, undefined);
});

test('secure state supports older WKWebView and persists only through the Keychain bridge', async () => {
  let saved = '', fail = false;
  const old = { window: globalThis.window, localStorage: globalThis.localStorage, sessionStorage: globalThis.sessionStorage, crypto: Object.getOwnPropertyDescriptor(globalThis, 'crypto') };
  globalThis.window = new EventTarget();
  globalThis.localStorage = globalThis.sessionStorage = { removeItem() {} };
  Object.defineProperty(globalThis, 'crypto', { configurable: true, value: { getRandomValues: bytes => webcrypto.getRandomValues(bytes) } });
  globalThis.__vaultFixture = { readState: async () => ({ value: saved }), writeState: async ({ value }) => { if (fail) throw new Error('Fixture storage failure'); saved = value; } };
  const source = readFileSync('src/native/secureState.js', 'utf8').replace(/^import .*;\r?\n/gm, '');
  try {
    const vault = await import('data:text/javascript;base64,' + Buffer.from('const isNative=true; const registerPlugin=()=>globalThis.__vaultFixture;\n' + source).toString('base64'));
    await vault.hydrateSecureState();
    const installation = vault.secureGet('installation');
    assert.match(installation.installationId, /^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/);
    assert.match(installation.installationSecret, /^[a-f0-9]{64}$/);
    vault.secureSet('token', 'fixture-token'); vault.secureSet('userInfo', { user_id: '1' }); await vault.flushSecureState();
    assert.equal(JSON.parse(saved).token, 'fixture-token');
    fail = true; vault.secureSet('token', 'second'); await assert.rejects(vault.flushSecureState());
    fail = false; vault.secureSet('token', undefined); await vault.flushSecureState();
    assert.equal(JSON.parse(saved).token, undefined);
  } finally {
    globalThis.window = old.window; globalThis.localStorage = old.localStorage; globalThis.sessionStorage = old.sessionStorage;
    Object.defineProperty(globalThis, 'crypto', old.crypto); delete globalThis.__vaultFixture;
  }
});

test('logout during an in-flight registration revokes the late binding instead of reviving it', async () => {
  const old = { window: globalThis.window, localStorage: globalThis.localStorage, fetch: globalThis.fetch };
  const listeners = {}, state = { installation: { installationId: '11111111-1111-4111-8111-111111111111', installationSecret: 'a'.repeat(64) } }, requests = [];
  let completeRegistration;
  globalThis.window = new EventTarget(); globalThis.localStorage = { getItem: () => 'zh-cn' };
  globalThis.fetch = async (url, options) => {
    requests.push({ url, body: JSON.parse(options.body) });
    if (url.endsWith('/register')) return new Promise(resolve => { completeRegistration = () => resolve({ ok: true, json: async () => ({ code: 200, data: { bindingId: 'late-binding', userId: '1', backendReady: true } }) }); });
    return { ok: true, json: async () => ({ code: 200, data: {} }) };
  };
  const plugin = { addListener: async (event, callback) => { listeners[event] = callback; return { remove: async () => {} }; }, checkPermissions: async () => ({ receive: 'granted' }), register: async () => { listeners.registration({ value: 'b'.repeat(64) }); }, removeAllDeliveredNotifications: async () => {} };
  globalThis.__pushFixture = { reactive: value => value, PushNotifications: plugin, App: plugin, Keyboard: plugin, Base64: { encode: value => value }, website: { tokenHeader: 'Blade-Auth' }, getToken: () => 'fixture-token', apiUrl: path => path, isNative: true, NativeSupport: { context: async () => ({ bundleId: 'com.dfyj.xiaoxing', environment: 'production' }) }, secureGet: name => state[name], secureSet: (name, value) => { state[name] = value; }, flushSecureState: async () => {}, notificationRoute };
  const source = readFileSync('src/native/notifications.js', 'utf8').replace(/^import .*;\r?\n/gm, '');
  const prefix = 'const {' + Object.keys(globalThis.__pushFixture).join(',') + '}=globalThis.__pushFixture;\n';
  try {
    const native = await import('data:text/javascript;base64,' + Buffer.from(prefix + source).toString('base64'));
    await native.startNativeNotifications({ afterEach() {}, isReady: async () => {} }, { getters: { token: 'fixture-token', userInfo: { user_id: '1' } }, watch() {} });
    for (let i=0;i<100&&!completeRegistration;i++) await new Promise(resolve => setTimeout(resolve, 1));
    assert.ok(completeRegistration, 'registration request started');
    window.dispatchEvent(new Event('native:logout')); completeRegistration();
    for (let i=0;i<100&&!requests.some(r=>r.url.endsWith('/revoke'));i++) await new Promise(resolve => setTimeout(resolve, 1));
    assert.equal(requests.find(r=>r.url.endsWith('/revoke'))?.body.bindingId, 'late-binding');
    assert.equal(state.pushBinding, undefined); assert.equal(native.pushState.registered, false);
  } finally {
    completeRegistration?.(); globalThis.window = old.window; globalThis.localStorage = old.localStorage; globalThis.fetch = old.fetch; delete globalThis.__pushFixture;
  }
});
