import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-develop/code/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    },
  });
};

export const build = ids => {
  return request({
    url: '/api/blade-develop/code/gen-code',
    method: 'post',
    params: {
      ids,
      system: 'saber',
    },
  });
};

export const buildFast = form => {
  return request({
    url: '/api/blade-develop/code/gen-code-fast',
    method: 'post',
    data: form,
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-develop/code/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-develop/code/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-develop/code/submit',
    method: 'post',
    data: row,
  });
};

export const copy = id => {
  return request({
    url: '/api/blade-develop/code/copy',
    method: 'post',
    params: {
      id,
    },
  });
};

export const getCode = id => {
  return request({
    url: '/api/blade-develop/code/detail',
    method: 'post',
    params: {
      id,
    },
  });
};
