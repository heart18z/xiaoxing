import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-thirdPartyTable/thirdPartyTable/page',
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
    url: '/api/blade-thirdPartyTable/thirdPartyTable/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-thirdPartyTable/thirdPartyTable/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-thirdPartyTable/thirdPartyTable/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-thirdPartyTable/thirdPartyTable/submit',
    method: 'post',
    data: row
  })
}

