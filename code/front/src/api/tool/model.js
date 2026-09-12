import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-develop/model/list',
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
    url: '/api/blade-develop/model/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-develop/model/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-develop/model/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-develop/model/submit',
    method: 'post',
    data: row,
  });
};

export const getTableList = datasourceId => {
  return request({
    url: '/api/blade-develop/model/table-list',
    method: 'post',
    params: {
      datasourceId,
    },
  });
};

export const getTableInfo = (modelId, datasourceId) => {
  return request({
    url: '/api/blade-develop/model/table-info',
    method: 'post',
    params: {
      modelId,
      datasourceId,
    },
  });
};

export const getTableInfoByName = (tableName, datasourceId) => {
  return request({
    url: '/api/blade-develop/model/table-info',
    method: 'post',
    params: {
      tableName,
      datasourceId,
    },
  });
};

export const getModelPrototype = (modelId, datasourceId) => {
  return request({
    url: '/api/blade-develop/model/model-prototype',
    method: 'post',
    params: {
      modelId,
      datasourceId,
    },
  });
};

export const submitModelPrototype = row => {
  return request({
    url: '/api/blade-develop/model-prototype/submit-list',
    method: 'post',
    data: row,
  });
};

export const prototypeDetail = modelId => {
  return request({
    url: '/api/blade-develop/model-prototype/select',
    method: 'post',
    params: {
      modelId,
    },
  });
};
