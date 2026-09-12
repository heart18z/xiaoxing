import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const dict = () => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/dict',
    method: 'post',

  })
}



export const getDetail = (id) => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/submit',
    method: 'post',
    data: row
  })
}

export const doSync = () => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/sync',
    method: 'post',
  })
}


export const update = (row) => {
  return request({
    url: '/api/blade-propertyRegion/propertyRegion/submit',
    method: 'post',
    data: row
  })
}

