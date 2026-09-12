import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-develop/datasource/list',
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
    url: '/api/blade-develop/datasource/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-develop/datasource/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-develop/datasource/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-develop/datasource/submit',
    method: 'post',
    data: row,
  });
};
