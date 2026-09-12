import request from '@/axios';

// =====================参数===========================

export const historyFlowList = processInstanceId => {
  return request({
    url: '/api/blade-flow/process/history-flow-list',
    method: 'post',
    params: {
      processInstanceId,
    },
  });
};

// =====================请假流程===========================

export const leaveProcess = data => {
  return request({
    url: '/api/blade-desk/process/leave/start-process',
    method: 'post',
    data,
  });
};

export const leaveDetail = businessId => {
  return request({
    url: '/api/blade-desk/process/leave/detail',
    method: 'post',
    params: {
      businessId,
    },
  });
};
