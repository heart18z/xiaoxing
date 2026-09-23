import request from '@/axios';
import { getToken } from '@/utils/auth';
import website from '@/config/website';
import { Base64 } from 'js-base64';
import { apiUrl, isNative } from '@/native/runtime';
import store from '@/store';
import { consumeChatStream, requestChatStream } from './chatStream.mjs';

// 该项目的本地 Vite 代理在 Windows 上会破坏 JSON 中的多字节字符，
// 将非 ASCII 字符转为标准 JSON unicode escape，服务端解析后仍是原始中文。
const asciiJson = data =>
  JSON.stringify(data).replace(/[\u007f-\uffff]/g, char =>
    `\\u${char.charCodeAt(0).toString(16).padStart(4, '0')}`
  );
const jsonOptions = data =>
  data == null
    ? {}
    : { headers: { 'Content-Type': 'application/json;charset=UTF-8' } };
const post = (url, data, params) =>
  request({ url, method: 'post', data, params, meta: { noProgress: true }, ...jsonOptions(data) });

const accountPost = (action, data) => request({ url:'/api/app/account/'+action, method:'post', data, meta:{noProgress:true,silent:true,isToken:action!=='register'} });
export const registerAccount = data => accountPost('register', data);
export const suggestAccount = () => request({url:'/api/app/account/suggest-account',method:'post',meta:{noProgress:true,silent:true,isToken:false}});
export const changeOwnPassword = data => accountPost('password', data);
export const silentReminder = (action, params) => request({url:'/api/app/reminder/'+action,method:'post',params,meta:{noProgress:true,silent:true}});

let bootstrapRequest;
// Share simultaneous shell/page requests, but do not cache user-specific results.
export const bootstrap = () => bootstrapRequest || (bootstrapRequest = post('/api/app/reminder/bootstrap').finally(() => { bootstrapRequest = null; }));
export const updateProfile = data => post('/api/app/reminder/profile/update', data);
export const saveFriendRemark = data => post('/api/app/reminder/friends/remark', data);
export const uploadAvatar = file => {
  const data = new FormData();
  data.append('file', file);
  return request({
    url: '/api/blade-resource/oss/endpoint/put-file',
    method: 'post',
    data,
    timeout: 120000,
    meta: { noProgress: true },
  });
};
export const getMessages = limit => request({url:'/api/app/reminder/chat/messages',method:'post',params:{limit},timeout:15000,meta:{noProgress:true,silent:true}});
export const syncMessages = revision => request({url:'/api/app/reminder/chat/sync',method:'post',params:{limit:150,revision},timeout:15000,meta:{noProgress:true,silent:true}});
export const sendMessage = data =>
  request({ url: '/api/app/reminder/chat/send', method: 'post', data, timeout: 300000, meta: { noProgress: true }, ...jsonOptions(data) });
