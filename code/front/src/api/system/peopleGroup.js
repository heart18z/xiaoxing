import request from '@/axios';

export const getPeopleGroupListPage = (current, size, params) => {
  return request({
    url: '/api/blade-system/peopleGroup/list-page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}


export const existPeopleGroupName = (data) => {
  return request({
    url: '/api/blade-system/peopleGroup/exist-people-group-name',
    method: 'post',
    data: data
  })
}

export const getPeopleGroupDetail = (id) => {
  return request({
    url: '/api/blade-system/peopleGroup/detail',
    method: 'post',
    params: {
      id
    }
  })
}

export const removePeopleGroup = (ids) => {
  return request({
    url: '/api/blade-system/peopleGroup/remove',
    method: 'post',
    params: {
      ids
    }
  })
}

export const submitPeopleGroup = (data) => {
  return request({
    url: '/api/blade-system/peopleGroup/submit',
    method: 'post',
    data: data
  })
}
