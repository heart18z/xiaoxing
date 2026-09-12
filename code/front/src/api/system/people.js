import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-people/people/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const doSync = () => {
  return request({
    url: '/api/blade-people/people/sync',
    method: 'post',
  })
}

export const getDetail = (id) => {
  return request({
    url: '/api/blade-people/people/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-people/people/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-people/people/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-people/people/submit',
    method: 'post',
    data: row
  })
}

export const getPeoplePageList = (current, size, params) => {
  return request({
    url: '/api/blade-people/people/listPage',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getPeopleListByIds = ( params) => {
  return request({
    url: '/api/blade-people/people/peopleListByIds',
    method: 'post',
    params: {
      ...params,
    }
  })
}
