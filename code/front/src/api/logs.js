import request from '@/axios';

export const getUsualList = (current, size) => {
  return request({
    url: '/api/blade-log/usual/list',
    method: 'post',
    params: {
      current,
      size,
    },
  });
};

export const getApiList = (current, size) => {
  return request({
    url: '/api/blade-log/api/list',
    method: 'post',
    params: {
      current,
      size,
    },
  });
};

export const getErrorList = (current, size) => {
  return request({
    url: '/api/blade-log/error/list',
    method: 'post',
    params: {
      current,
      size,
    },
  });
};

export const getUsualLogs = id => {
  return request({
    url: '/api/blade-log/usual/detail',
    method: 'post',
    params: {
      id,
    },
  });
};
export const getApiLogs = id => {
  return request({
    url: '/api/blade-log/api/detail',
    method: 'post',
    params: {
      id,
    },
  });
};
export const getErrorLogs = id => {
  return request({
    url: '/api/blade-log/error/detail',
    method: 'post',
    params: {
      id,
    },
  });
};
