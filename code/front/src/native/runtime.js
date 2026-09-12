import { Capacitor } from '@capacitor/core';
export const isNative = Capacitor.isNativePlatform();
export const nativeApiOrigin = import.meta.env.VITE_APP_API || 'https://www.chentong.xyz';
export function apiUrl(path) {
  return isNative && path.startsWith('/api/') ? nativeApiOrigin.replace(/\/$/, '') + path : path;
}
// Keep bundled images local; only backend file URLs need the API origin.
export function nativeAssetUrls(value) {
  if (!isNative || !value || typeof value !== 'object') return value;
  for (const [key, child] of Object.entries(value)) {
    if (typeof child === 'string' && /^(avatar|aiAvatar|url|fileUrl|link|previewUrl)$/.test(key) && child.startsWith('/api/')) value[key] = apiUrl(child);
    else if (child && typeof child === 'object') nativeAssetUrls(child);
  }
  return value;
}
