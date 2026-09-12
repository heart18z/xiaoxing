import request from '@/axios';

export const getList = (current, size, params, deptId) => {
  return request({
    url: '/api/blade-system/user/page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
      deptId,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/user/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/user/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/user/update',
    method: 'post',
    data: row,
  });
};

export const updatePlatform = (userId, userType, userExt) => {
  return request({
    url: '/api/blade-system/user/update-platform',
    method: 'post',
    params: {
      userId,
      userType,
      userExt,
    },
  });
};

export const getUser = id => {
  return request({
    url: '/api/blade-system/user/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getUserPlatform = id => {
  return request({
    url: '/api/blade-system/user/platform-detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getUserInfo = () => {
  return request({
    url: '/api/blade-system/user/info',
    method: 'post',
  });
};

export const resetPassword = userIds => {
  return request({
    url: '/api/blade-system/user/reset-password',
    method: 'post',
    params: {
      userIds,
    },
  });
};

export const resetPasswordShow = userId => {
  return request({
    url: '/api/blade-system/user/reset-password-show',
    method: 'post',
    params: {
      userId,
    },
  });
};

export const unlock = userIds => {
  return request({
    url: '/api/blade-system/user/unlock',
    method: 'post',
    params: {
      userIds,
    },
  });
};

export const lock = userIds => {
  return request({
    url: '/api/blade-system/user/lock',
    method: 'post',
    params: {
      userIds,
    },
  });
};

export const updatePassword = (oldPassword, newPassword, newPassword1) => {
  return request({
    url: '/api/blade-system/user/update-password',
    method: 'post',
    params: {
      oldPassword,
      newPassword,
      newPassword1,
    },
  });
};

export const updateInfo = row => {
  return request({
    url: '/api/blade-system/user/update-info',
    method: 'post',
    data: row,
  });
};

export const grant = (userIds, roleIds) => {
  return request({
    url: '/api/blade-system/user/grant',
    method: 'post',
    params: {
      userIds,
      roleIds,
    },
  });
};

export const auditPass = userIds => {
  return request({
    url: '/api/blade-system/user/audit-pass',
    method: 'post',
    params: {
      userIds,
    },
  });
};

export const auditRefuse = userIds => {
  return request({
    url: '/api/blade-system/user/audit-refuse',
    method: 'post',
    params: {
      userIds,
    },
  });
};

export const setLeader = userId => {
  return request({
    url: '/api/blade-system/user/set-leader',
    method: 'post',
    params: {
      userId,
    },
  });
};

export const getLeaderList = (tenantId, realName) => {
  return request({
    url: '/api/blade-system/user/leader-list',
    method: 'post',
    params: {
      tenantId,
      realName,
    },
  });
};

export const getUserPage = (current, size, account, name) => {
  return request({
    url: '/api/blade-system/user/user-page',
    method: 'post',
    params: {
      current,
      size,
      account,
      name,
    },
  });
};

export const getUserContactList = param => {
  return request({
    url: '/api/blade-system/user/user-contact',
    method: 'post',
    params: param,
  });
};

export const getUserContactDict = param => {
  return request({
    url: '/api/blade-system/user/user-contact-dict',
    method: 'post',
    params: param,
  });
};

export const userAccountKvList = () => {
  return request({
    url: '/api/blade-system/user/user-account-kv-list',
    method: 'post',
  });
};

export const loginUrl = () => {
  return request({
    url: '/api/unified-oauth/get-login-url',
    method: 'post',
  });
};

