import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/topmenu/list',
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
    url: '/api/blade-system/topmenu/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/topmenu/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/topmenu/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/topmenu/submit',
    method: 'post',
    data: row,
  });
};

export const grantTree = () => {
  return request({
    url: '/api/blade-system/menu/grant-top-tree',
    method: 'post',
  });
};

export const getTopTree = topMenuIds => {
  return request({
    url: '/api/blade-system/menu/top-tree-keys',
    method: 'post',
    params: {
      topMenuIds,
    },
  });
};

export const grant = (topMenuIds, menuIds) => {
  return request({
    url: '/api/blade-system/topmenu/grant',
    method: 'post',
    data: {
      topMenuIds,
      menuIds,
    },
  });
};

export const enable = id => {
  return request({
    url: '/api/blade-system/topmenu/enable',
    method: 'post',
    params: {
      id,
    },
  });
};
