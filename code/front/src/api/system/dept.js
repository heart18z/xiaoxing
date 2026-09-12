import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/dept/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getLazyList = (parentId, params) => {
  return request({
    url: '/api/blade-system/dept/lazy-list',
    method: 'post',
    params: {
      ...params,
      parentId,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/dept/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/dept/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/dept/submit',
    method: 'post',
    data: row,
  });
};

export const getDept = id => {
  return request({
    url: '/api/blade-system/dept/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getDeptTree = tenantId => {
  return request({
    url: '/api/blade-system/dept/tree',
    method: 'post',
    params: {
      tenantId,
    },
  });
};

export const getDeptLazyTree = parentId => {
  return request({
    url: '/api/blade-system/dept/lazy-tree',
    method: 'post',
    params: {
      parentId,
    },
  });
};
