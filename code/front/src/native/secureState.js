import { registerPlugin } from '@capacitor/core';
import { isNative } from './runtime';
export const NativeSupport = registerPlugin('XiaoxingNative');
let state = {}, queue = Promise.resolve(), persistenceError;
export const secureNames = new Set(['token', 'refreshToken', 'userInfo']);
export const secureGet = name => state[name];
export function secureSet(name, value) {
  if (value === undefined) delete state[name]; else state[name] = value;
  const snapshot = JSON.stringify(state);
  queue = queue.catch(() => {}).then(() => NativeSupport.writeState({ value: snapshot })).then(() => { persistenceError = null; }).catch(error => {
    persistenceError = error;
    window.dispatchEvent(new Event('native:storage-error'));
  });
}
export async function flushSecureState() { await queue; if (persistenceError) throw persistenceError; }
export async function hydrateSecureState() {
  if (!isNative) return;
  const saved = await NativeSupport.readState();
  state = saved.value ? JSON.parse(saved.value) : {};
  if (!state || typeof state !== 'object' || Array.isArray(state)) throw new Error('Invalid secure state');
  if (!state.installation) {
    const bytes = crypto.getRandomValues(new Uint8Array(32));
    // getRandomValues also works in older WKWebView/custom-scheme contexts where randomUUID is absent.
    const id = crypto.getRandomValues(new Uint8Array(16)); id[6] = (id[6] & 15) | 64; id[8] = (id[8] & 63) | 128;
    const hex = Array.from(id, b => b.toString(16).padStart(2, '0')).join('');
    const installationId = [hex.slice(0,8),hex.slice(8,12),hex.slice(12,16),hex.slice(16,20),hex.slice(20)].join('-');
    secureSet('installation', { installationId, installationSecret: Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('') });
    await flushSecureState();
  }
  for (const name of secureNames) { localStorage.removeItem('saber-' + name); sessionStorage.removeItem('saber-' + name); }
}
