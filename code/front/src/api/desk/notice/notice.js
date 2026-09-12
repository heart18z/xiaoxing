import request from "@/axios";

export const addNotice = (data) =>
  request({
    url: '/api/blade-bizNotice/bizNotice/addNotice',
    method: 'post',
    data
  })

export const updateNotice = (data) =>
  request({
    url: '/api/blade-bizNotice/bizNotice/updateNotice',
    method: 'post',
    data
  })

export const getNoticeManagePage = (current, size, params) => {
  return request({
    url: '/api/blade-bizNotice/bizNotice/selectNoticeManage',
    method: 'post',
    params: {
      current,
      size,
    },
    data:params
  })
}
export const getMessageBySendUser = ( params) => {
  return request({
    url: '/api/blade-bizNotice/bizNotice/getMessageBySendUser',
    method: 'post',
    data:params
  })
}

// 获取消息数据
export const getUserListByGroupId = (groupId = '', userName = '') => {
  return request({
    url: '/api/blade-bizNotice/bizNotice/selectGroupUser',
    method: 'post',
    params: {
      groupId,
      userName
    }
  })
}


export const getGroupTree = () =>
  request({
    url: '/api/blade-bizNotice/bizNotice/groupTree',
    method: 'post',
  })

// 获取机构树
export const getDeptTree = () =>
    request({
        url: '/api/blade-bizNotice/bizNotice/deptTree',
        method: 'post',
    })
