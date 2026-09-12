import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/api-key-log/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getDetail = id => {
  return request({
    url: '/api/blade-system/api-key-log/detail',
    method: 'post',
    params: {
      id,
    },
  });
};
