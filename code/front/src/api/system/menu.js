import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/menu/list',
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
    url: '/api/blade-system/menu/lazy-list',
    method: 'post',
    params: {
      ...params,
      parentId,
    },
  });
};

export const getLazyMenuList = (parentId, params) => {
  return request({
    url: '/api/blade-system/menu/lazy-menu-list',
    method: 'post',
    params: {
      ...params,
      parentId,
    },
  });
};

export const getMenuList = (current, size, params) => {
  return request({
    url: '/api/blade-system/menu/menu-list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const getMenuTree = tenantId => {
  return request({
    url: '/api/blade-system/menu/tree',
    method: 'post',
    params: {
      tenantId,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/menu/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/menu/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/menu/submit',
    method: 'post',
    data: row,
  });
};

export const getMenu = id => {
  return request({
    url: '/api/blade-system/menu/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getTopMenu = () =>
  request({
    url: '/api/blade-system/menu/top-menu',
    method: 'post',
  });

export const getRoutes = topMenuId =>
  request({
    url: '/api/blade-system/menu/routes',
    method: 'post',
    params: {
      topMenuId,
    },
  });
