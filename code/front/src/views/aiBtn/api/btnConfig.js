import request from '@/axios';

export const getList = (current, size, params) => {
  return request({
    url: '/api/blade-ai/btnConfig/list',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getDetail = (params) => {
  return request({
    url: '/api/blade-ai/btnConfig/detail',
    method: 'post',
    params: {
      ...params,
    }
  })
}

export const remove = (ids) => {
  return request({
    url: '/api/blade-ai/btnConfig/remove',
    method: 'post',
    params: {
      ids,
    }
  })
}

export const add = (row) => {
  return request({
    url: '/api/blade-ai/btnConfig/submit',
    method: 'post',
    data: row
  })
}

export const update = (row) => {
  return request({
    url: '/api/blade-ai/btnConfig/submit',
    method: 'post',
    data: row
  })
}

/** AI 测试对话：经本系统后端转发；data 中需含 aiInterfaceUrl（与按钮配置一致），转发时会由后端去掉该字段 */
export const aiTestProxy = (data) => {
  return request({
    url: '/api/blade-ai/btnConfig/ai-test-proxy',
    method: 'post',
    data
  })
}

