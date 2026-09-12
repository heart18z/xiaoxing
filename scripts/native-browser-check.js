async (page) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.setViewportSize({ width: 390, height: 844 });
  await page.addInitScript(() => {
    const listeners = {};
    let value = JSON.stringify({ token: 'offline-fixture', refreshToken: 'offline-refresh', userInfo: { user_id: '1', user_name: '测试用户', role_name: 'app' } });
    window.nativeFixture = { listeners, requests: [], getState: () => JSON.parse(value) };
    window.CapacitorCustomPlatform = { name: 'ios' };
    window.Capacitor = {
      PluginHeaders: [
        { name: 'XiaoxingNative', methods: ['readState', 'writeState', 'context', 'openSettings'] },
        { name: 'PushNotifications', methods: ['checkPermissions', 'requestPermissions', 'register', 'removeAllDeliveredNotifications', 'removeListener'] },
        { name: 'App', methods: ['removeListener'] }, { name: 'Keyboard', methods: ['removeListener'] }
      ].map(p => ({ ...p, methods: [...p.methods.map(name => ({ name, rtype: 'promise' })), { name: 'addListener', rtype: 'callback' }] })),
      nativePromise: async (plugin, method, options) => {
        if (method === 'readState') return { value };
        if (method === 'writeState') { value = options.value; return {}; }
        if (method === 'context') return { bundleId: 'com.dfyj.xiaoxing', environment: 'production', appVersion: '1.0' };
        if (method === 'checkPermissions' || method === 'requestPermissions') return { receive: 'granted' };
        if (method === 'register') setTimeout(() => listeners['PushNotifications:registration']?.({ value: 'b'.repeat(64) }), 10);
        return {};
      },
      nativeCallback: (plugin, method, options, callback) => { listeners[plugin + ':' + options.eventName] = callback; return 'mock-' + options.eventName; }
    };
  });
  await page.route('https://www.chentong.xyz/**', async route => {
    const path = route.request().url().split('www.chentong.xyz')[1].split('?')[0];
    let data = {};
    if (path.endsWith('/push/register')) data = { bindingId: '11111111-1111-1111-1111-111111111111', userId: '1', backendReady: false };
    else if (path.endsWith('/bootstrap')) data = { user: { id: '1', name: '测试用户' }, activeEventCount: 0 };
    else if (path.includes('/settings/')) data = { language: 'zh-cn', models: [], llmMode: 'SYSTEM', speechMode: 'SYSTEM' };
    else if (path.includes('/messages') || path.endsWith('/requests') || path.endsWith('/list')) data = [];
    else if (path.includes('/events/99')) data = { event: { id: '99', creatorUserId: '1', eventSummary: '通知陈颖下午3点开会', eventStatus: 'ACTIVE' }, branches: [], timeline: [] };
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, success: true, data }) });
  });
  await page.goto('http://127.0.0.1:4188/app/settings');
  await page.getByText('手机通知', { exact: true }).waitFor({ timeout: 60000 });
  await page.getByText('已获取通知权限，服务器 APNs 配置尚未就绪。', { exact: true }).waitFor({ timeout: 15000 });
  const state = await page.evaluate(() => ({ native: document.documentElement.classList.contains('native-app'), tokenInLocalStorage: localStorage.getItem('saber-token'), binding: window.nativeFixture.getState().pushBinding?.bindingId, scrolling: (() => { const el = document.querySelector('.sr-main'); return el && el.scrollHeight > el.clientHeight; })() }));
  if (!state.native || state.tokenInLocalStorage || !state.binding || !state.scrolling) throw new Error('Native setup/secure storage/scroll check failed: ' + JSON.stringify(state));
  await page.screenshot({ path: 'output/playwright/native-settings.png', fullPage: true });
  await page.evaluate(() => window.nativeFixture.listeners['PushNotifications:pushNotificationActionPerformed']({ notification: { data: { recipientUserId: '2', eventId: '99' } } }));
  if (!page.url().endsWith('/app/settings')) throw new Error('Cross-account notification navigated');
  await page.evaluate(() => window.nativeFixture.listeners['PushNotifications:pushNotificationActionPerformed']({ notification: { data: { recipientUserId: '1', eventId: '99' } } }));
  await page.waitForURL('**/app/event/99');
  await page.evaluate(() => window.dispatchEvent(new Event('native:logout')));
  await page.waitForFunction(() => !window.nativeFixture.getState().pushBinding);
  console.log(JSON.stringify({ state, errors, route: page.url(), note: 'Mock native bridge only, not an APNs delivery test' }));
  if (errors.length) throw new Error(errors.join('; '));
}
