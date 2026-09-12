import { reactive } from 'vue';
import { PushNotifications } from '@capacitor/push-notifications';
import { App } from '@capacitor/app';
import { Keyboard } from '@capacitor/keyboard';
import { Base64 } from 'js-base64';
import website from '@/config/website';
import { getToken } from '@/utils/auth';
import { apiUrl, isNative } from './runtime';
import { NativeSupport, secureGet, secureSet, flushSecureState } from './secureState';
import { notificationRoute } from './notificationRoute.mjs';

export const pushState = reactive({ permission: 'prompt', registered: false, backendReady: false, busy: false, error: '' });
let router, store, context, deviceToken, generation = 0, pendingTap, operations = Promise.resolve();
const userId = () => getToken() ? String(store?.getters.userInfo?.user_id || '') : '';
const serial = operation => { const task = operations.catch(() => {}).then(operation); operations = task; return task; };
const refreshViews = () => {
  window.dispatchEvent(new Event('smart-reminder:badge-refresh'));
  window.dispatchEvent(new Event('smart-reminder:push-refresh'));
};
async function post(path, body, token) {
  const headers = { 'Content-Type': 'application/json', Authorization: `Basic ${Base64.encode(`${website.clientId}:${website.clientSecret}`)}` };
  if (token) headers[website.tokenHeader] = `bearer ${token}`;
  const controller = new AbortController(), timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch(apiUrl('/api/app/push/' + path), { method: 'POST', credentials: 'omit', headers, body: JSON.stringify(body), signal: controller.signal });
    const result = await response.json();
    if (!response.ok || result.code !== 200) throw new Error('Push service unavailable');
    return result.data;
  } finally { clearTimeout(timeout); }
}
function rememberRevocation(binding) {
  if (!binding?.bindingId) return;
  const pending = secureGet('pendingRevocations') || [];
  if (!pending.some(item => item.bindingId === binding.bindingId)) secureSet('pendingRevocations', [...pending, { ...secureGet('installation'), bindingId: binding.bindingId }]);
}
async function revokePending() {
  for (const item of [...(secureGet('pendingRevocations') || [])]) {
    await flushSecureState();
    await post('revoke', item);
    secureSet('pendingRevocations', (secureGet('pendingRevocations') || []).filter(value => value.bindingId !== item.bindingId));
    await flushSecureState();
  }
}
function logout() {
  generation++;
  pendingTap = null;
  rememberRevocation(secureGet('pushBinding'));
  secureSet('pushBinding', undefined);
  pushState.registered = false; pushState.backendReady = false;
  // Durable retry is independent of the now-expired OAuth token; a stale binding cannot revoke a new login.
  serial(revokePending).catch(() => { pushState.error = 'cleanup'; });
  PushNotifications.removeAllDeliveredNotifications().catch(() => {});
}
async function bindDevice() {
  const id = userId(), token = getToken(), revision = generation, apnsToken = deviceToken;
  if (!id || !token || !apnsToken) return;
  return serial(async () => {
    if (revision !== generation || id !== userId()) return;
    try { await revokePending(); } catch { /* New registration rotates the server binding; old revocation remains safe to retry. */ }
    await flushSecureState();
    const result = await post('register', { ...secureGet('installation'), token: apnsToken, environment: context.environment, bundleId: context.bundleId, appVersion: context.appVersion, language: localStorage.getItem('smart-ui-language') || 'zh-cn' }, token);
    if (revision !== generation || id !== userId()) {
      rememberRevocation(result); await flushSecureState(); await revokePending(); return;
    }
    secureSet('pushBinding', { bindingId: result.bindingId, userId: result.userId });
    await flushSecureState();
    pushState.registered = true; pushState.backendReady = result.backendReady === true; pushState.error = '';
  }).catch(() => { pushState.registered = false; pushState.error = 'connection'; });
}
function openPendingTap() {
  if (!pendingTap || !userId()) return;
  const route = notificationRoute(pendingTap, userId()); pendingTap = null;
  if (route) router.push(route).catch(() => {});
}
export async function enableNotifications(requestPermission = true) {
  if (!isNative || !userId() || pushState.busy) return;
  pushState.busy = true; pushState.error = '';
  try {
    let status = await PushNotifications.checkPermissions();
    if (requestPermission && status.receive === 'prompt') status = await PushNotifications.requestPermissions();
    pushState.permission = status.receive;
    if (status.receive === 'granted') await PushNotifications.register();
    else if (status.receive === 'denied') { rememberRevocation(secureGet('pushBinding')); secureSet('pushBinding', undefined); pushState.registered = false; await serial(revokePending); }
  } catch { pushState.error = 'connection'; }
  finally { pushState.busy = false; }
}
export const openNotificationSettings = () => NativeSupport.openSettings().catch(() => { pushState.error = 'settings'; });
export async function startNativeNotifications(appRouter, appStore) {
  router = appRouter; store = appStore;
  context = await NativeSupport.context();
  window.addEventListener('native:logout', logout);
  window.addEventListener('native:storage-error', () => { pushState.error = 'storage'; });
  await PushNotifications.addListener('registration', result => { deviceToken = result.value; void bindDevice(); });
  await PushNotifications.addListener('registrationError', () => { pushState.error = 'registration'; });
  await PushNotifications.addListener('pushNotificationReceived', notification => { if (notificationRoute(notification.data, userId())) refreshViews(); });
  await PushNotifications.addListener('pushNotificationActionPerformed', action => { pendingTap = action.notification.data; refreshViews(); openPendingTap(); });
  await App.addListener('appStateChange', ({ isActive }) => {
    if (!isActive) return;
    refreshViews(); serial(revokePending).catch(() => {}); void enableNotifications(false); openPendingTap();
  });
  await Keyboard.addListener('keyboardWillShow', () => document.documentElement.classList.add('keyboard-open'));
  await Keyboard.addListener('keyboardWillHide', () => document.documentElement.classList.remove('keyboard-open'));
  store.watch(() => store.getters.token && store.getters.userInfo?.user_id, (id, oldId) => {
    if (!id) return;
    if (oldId && String(oldId) !== String(id)) logout();
    void enableNotifications(true); openPendingTap();
  });
  router.afterEach(openPendingTap);
  // Keychain survives reinstall and crashes; clean up an old account before requesting a new binding.
  const old = secureGet('pushBinding');
  if (old && old.userId !== userId()) { rememberRevocation(old); secureSet('pushBinding', undefined); }
  serial(revokePending).catch(() => {});
  await router.isReady(); await enableNotifications(true); openPendingTap();
}
