import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/role/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};
export const grantTree = () => {
  return request({
    url: '/api/blade-system/menu/grant-tree',
    method: 'post',
  });
};

export const grant = (roleIds, menuIds, dataScopeIds, apiScopeIds) => {
  return request({
    url: '/api/blade-system/role/grant',
    method: 'post',
    data: {
      roleIds,
      menuIds,
      dataScopeIds,
      apiScopeIds,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/role/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/role/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/role/submit',
    method: 'post',
    data: row,
  });
};

export const getRole = roleIds => {
  return request({
    url: '/api/blade-system/menu/role-tree-keys',
    method: 'post',
    params: {
      roleIds,
    },
  });
};

export const getRoleTree = tenantId => {
  return request({
    url: '/api/blade-system/role/tree',
    method: 'post',
    params: {
      tenantId,
    },
  });
};

export const getRoleTreeById = roleId => {
  return request({
    url: '/api/blade-system/role/tree-by-id',
    method: 'post',
    params: {
      roleId,
    },
  });
};

export const getRoleAlias = () => {
  return request({
    url: '/api/blade-system/role/alias',
    method: 'post',
    params: {},
  });
};
