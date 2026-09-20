// The uni-app client authenticates with Blade-Auth and its own session storage.
// Cookies are shared across ports; legacy Saber cookies must not select its user.
const legacy = /^(saber-access-token|saber-refresh-token)=/i;
exports.requestHeaders = headers => {
  const result = { ...headers };
  const cookie = (result.cookie || '').split(';').map(v => v.trim()).filter(v => v && !legacy.test(v)).join('; ');
  if (cookie) result.cookie = cookie;
  else delete result.cookie;
  return result;
};
exports.responseHeaders = headers => {
  const result = { ...headers };
  const cookies = result['set-cookie'];
  if (cookies) {
    const kept = (Array.isArray(cookies) ? cookies : [cookies]).filter(v => !legacy.test(v.trim()));
    if (kept.length) result['set-cookie'] = kept;
    else delete result['set-cookie'];
  }
  return result;
};
