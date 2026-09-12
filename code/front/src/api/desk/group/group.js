import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-group/group/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getLazyTreeList = (current, size, params) => {
  return request({
    url: '/api/blade-group/group/lazy-list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getDetail = (id) => {
  return request({
    url: '/api/blade-group/group/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const getTree = (tenantId) => {
  return request({
    url: '/api/blade-group/group/tree',
    method: 'post',
    params: {
      tenantId,
    }
  })
}


export const remove = (ids) => {
  return request({
    url: '/api/blade-group/group/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-group/group/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-group/group/submit',
    method: 'post',
    data: row
  })
}

export const getGroupUserList = (params) => {
  return request({
    url: '/api/blade-group/group/group-user-list',
    method: 'post',
    params: {
      ...params,
    }
  })
}

export const submitGroupUser = (row) => {
  return request({
    url: '/api/blade-group/group/group-user-submit',
    method: 'post',
    data: row
  })
}
