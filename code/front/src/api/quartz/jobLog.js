import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-job/jobLog/list',
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
    url: '/api/blade-job/jobLog/detail',
    method: 'post',
    params: {
      jobLogId:id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-job/jobLog/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-job/jobLog/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-job/jobLog/submit',
    method: 'post',
    data: row
  })
}

