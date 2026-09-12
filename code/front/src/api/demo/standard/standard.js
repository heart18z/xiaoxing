import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-standard/standard/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}


export const getLazyTreeList = (params) => {
  return request({
    url: '/api/blade-standard/standard/lazy-list',
    method: 'post',
    params: params
  })
}

export const getDetail = (id) => {
  return request({
    url: '/api/blade-standard/standard/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-standard/standard/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-standard/standard/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-standard/standard/submit',
    method: 'post',
    data: row
  })
}

