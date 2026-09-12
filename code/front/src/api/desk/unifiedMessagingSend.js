import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/list',
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
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/update',
    method: 'post',
    data: row
  })
}

export const changeStatus = (row) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/change-status',
    method: 'post',
    data: row
  })
}


export const submit = (row) => {
  return request({
    url: '/api/blade-unifiedMessagingSend/unifiedMessagingSend/submit',
    method: 'post',
    data: row
  })
}
