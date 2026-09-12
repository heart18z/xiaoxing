import request from '@/axios';

import website from '@/config/website';
import func from '@/utils/func';
import store from '@/store';

export const loginByInvitationCode = (code, confirm = false) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    meta: {
      isLogin: true,
    },
    headers: {
      ...(confirm ? { confirm: 'true' } : {}),
    },
    params: {
      scope: 'all',
      username: code,
      grant_type: 'invitation',
    },
  });

export const loginByUsername = (
  tenantId,
  deptId,
  roleId,
  username,
  password,
  type,
  key,
  code,
  confirm = false
) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    meta: {
      isLogin: true,
    },
    headers: {
      'Tenant-Id': tenantId,
      'Dept-Id': func.toStr(deptId),
      'Role-Id': func.toStr(roleId),
      'Captcha-Key': key,
      'Captcha-Code': code,
      ...(confirm ? { confirm: 'true' } : {}),
    },
    params: {
      tenantId,
      username,
      password,
      grant_type: store.getters.systemParam?.chekcode === 'true' ? 'captcha' : 'password',
      scope: 'all',
      type,
    },
  });

export const loginBySocial = (tenantId, source, code, state) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    headers: {
      'Tenant-Id': tenantId,
    },
    params: {
      tenantId,
      source,
      code,
      state,
      grant_type: 'social',
      scope: 'all',
    },
  });

export const loginBySso = (state, code) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    headers: {
      'Tenant-Id': state,
    },
    params: {
      tenantId: state,
      code,
      grant_type: 'authorization_code',
      scope: 'all',
      redirect_uri: website.oauth2.redirectUri,
    },
  });

export const loginByKeycloak = (tenantId, code) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    headers: {
      'Tenant-Id': tenantId,
    },
    params: {
      tenantId,
      code,
      grant_type: 'keycloak',
      scope: 'all',
    },
  });

export const getKeycloakLoginUrl = state =>
  request({
    url: '/api/blade-auth/oauth/keycloak/login-url',
    method: 'post',
    authorization: false,
    params: {
      state,
    },
  });

export const getKeycloakEnabled = () =>
  request({
    url: '/api/blade-auth/oauth/keycloak/enabled',
    method: 'post',
    authorization: false,
  });

export const loginByCode = (userCode, code, confirm = false) =>
  request({
    url: '/api/unified-oauth/login',
    method: 'post',
    meta: {
      isLogin: true,
    },
    headers: {
      userCode,
      code,
      ...(confirm ? { confirm: 'true' } : {}),
    },
    params: {
      userCode,
      code,
    },
  });

export const loginByPhone = (tenantId, phone, id, value) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    headers: {
      'Tenant-Id': tenantId,
    },
    params: {
      tenantId,
      phone,
      id,
      value,
      grant_type: 'sms_code',
      scope: 'all',
    },
  });

export const refreshToken = (refresh_token, tenantId, deptId, roleId) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    // A rejected refresh must never wait on its own refresh promise.
    meta: { isRefreshToken: true },
    headers: {
      'Tenant-Id': tenantId,
      'Dept-Id': func.toStr(deptId),
      'Role-Id': func.toStr(roleId),
    },
    params: {
      tenantId,
      refresh_token,
      grant_type: 'refresh_token',
      scope: 'all',
    },
  });

export const registerUser = (tenantId, name, account, password, phone, email) =>
  request({
    url: '/api/blade-auth/oauth/token',
    method: 'post',
    headers: {
      'Tenant-Id': tenantId,
    },
    params: {
      name,
      username: account,
      account,
      password,
      phone,
      email,
      grant_type: 'register',
      scope: 'all',
    },
  });

export const registerGuest = (form, oauthId) =>
  request({
    url: '/api/blade-system/user/register-guest',
    method: 'post',
    params: {
      tenantId: form.tenantId,
      name: form.name,
      account: form.account,
      password: form.password,
      oauthId,
    },
  });

export const getButtons = () =>
  request({
    url: '/api/blade-system/menu/buttons',
    method: 'post',
  });

export const getCaptcha = () =>
  request({
    url: '/api/blade-auth/oauth/captcha',
    method: 'post',
    authorization: false,
  });

export const logout = () =>
  request({
    url: '/api/blade-auth/oauth/logout',
    method: 'post',
    authorization: false,
  });

export const getUserInfo = () =>
  request({
    url: '/api/blade-auth/oauth/user-info',
    method: 'post',
  });

export const sendLogs = list =>
  request({
    url: '/api/blade-auth/oauth/logout',
    method: 'post',
    data: list,
  });

export const clearCache = () =>
  request({
    url: '/api/blade-auth/oauth/clear-cache',
    method: 'post',
    authorization: false,
  });

export const verifySetPassword = () =>
  request({
    url: '/api/blade-auth/oauth/verify-set-password',
    method: 'post',
  });

export const sendSms = (tenantId, phone) =>
  request({
    url: '/api/blade-auth/oauth/sms/send-validate',
    method: 'post',
    params: {
      tenantId,
      phone,
    },
  });
