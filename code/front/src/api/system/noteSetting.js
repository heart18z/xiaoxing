import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-noteSetting/noteSetting/list',
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
    url: '/api/blade-noteSetting/noteSetting/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const selectDomNote = (menuId,type) => {
  return request({
    url: '/api/blade-noteSetting/noteSetting/select-dom-note',
    method: 'post',
    params: {
      menuId:menuId,
      type:type
    }
  })
}



export const remove = (ids) => {
  return request({
    url: '/api/blade-noteSetting/noteSetting/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-noteSetting/noteSetting/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-noteSetting/noteSetting/submit',
    method: 'post',
    data: row
  })
}

