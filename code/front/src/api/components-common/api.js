import request from '@/axios';

export const getMenuByPath = (params) => {
  return request({
    url: '/api/components/common/getMenuByPath',
    method: 'post',
    params: params
  })
}


export const singleDictByCode = (params) => {
  return request({
    url: '/api/baseData/base-dict/single-dict-by-code',
    method: 'post',
    params: params
  })
}

