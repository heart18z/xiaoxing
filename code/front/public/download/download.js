const $ = id => document.getElementById(id);
const dialog = $('download-dialog');
let config = null;
let toastTimer;
function closeDialog() {
  if (typeof dialog.close === 'function') dialog.close();
  else { dialog.removeAttribute('open'); document.body.classList.remove('fallback-modal'); }
}
const sections = ['qr', 'harmony', 'version', 'help', 'message'];
function show(type, title) {
  sections.forEach(name => $(name + '-content').hidden = name !== type);
  $('dialog-title').textContent = title;
  if (!dialog.open) {
    if (typeof dialog.showModal === 'function') dialog.showModal();
    else { dialog.setAttribute('open', ''); document.body.classList.add('fallback-modal'); }
  }
}
function message(text) {
  $('dialog-message').textContent = text;
  show('message', '下载提示');
}
function toast(text) {
  clearTimeout(toastTimer);
  $('toast').textContent = text;
  $('toast').hidden = false;
  toastTimer = setTimeout(() => $('toast').hidden = true, 2500);
}
function officialLink(value, hosts) {
  try {
    const url = new URL(value);
    return url.protocol === 'https:' && !url.username && !url.password && (!url.port || url.port === '443') && hosts.includes(url.hostname) ? url.href : '';
  } catch { return ''; }
}
function download(platform) {
  if (!config) { message('暂时无法获取下载信息，请检查网络后点击重新加载。'); return; }
  if (platform === 'harmony') { show('harmony', '选择鸿蒙版本'); return; }
  const s = config.settings;
  if (platform === 'android' || platform === 'harmony-apk') {
    const enabled = platform === 'android' ? s.androidEnabled : s.harmonyApkEnabled;
    if (!enabled) { message('该平台版本正在适配中，敬请期待。'); return; }
    if (!config.androidAvailable) { message('安卓版本暂未发布，请稍后再试。'); return; }
    if (/MicroMessenger/i.test(navigator.userAgent)) {
      message('请点击右上角菜单，选择「在浏览器中打开」，再下载安装包。'); return;
    }
    closeDialog();
    // Fixed same-origin endpoint always serves the latest published APK. Never trust a redirect from config.
    window.location.assign('/api/app/download/android.apk');
    return;
  }
  const ios = platform === 'ios';
  if (!(ios ? s.iosEnabled : s.harmonyNativeEnabled)) {
    message(ios ? 'iOS 版本正在适配中，敬请期待。' : 'HarmonyOS 5 及以上版本正在适配中，敬请期待。'); return;
  }
  const url = ios ? (s.iosMode === 'appstore'
    ? officialLink(s.appStoreUrl, ['apps.apple.com'])
    : officialLink(s.testflightUrl, ['testflight.apple.com']))
    : officialLink(s.harmonyStoreUrl, ['appgallery.huawei.com', 'appgallery.cloud.huawei.com']);
  if (!url) { message('下载链接暂不可用，请稍后再试。'); return; }
  window.location.assign(url);
}
async function load() {
  config = null;
  $('load-error').hidden = true;
  $('version-label').textContent = '正在获取版本信息…';
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12000);
  try {
    const response = await fetch('/api/app/download/config', { cache: 'no-store', credentials: 'omit', signal: controller.signal });
    if (!response.ok) throw new Error();
    const body = await response.json();
    if (body.code !== 200 || !body.data?.settings || !body.data.android) throw new Error();
    config = body.data;
    const version = config.android;
    $('version-label').textContent = version.versionName ? '安卓版本：' + version.versionName : '更多平台，持续适配中';
    $('version-number').textContent = version.versionName ? version.versionName + '（' + version.versionCode + '）' : '暂未发布';
    $('version-size').textContent = version.fileSize ? (version.fileSize / 1048576).toFixed(1) + ' MB' : '—';
    $('version-notes').textContent = version.notes || '暂无更新说明';
    const pageUrl = config.settings.pageUrl;
    const safePage = officialLink(pageUrl, [new URL(pageUrl).hostname]);
    $('page-link').textContent = safePage;
    if (safePage) $('page-link').href = safePage;
    $('qr-image').src = '/api/app/download/qr?v=' + Date.now();
  } catch {
    config = null;
    $('load-error').hidden = false;
    $('version-label').textContent = '下载信息暂不可用';
  } finally { clearTimeout(timeout); }
}
document.querySelectorAll('[data-platform]').forEach(button => button.addEventListener('click', () => download(button.dataset.platform)));
document.querySelectorAll('[data-dialog]').forEach(button => button.addEventListener('click', () => {
  const type = button.dataset.dialog;
  if ((type === 'qr' || type === 'version') && !config) { message('请先检查网络并重新加载下载信息。'); return; }
  show(type, { qr: '扫码下载 AI小醒', version: '版本信息', help: '安装帮助' }[type]);
}));
$('dialog-close').addEventListener('click', closeDialog);
$('message-ok').addEventListener('click', closeDialog);
dialog.addEventListener('click', event => {
  const r = dialog.getBoundingClientRect();
  if (event.target === dialog && (event.clientX < r.left || event.clientX > r.right || event.clientY < r.top || event.clientY > r.bottom)) closeDialog();
});
$('copy-link').addEventListener('click', async () => {
  try { await navigator.clipboard.writeText(config.settings.pageUrl); toast('下载链接已复制'); }
  catch { toast('请长按上方网址复制链接'); }
});
$('qr-image').addEventListener('error', () => toast('二维码加载失败，请重新加载页面'));
$('retry').addEventListener('click', load);
load();
