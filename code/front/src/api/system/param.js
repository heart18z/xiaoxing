import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-system/param/list',
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
    url: '/api/blade-system/param/detail',
    method: 'post',
    params: {
      id,
    },
  });
};

export const detail = param => {
  return request({
    url: '/api/blade-system/param/detail',
    method: 'post',
    params: param,
  });
};

export const remove = ids => {
  return request({
    url: '/api/blade-system/param/remove',
    method: 'post',
    params: {
      ids,
    },
  });
};

export const add = row => {
  return request({
    url: '/api/blade-system/param/submit',
    method: 'post',
    data: row,
  });
};

export const update = row => {
  return request({
    url: '/api/blade-system/param/submit',
    method: 'post',
    data: row,
  });
};

export const getSysTemParamMap = () => {
  return request({
    url: '/api/blade-system/param/param-map',
    method: 'post',
  });
};

export const getValueByKey = param => {
  return request({
    url: '/api/blade-system/param/get-value-by-key',
    method: 'post',
    params: { key: param },
  });
};

export const getLoginPageParam = () => {
  return request({
    url: '/api/blade-system/param/login-page-param',
    method: 'post',
  });
};
