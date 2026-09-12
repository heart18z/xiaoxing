import request from '@/axios';

export const getAuthLogPage = (current, size, params) => {
  return request({
    url: '/api/blade-system/auth-log/page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getAuthLogDetail = id => {
  return request({
    url: '/api/blade-system/auth-log/detail',
    method: 'post',
    params: { id },
  });
};