export const streamMessage = async (data, handlers = {}) => {
  const headers = {
    Authorization: `Basic ${Base64.encode(`${website.clientId}:${website.clientSecret}`)}`,
    'Content-Type': 'application/json;charset=UTF-8',
    Accept: 'text/event-stream',
    'Blade-Requested-With': 'BladeHttpRequest',
  };
  const response = await requestChatStream(() => {
    const token = getToken();
    if(token) headers[website.tokenHeader]=`bearer ${token}`;
    else delete headers[website.tokenHeader];
    return fetch(apiUrl('/api/app/reminder/chat/stream'), {
    method: 'POST', headers, body: asciiJson(data), credentials: isNative ? 'omit' : 'include', signal:handlers.signal,
    });
  }, () => store.dispatch('RefreshToken'), handlers.signal);
  return consumeChatStream(response, handlers);
};
export const readMessages = messageIds => post('/api/app/reminder/chat/read', { messageIds });
export const clearChatContext = () => post('/api/app/reminder/chat/context/clear');
export const stopMessage = requestId => post('/api/app/reminder/chat/stop',{requestId});
export const confirmCandidate = (candidateId,acceptConflicts=false) => request({url:'/api/app/reminder/candidate/confirm',method:'post',data:{candidateId,acceptConflicts},timeout:300000,meta:{noProgress:true}});
export const uploadFile = file => {
  const data = new FormData();
  data.append('file', file);
  return request({ url: '/api/app/reminder/files/upload', method: 'post', data, timeout:300000, meta: { noProgress: true } });
};
export const searchUsers = keyword => post('/api/app/reminder/friends/search', null, { keyword });
export const getFriends = () => post('/api/app/reminder/friends/list');
export const getFriendRequests = () => post('/api/app/reminder/friends/requests');
export const requestFriend = data => post('/api/app/reminder/friends/request', data);
export const replyFriend = data => post('/api/app/reminder/friends/reply', data);
export const removeFriend = targetUserId => post('/api/app/reminder/friends/remove', {targetUserId});
export const getEvents = (type, status = '') => post('/api/app/reminder/events', null, { type, status });
export const getEventCounts = type => post('/api/app/reminder/events/counts', null, { type });
export const getEventDetail = eventId => post('/api/app/reminder/event/detail', null, { eventId });
export const getReminderDrawer = type => request({url:'/api/app/reminder/events/drawer',method:'post',params:{type},meta:{noProgress:true,silent:true}});
export const submitChatJob = data => request({url:'/api/app/reminder/chat/jobs/submit',method:'post',data,timeout:15000,meta:{noProgress:true,silent:true}});
export const getChatJob = requestId => request({url:'/api/app/reminder/chat/jobs/status',method:'post',data:{requestId},meta:{noProgress:true,silent:true}});
// Observe accepted work only; closing this connection never cancels the server job.
export const watchChatJob = async (requestId,{signal,onReasoning}={}) => {
  const controller=new AbortController(),abort=()=>controller.abort(),hidden=()=>{if(document.hidden)abort();};
  const timer=setTimeout(abort,35000);
  signal?.addEventListener('abort',abort,{once:true});document.addEventListener('visibilitychange',hidden);
  if(signal?.aborted||document.hidden)abort();
  try{
    const response=await requestChatStream(()=>{
      const headers={Authorization:`Basic ${Base64.encode(`${website.clientId}:${website.clientSecret}`)}`,'Content-Type':'application/json;charset=UTF-8',Accept:'text/event-stream','Blade-Requested-With':'BladeHttpRequest'};
      const token=getToken();if(token)headers[website.tokenHeader]=`bearer ${token}`;
      return fetch(apiUrl('/api/app/reminder/chat/jobs/watch'),{method:'POST',headers,body:asciiJson({requestId}),credentials:isNative?'omit':'include',signal:controller.signal});
    },()=>store.dispatch('RefreshToken'),controller.signal);
    return await consumeChatStream(response,{onSnapshot:event=>onReasoning?.(event.reasoning||'')});
  }finally{clearTimeout(timer);signal?.removeEventListener('abort',abort);document.removeEventListener('visibilitychange',hidden);}
};
export const getEventConversation = (eventId, participantUserId) => request({url:'/api/app/reminder/event/conversation',method:'post',params:{eventId,participantUserId},timeout:30000,meta:{noProgress:true}});
export const stopEvent = data => post('/api/app/reminder/event/stop', data);

export const getAiConfig = (configType='LLM',id) => post('/api/blade-smart/ai-config/detail',null,{configType,id});
export const getAiConfigs = () => post('/api/blade-smart/ai-config/list');
export const setDefaultAiConfig = (configType,id) => post('/api/blade-smart/ai-config/default',null,{configType,id});
export const activateAiConfig = (configType,id) => post('/api/blade-smart/ai-config/activate',null,{configType,id});
export const getAiPrompts = () => post('/api/blade-smart/ai-config/prompts');
export const saveAiPrompts = data => post('/api/blade-smart/ai-config/prompts/save',data);
export const getModelSettings = () => post('/api/app/reminder/settings/models');
export const saveModelSettings = data => post('/api/app/reminder/settings/models/save',data);
export const transcribeAudio = (file,signal) => {const data=new FormData();data.append('file',file);return request({url:'/api/app/reminder/audio/transcriptions',method:'post',data,signal,timeout:125000,meta:{noProgress:true,silent:true}});};
export const saveAiConfig = data => post('/api/blade-smart/ai-config/submit', data);
export const testAiConfig = id => request({ url: '/api/blade-smart/ai-config/test', method: 'post', params:{id},timeout: 130000 });

export const getAdminDashboard = () => post('/api/blade-smart/admin/dashboard');
export const getAdminEvents = params => post('/api/blade-smart/admin/events', null, params);
export const getAdminNotifications = params => post('/api/blade-smart/admin/notifications', null, params);
export const getAdminEvaluations = params => post('/api/blade-smart/admin/evaluations', null, params);

export const getFilePreview = id => post('/api/app/reminder/files/preview', null, { id });
