/* Runs before the application bundle, including when that bundle cannot load. */
(function () {
  var panel = document.getElementById('xiaoxing-startup');
  if (!document.documentElement.hasAttribute('data-xiaoxing-boot')) { panel.remove(); return; }
  var message = document.getElementById('xiaoxing-startup-message');
  var retry = document.getElementById('xiaoxing-startup-retry');
  var finished = false;
  retry.onclick = function () { location.reload(); };
  var slow = setTimeout(function () {
    if (finished) return;
    message.textContent = '加载比平时稍久，请稍候或重新加载';
    retry.hidden = false;
  }, 15000);
  function fail() {
    if (finished) return;
    clearTimeout(slow);
    panel.setAttribute('data-failed', '');
    message.textContent = '暂时无法打开，请检查网络后重试';
    retry.hidden = false;
  }
  function resourceError(event) {
    if (event.target && event.target.tagName === 'SCRIPT' && event.target.type === 'module') fail();
  }
  window.addEventListener('error', resourceError, true);
  window.addEventListener('vite:preloadError', fail);
  window.xiaoxingStartup = {
    fail: fail,
    ready: function () {
      if (finished) return;
      finished = true;
      clearTimeout(slow);
      window.removeEventListener('error', resourceError, true);
      window.removeEventListener('vite:preloadError', fail);
      panel.style.opacity = '0';
      panel.style.pointerEvents = 'none';
      setTimeout(function () { panel.remove(); document.documentElement.removeAttribute('data-xiaoxing-boot'); }, 260);
    }
  };
})();
