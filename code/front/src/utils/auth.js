import Cookies from 'js-cookie';
import website from '@/config/website';
import { isNative } from '@/native/runtime';
import { secureGet, secureSet } from '@/native/secureState';

const TokenKey = website.tokenKey;
const RefreshTokenKey = website.refreshTokenKey;
const SessionId = 'JSESSIONID';
const UserId = 'b-user-id';

export function getToken() {
  if (isNative) return secureGet('token');
  return Cookies.get(TokenKey);
}

export function setToken(token) {
  if (isNative) { if (!token) window.dispatchEvent(new Event('native:logout')); return secureSet('token', token); }
  return Cookies.set(TokenKey, token);
}

export function getRefreshToken() {
  if (isNative) return secureGet('refreshToken');
  return Cookies.get(RefreshTokenKey);
}

export function setRefreshToken(token) {
  if (isNative) return secureSet('refreshToken', token);
  return Cookies.set(RefreshTokenKey, token);
}

export function removeToken() {
  if (isNative) { window.dispatchEvent(new Event('native:logout')); secureSet('userInfo', undefined); return secureSet('token', undefined); }
  Cookies.remove(SessionId);
  Cookies.remove(UserId);
  return Cookies.remove(TokenKey);
}

export function removeRefreshToken() {
  if (isNative) return secureSet('refreshToken', undefined);
  return Cookies.remove(RefreshTokenKey);
}
