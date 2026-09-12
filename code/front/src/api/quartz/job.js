import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-job/job/list',
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
    url: '/api/blade-job/job/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-job/job/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}


// 新增定时任务调度
export function addJob(data) {
  return request({
    url: '/api/blade-job/job/add',
    method: 'post',
    data: data
  })
}


export function run(data) {
  return request({
    url: '/api/blade-job/job/run',
    method: 'post',
    data: data
  })
}

export function queryCronExpression(ids) {
  return request({
    url: '/api/blade-job/job/queryCronExpression',
    method: 'post',
    params: {
      ids:ids
    }
  })
}
export function queryCronExpressionByInvokeTarget(invokeTarget) {
  return request({
    url: '/api/blade-job/job/queryCronExpressionByInvokeTarget',
    method: 'post',
    params: {
      invokeTarget:invokeTarget
    }
  })
}


// 修改定时任务调度
export function updateJob(data) {
  return request({
    url: '/api/blade-job/job/edit',
    method: 'post',
    data: data
  })
}


// 任务状态修改
export function changeJobStatus(jobId, status) {
  const data = {
    id:jobId,
    processStatus:status
  }
  return request({
    url: '/api/blade-job/job/changeStatus',
    method: 'post',
    data: data
  })
}


export const cleanData = (row) => {
  return request({
    url: '/api/blade-job/job/clean-data',
    method: 'post',
    data: row
  })
}
