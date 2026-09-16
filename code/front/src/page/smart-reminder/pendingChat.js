import store from '@/store';
import {isNative} from '@/native/runtime';
import {secureGet,secureSet,flushSecureState} from '@/native/secureState';
import {submitChatJob,getChatJob,watchChatJob} from '@/api/smartReminder';
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
  let known=false,failures=0,streamSupported=false,streamDisabled=false;
  while(!signal?.aborted){
    if(user()!==request.userId)throw Object.assign(new Error('账号已切换'),{pending:true});
    if(document.hidden){await wait(800);continue;}
    let state;
    try{
      if(known&&streamSupported&&!streamDisabled){
        try{state=await watchChatJob(request.requestId,{signal,onReasoning:value=>{if(!signal?.aborted&&user()===request.userId){onWaiting?.('');onReasoning?.(value);}}});}
        catch(error){
          if(signal?.aborted||user()!==request.userId)throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
          if(document.hidden)continue;
          // Read-only fallback, not a second submission. No banner for a stream reconnect.
          streamDisabled=true;state=(await getChatJob(request.requestId)).data.data;
        }
      }else state=(await getChatJob(request.requestId)).data.data;
      if(signal?.aborted||user()!==request.userId)throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
      if(state?.status==='NOT_FOUND'&&!known){state=(await submitChatJob({content:request.content,fileIds:request.fileIds,requestId:request.requestId})).data.data;}
    }catch(error){
      if(error.pending)throw error;
      if(signal?.aborted||user()!==request.userId)throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
      const status=Number(error.status||error.response?.data?.code||error.response?.status);
      if(status===401||status===403)throw Object.assign(new Error('登录状态已变化，请重新登录后查询处理结果'),{pending:true});
      if(status>=400&&status<500&&![408,429].includes(status))throw Object.assign(new Error('暂时无法查询此消息，请核对事件后再操作'),{pending:true});
      failures++;
      const offline=typeof navigator!=='undefined'&&navigator.onLine===false;
      // One slow/failed poll does not mean the phone has lost connectivity.
      if(offline)onWaiting?.('当前设备离线，联网后将继续查询结果');
      else if(failures>=3)onWaiting?.(status>=500?'服务端查询暂时异常，正在重试，请勿重复发送':'结果查询暂时较慢，正在重试，请勿重复发送');
      if(Date.now()-request.createdAt>900000)throw Object.assign(new Error('处理结果待确认，请核对事件后再操作'),{pending:true});
      await wait(2000);continue;
    }
    if(signal?.aborted||user()!==request.userId)throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
    failures=0;onWaiting?.('');
    if(!state?.status)throw Object.assign(new Error('服务器尚未支持恢复处理，请更新服务器后重试'),{pending:true});
    if(known&&state.status==='NOT_FOUND')throw Object.assign(new Error('处理记录暂不可用，请先核对事件，不要重复发送'),{pending:true});
    known=known||state.status!=='NOT_FOUND';
    streamSupported=state.streamSupported===true;
    if(state.reasoning)onReasoning?.(state.reasoning);
    if(state.status==='SUCCEEDED'){clearPendingChat(request.requestId);return state.result;}
    if(state.status==='FAILED'||state.status==='CANCELLED'){clearPendingChat(request.requestId);throw Object.assign(new Error(state.message||'消息未处理成功'),{cancelled:state.status==='CANCELLED',definite:true});}
    if(state.status==='UNKNOWN')throw Object.assign(new Error('处理时间较长或服务器已重启，请先核对事件，不要重复发送'),{pending:true});
    // Normal queue/run states stay inside the assistant bubble, never a top banner.
    if(!streamSupported||streamDisabled)await wait(1200);
  }
  throw Object.assign(new Error('已暂停本机等待，服务器仍会继续处理'),{pending:true});
}
