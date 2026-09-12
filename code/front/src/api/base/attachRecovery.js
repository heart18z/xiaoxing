import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-resource/attach-recovery/page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getListByUser = (current, size, params) => {
  return request({
    url: '/api/blade-resource/attach-recovery/page-of-my',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getListBySourceIds = (current, size, params) => {
  return request({
    url: '/api/blade-resource/attach-recovery/page-of-source',
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
    url: '/api/blade-resource/attach-recovery/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const completeRemove = (ids) => {
  return request({
    url: '/api/blade-resource/attach-recovery/completeRemove',
    method: 'post',
    params: {
      ids,
    }
  })
}
export const remove = (ids) => {
  return request({
    url: '/api/blade-resource/attach-recovery/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const restore = (ids) => {
  return request({
    url: '/api/blade-resource/attach-recovery/restore',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-resource/attach-recovery/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-resource/attach-recovery/submit',
    method: 'post',
    data: row
  })
}

