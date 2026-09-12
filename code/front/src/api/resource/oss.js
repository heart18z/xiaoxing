import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-resource/oss/list',
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
    url: '/api/blade-resource/oss/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-resource/oss/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-resource/oss/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-resource/oss/submit',
    method: 'post',
    data: row,
  });
};

export const enable = id => {
  return request({
    url: '/api/blade-resource/oss/enable',
    method: 'post',
    params: {
      id,
    },
  });
};

export const putFileAttach = row => {
  return request({
    url: '/api/blade-resource/oss/endpoint/put-file-attach',
    method: 'post',
    data: row,
  });
};

export const getCurrOss = () => {
  return request({
    url: '/api/blade-resource/oss/get-curr-oss',
    method: 'post',
  });
};

export const replaceFiles = row => {
  return request({
    url: '/api/blade-resource/oss/endpoint/replace-files',
    method: 'post',
    data: row,
  });
};
