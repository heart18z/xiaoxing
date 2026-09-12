import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/post/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getPostList = tenantId => {
  return request({
    url: '/api/blade-system/post/select',
    method: 'post',
    params: {
      tenantId,
    },
  });
};

export const getDetail = id => {
  return request({
    url: '/api/blade-system/post/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/post/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/post/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/post/submit',
    method: 'post',
    data: row,
  });
};
