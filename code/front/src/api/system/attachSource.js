import request from '@/axios';

export const getListBySource = (params) => {
  return request({
    url: '/api/blade-attach-source/attachSource/list-by-source',
    method: 'post',
    params: params
  })
}




export const removeFile = (data) => {
  return request({
    url: '/api/blade-attach-source/attachSource/remove',
    method: 'post',
    data: data
  })
}

export const submit = (row) => {
  return request({
    url: '/api/blade-attach-source/attachSource/submit',
    method: 'post',
    data: row
  })
}

export const setPermission = (row) => {
  return request({
    url: '/api/blade-attach-source/attachSource/user-account-permission',
    method: 'post',
    data: row
  })
}

export const attachPermissionLogList = (id) => {
  return request({
    url: '/api/blade-attachPermissionLog/attachPermissionLog/list',
    method: 'post',
    params:{
      attachSourceId: id
    }
  })
}

export const getRegionList = () => {
  return request({
    url: '/api/blade-attach-source/attachSource/region-list',
    method: 'post'
  })
}



export const postPdfSign = (data) => {
  return request({
    url: '/api/blade-attach-source/attachSource/post-sign',
    method: 'post',
    data: data
  })
}

export const getListByOriId = ( params) => {
  return request({
    url: '/api/blade-attach-source/attachSource/list-by-oriId',
    method: 'post',
    params: params
  })
}
