import request from "@/axios";

export const getPage = (current, size, params) => {
  return request({
    url: '/api/config-database/config-database-data-clone/page',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getDatasource = (current, size, params) => {
  return request({
    url: '/api/blade-develop/datasource/select',
    method: 'post',
    params: {
      ...params,
      current,
      size,
    }
  })
}

export const getDatasourceByMenuTable = () => {
  return request({
    url: '/api/blade-develop/datasource/select-by-menu-table',
    method: 'post',

  })
}

export const submitSync = (data) => {
  return request({
    url: '/api/config-database/config-database-data-clone/submit-sync',
    method: 'post',
    data:data
  })
}

export const getTableListByMenuTable= (datasourceId) => {
  return request({
    url: '/api/blade-develop/datasource/table-list-by-menu-table',
    method: 'post',
    params: {
      datasourceId,
    }
  })
}

