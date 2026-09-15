import store from '@/store';
import {isNative} from '@/native/runtime';
import {secureGet,secureSet,flushSecureState} from '@/native/secureState';
import {submitChatJob,getChatJob} from '@/api/smartReminder';
const key='xiaoxing:pending-chat';
const user=()=>String(store.getters.userInfo?.user_id||'');
export function pendingChat(){try{const saved=isNative?secureGet('pendingChat'):JSON.parse(localStorage.getItem(key)||'null');return saved?.userId===user()?saved:null;}catch{return null;}}
export async function rememberPendingChat(request){
  const saved={...request,userId:user(),createdAt:Date.now()};
  if(!saved.userId)throw new Error('请先登录');
  if(isNative){secureSet('pendingChat',saved);await flushSecureState();}else localStorage.setItem(key,JSON.stringify(saved));
  return saved;
}
export function clearPendingChat(requestId){if(pendingChat()?.requestId!==requestId)return;if(isNative)secureSet('pendingChat',undefined);else localStorage.removeItem(key);}
const wait=ms=>new Promise(resolve=>setTimeout(resolve,ms));
// Safe retry uses the same server-side idempotency key, never a new event mutation request.
export async function followChatJob(request,{signal,onReasoning,onWaiting}={}){
  let known=false;
  while(!signal?.aborted){
    if(user()!==request.userId)throw Object.assign(new Error('账号已切换'),{pending:true});
    if(document.hidden){await wait(800);continue;}
    let state;
    try{
      state=(await getChatJob(request.requestId)).data.data;
      if(state?.status==='NOT_FOUND'&&!known){state=(await submitChatJob({content:request.content,fileIds:request.fileIds,requestId:request.requestId})).data.data;}
    }catch{
      onWaiting?.('连接暂时中断，恢复后将继续查询结果');
      if(Date.now()-request.createdAt>900000)throw Object.assign(new Error('处理结果待确认，请核对事件后再操作'),{pending:true});
      await wait(2000);continue;
    }
    if(signal?.aborted||user()!==request.userId)throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
    if(!state?.status)throw Object.assign(new Error('服务器尚未支持恢复处理，请更新服务器后重试'),{pending:true});
    if(known&&state.status==='NOT_FOUND')throw Object.assign(new Error('处理记录暂不可用，请先核对事件，不要重复发送'),{pending:true});
    known=known||state.status!=='NOT_FOUND';
    if(state.reasoning)onReasoning?.(state.reasoning);
    if(state.status==='SUCCEEDED'){clearPendingChat(request.requestId);return state.result;}
    if(state.status==='FAILED'||state.status==='CANCELLED'){clearPendingChat(request.requestId);throw Object.assign(new Error(state.message||'消息未处理成功'),{cancelled:state.status==='CANCELLED',definite:true});}
    if(state.status==='UNKNOWN')throw Object.assign(new Error('处理时间较长或服务器已重启，请先核对事件，不要重复发送'),{pending:true});
    onWaiting?.(state.status==='QUEUED'?'消息已接收，正在排队处理':'正在处理，息屏后也会继续');
    await wait(1200);
  }
  throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
}
