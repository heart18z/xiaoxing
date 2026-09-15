import { isNative } from './runtime';
import { hydrateSecureState } from './secureState';
async function start() {
  if (isNative) {
    await hydrateSecureState();
    document.documentElement.classList.add('native-app');
    await import('./native.css');
  }
  if (!location.pathname.startsWith('/app/')) history.replaceState(null, '', '/app/chat');
  await import('../main');
  if (isNative) {
    const [{ default: router }, { default: store }, { startNativeNotifications, pushState }] = await Promise.all([import('../router'), import('../store'), import('./notifications')]);
    const {startNativeAlarms}=await import('./alarms');
    startNativeAlarms();
    await startNativeNotifications(router, store).catch(() => { pushState.error = 'initialization'; });
  }
}
// Optional native integrations failing must not erase an already mounted page.
start().catch(() => window.xiaoxingStartup?.fail());
