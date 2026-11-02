import request from '@/axios';
export const reportRequest=(action,params={},data=null)=>request({url:'/api/blade-smart/admin/reports/'+action,method:'post',params,data,timeout:300000});
export const downloadReport=async(id,format)=>{
  const response=await request({url:'/api/blade-smart/admin/reports/download',method:'get',params:{id,format},responseType:'blob'});
  if(response.data.type?.includes('json'))throw new Error('报告下载失败，请刷新后重试');
  const url=URL.createObjectURL(response.data),link=document.createElement('a');link.href=url;link.download=`小醒事件报告.${format}`;link.click();setTimeout(()=>URL.revokeObjectURL(url),10000);
};
