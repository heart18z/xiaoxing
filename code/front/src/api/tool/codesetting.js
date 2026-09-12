import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-develop/code-setting/list',
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
    url: '/api/blade-develop/code-setting/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-develop/code-setting/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-develop/code-setting/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-develop/code-setting/submit',
    method: 'post',
    data: row,
  });
};

export const enable = id => {
  return request({
    url: '/api/blade-develop/code-setting/enable',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getEnableDetail = () => {
  return request({
    url: '/api/blade-develop/code-setting/enable-detail',
    method: 'post',
    params: {},
  });
};

export const getTableForm = tableName => {
  return request({
    url: '/api/blade-develop/code-setting/table-form',
    method: 'post',
    params: {
      tableName,
    },
  });
};

export const getTablePrototype = (tableName, datasourceId) => {
  return request({
    url: '/api/blade-develop/code-setting/table-prototype',
    method: 'post',
    params: {
      tableName,
      datasourceId,
    },
  });
};
