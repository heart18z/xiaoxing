export const NEED_CONFIRM_LOGIN_ERROR = 'need_confirm_login';

export function parseOAuthResponseData(data) {
  if (!data) {
    return null;
  }
  if (typeof data === 'string') {
    try {
      return JSON.parse(data);
    } catch {
      return null;
    }
  }
  if (typeof data !== 'object') {
    return null;
  }
  if (
    data.data &&
    typeof data.data === 'object' &&
    !data.error &&
    !data.error_description &&
    !data.access_token
  ) {
    return data.data;
  }
  return data;
}

export function isNeedConfirmLogin(data) {
  const body = parseOAuthResponseData(data);
  if (!body) {
    return false;
  }
  if (body.error === NEED_CONFIRM_LOGIN_ERROR) {
    return true;
  }
  if (body.code === 601) {
    return true;
  }
  const desc = String(body.error_description || body.msg || body.message || '');
  return desc.includes('其他地址') && desc.includes('是否继续');
}

export function normalizeAxiosResponse(err) {
  if (!err) {
    return null;
  }
  if (err.data !== undefined && (err.status !== undefined || err.config)) {
    return err;
  }
  if (err.response) {
    return err.response;
  }
  return null;
}
