import { isNative } from '@/native/runtime';
import { secureGet, secureSet } from '@/native/secureState';
const key='xiaoxing:last-login-account';
export function lastLoginAccount() {
  try { return String((isNative ? secureGet('lastLoginAccount') : localStorage.getItem(key)) || '').slice(0,32); }
  catch { return ''; }
}
// Keep only the last successful account, never a password; survives normal logout.
export function rememberLoginAccount(account) {
  const value=String(account||'').trim().slice(0,32);
  try { if(isNative)secureSet('lastLoginAccount',value);else localStorage.setItem(key,value); }
  catch { /* Account remembering must not make a successful login fail. */ }
}
