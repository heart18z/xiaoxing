import { isNative } from './runtime';
import { hydrateSecureState } from './secureState';
async function start() {
  if (isNative) {
    await hydrateSecureState();
    document.documentElement.classList.add('native-app');
  }
  if (!location.pathname.startsWith('/app/')) history.replaceState(null, '', '/app/chat');
  await import('../main');
  if (isNative) {
    await import('./native.css');
    const [{ default: router }, { default: store }, { startNativeNotifications, pushState }] = await Promise.all([import('../router'), import('../store'), import('./notifications')]);
    await startNativeNotifications(router, store).catch(() => { pushState.error = 'initialization'; });
  }
}
start().catch(() => {
  const app = document.getElementById('app');
  app.replaceChildren();
  const message = document.createElement('p'); message.textContent = '应用初始化失败。请关闭后重试；若持续失败，请联系管理员。';
  app.append(message);
});
