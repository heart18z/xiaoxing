async (page) => {
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  await page.unroute('https://www.chentong.xyz/**');
  await page.addInitScript(() => {
    const listeners = {};
    let value = JSON.stringify({ token: 'offline', userInfo: { user_id: '1', user_name: '测试用户', role_name: 'app' } });
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
        if (method === 'checkPermissions') return { receive: 'granted' };
        return {};
      },
      nativeCallback: (plugin, method, options, cb) => { listeners[plugin + ':' + options.eventName] = cb; return 'mock'; }
    };
  });
  const prose = '这是一段用于检查消息宽度的测试文字，应当充分利用正常回复的行宽，不挤成狭窄的一列。'.repeat(3);
  const messages = [
    { id: '11', messageRole: 'assistant', messageType: 'TEXT', content: prose, isRead: true },
    { id: '12', messageRole: 'assistant', messageType: 'TEXT', content: '', reasoningContent: prose, isRead: true }
  ];
  await page.route('https://www.chentong.xyz/**', async route => {
    const path = route.request().url().split('www.chentong.xyz')[1].split('?')[0];
    let data = {};
    if (path.endsWith('/bootstrap')) data = { user: { id: '1', name: '测试用户' }, activeEventCount: 1 };
    else if (path.endsWith('/chat/messages')) data = messages;
    else if (path.endsWith('/chat/sync')) data = { revision: 'fixture', messages, unreadCount: 0 };
    else if (path.endsWith('/event/detail')) data = { creator: true, event: { id: '99', event_no: 'TEST99', event_status: 'ACTIVE', latest_summary: '陈通：吃药（时间：2026-09-13T15:00）。', event_time: '2026-09-13T15:00:00' }, branches: [], timeline: [] };
    else if (path.includes('/settings/')) data = { language: 'zh-cn', models: [], llmMode: 'SYSTEM', speechMode: 'SYSTEM' };
    else if (path.endsWith('/requests') || path.endsWith('/list')) data = [];
    await route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify({ code: 200, success: true, data }) });
  });
  const sizes = [];
  for (const width of [390, 320]) {
    await page.setViewportSize({ width, height: 844 });
    await page.goto('http://127.0.0.1:4188/app/chat');
    await page.locator('.thinking-toggle').waitFor({ timeout: 60000 });
    if (await page.locator('.thinking-toggle').getAttribute('aria-expanded') !== 'true') await page.locator('.thinking-toggle').click();
    await page.waitForFunction(() => document.querySelector('.thinking-toggle')?.getAttribute('aria-expanded') === 'true');
    await page.evaluate(async () => { await document.fonts.ready; await new Promise(requestAnimationFrame); });
    await page.waitForFunction(() => { const p = document.querySelector('.thinking-panel'); return p && p.scrollWidth <= p.clientWidth + 1; });
    const layout = await page.evaluate(() => {
      const a = document.querySelector('[data-message-id="11"] .bubble');
      const b = document.querySelector('[data-message-id="12"] .bubble');
      const p = document.querySelector('.thinking-panel');
      const input = document.querySelector('.composer-box textarea');
      return { normal: a.getBoundingClientRect().width, thinking: b.getBoundingClientRect().width, overflow: p.scrollWidth > p.clientWidth + 1, font: getComputedStyle(input).fontSize, background: getComputedStyle(document.body).backgroundColor };
    });
    if (Math.abs(layout.normal - layout.thinking) > 2 || layout.overflow || parseFloat(layout.font) < 16) throw new Error(JSON.stringify(layout));
    sizes.push({ width, ...layout });
    await page.screenshot({ path: `output/playwright/mobile-polish-thinking-${width}.png` });
  }
  await page.goto('http://127.0.0.1:4188/app/event/99');
  await page.locator('.hero h2').waitFor();
  const summary = await page.locator('.hero h2').innerText();
  if (summary.includes('13T15') || !summary.includes('2026-09-13 15:00')) throw new Error('ISO time not formatted');
  await page.screenshot({ path: 'output/playwright/mobile-polish-event.png' });
  if (errors.length) throw new Error(errors.join('; '));
  console.log(JSON.stringify({ sizes, summary, errors, nativeKeyboard: 'Requires real iPhone validation' }));
}
