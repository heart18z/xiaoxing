import request from '@/axios';

export const getList = (current, size, params) => {
    return request({
        url: '/api/blade-system/bizParam/list',
        method: 'post',
        params: {
            ...params,
            current,
            size,
        }
    })
}

export const getParentList = (current, size, params) => {
    return request({
        url: '/api/blade-system/bizParam/parent-list',
        method: 'post',
        params: {
            ...params,
            current,
            size,
        }
    })
}

export const getChildList = (current, size, parentId, params) => {
    return request({
        url: '/api/blade-system/bizParam/child-list',
        method: 'post',
        params: {
            ...params,
            current,
            size,
            parentId: parentId,
        }
    })
}

export const remove = (ids) => {
    return request({
        url: '/api/blade-system/bizParam/remove',
        method: 'post',
        params: {
            ids,
        }
    })
}

export const add = (row) => {
    return request({
        url: '/api/blade-system/bizParam/submit',
        method: 'post',
        data: row
    })
}

export const update = (row) => {
    return request({
        url: '/api/blade-system/bizParam/submit',
        method: 'post',
        data: row
    })
}


export const getBizParam = (id) => {
    return request({
        url: '/api/blade-system/bizParam/detail',
        method: 'post',
        params: {
            id,
        }
    })
}



export const getBizParamTree = () => {
    return request({
        url: '/api/blade-system/bizParam/tree?code=BizParam',
        method: 'post'
    })
}

export const getBizParamTreeByPath = path => {
    return request({
        url: `/api/blade-system/bizParam/tree-by-path`,
        method: 'post',
        params: {
            path
        }
    });
}


export const dictionary = (paramValue) => {
  return request({
    url: `/api/blade-system/bizParam/dictionary`,
    method: 'post',
    params: {
      paramValue: paramValue
    }
  });
}
