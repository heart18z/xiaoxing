import request from '@/axios';

export const getAuthLockPage = (current, size, params) => {
  return request({
    url: '/api/blade-system/auth-lock/page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAuthLockDetail = id => {
  return request({
    url: '/api/blade-system/auth-lock/detail',
    method: 'post',
    params: { id },
  });
};

export const authLockUnlock = (id, unlockReason) => {
  return request({
    url: '/api/blade-system/auth-lock/unlock',
    method: 'post',
    params: {
      id,
      unlockReason,
    },
  });
};

export const lockUser = (userId, lockReason, lockBeginTime, lockEndTime) => {
  return request({
    url: '/api/blade-system/auth-lock/lock',
    method: 'post',
    params: {
      userId,
      lockReason,
      lockBeginTime,
      lockEndTime,
    },
  });
};
