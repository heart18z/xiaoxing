import {reactive} from 'vue';
import {Capacitor,registerPlugin} from '@capacitor/core';
import {App} from '@capacitor/app';
import store from '@/store';
import {silentReminder} from '@/api/smartReminder';
import {getToken} from '@/utils/auth';
import {alarmPlan} from './alarmPlan.mjs';
const bridge=registerPlugin('XiaoxingAlarm');
export const alarmState=reactive({supported:false,permission:'unknown',items:[],error:'',ready:false});
const owner=()=>getToken()?String(store.getters.userInfo?.user_id||''):'';
let started=false,revision=0,queue=Promise.resolve(),syncing=false;
const available=()=>Capacitor.getPlatform()==='ios'&&Capacitor.isPluginAvailable('XiaoxingAlarm');
const enqueue=fn=>{const job=queue.catch(()=>{}).then(fn);queue=job;return job;};
export async function refreshAlarms() {
  if(!available()){alarmState.ready=true;alarmState.supported=false;return;}
  const account=owner(),version=revision;
  await enqueue(async()=>{
    if(version!==revision)return;
    const result=await bridge.activate({owner:account});
    if(version!==revision)return;
    alarmState.supported=!!result.supported;
    if(account&&result.supported){
      const list=await bridge.list({owner:account});
      if(version!==revision)return;
      alarmState.items=list.alarms||[];alarmState.permission=list.permission;
    }else alarmState.items=[];
    alarmState.error='';alarmState.ready=true;
  });
}
export async function setEventAlarm(detail) {
  await refreshAlarms();
  if(!alarmState.supported)throw Error('系统闹铃需要 iOS 26 及以上，并安装支持闹铃的新版小醒；当前仅保留普通提醒');
  const account=owner(),version=revision,plan=alarmPlan(detail,account);
  const result=await bridge.schedule(plan);
  if(version!==revision||owner()!==account)throw Error('账号已变化，请检查闹铃状态');
  if(!result?.id)throw Error('系统未确认闹铃设置成功');
  await refreshAlarms();
  if(!alarmState.items.some(item=>item.eventId===plan.eventId&&item.active))throw Error('无法确认闹铃状态，请重新检查');
}
export async function cancelEventAlarm(eventId) {
  if(!available())return;
  await bridge.cancel({owner:owner(),eventId:String(eventId)});
  await refreshAlarms();
}
export async function reconcileAlarms() {
  if(syncing||document.hidden||!owner())return;
  syncing=true;const version=revision;
  try{
    await refreshAlarms();
    if(!alarmState.supported)return;
    for(const item of [...alarmState.items].filter(x=>x.active)){
      if(version!==revision)return;
      let detail;
      try{detail=(await silentReminder('event/detail',{eventId:item.eventId})).data.data;}
      catch{continue;} // Offline does not revoke an already scheduled local alarm.
      if(version!==revision)return;
      let plan;
      try{plan=alarmPlan(detail,owner());}
      catch{await cancelEventAlarm(item.eventId);continue;}
      if(plan.timestamp!==item.timestamp||plan.title!==item.title)await setEventAlarm(detail);
    }
  }catch(e){alarmState.error=e?.message||'同步本机闹铃失败，请在事件详情检查';}
  finally{syncing=false;}
}
export function startNativeAlarms() {
  if(started||!available())return;started=true;
  store.watch(owner,()=>{
    revision++;alarmState.items=[];alarmState.ready=false;
    refreshAlarms().then(reconcileAlarms).catch(e=>{alarmState.error=e?.message||'清理本机闹铃失败';});
  },{immediate:true});
  App.addListener('appStateChange',({isActive})=>{if(isActive)void reconcileAlarms();});
  window.addEventListener('smart-reminder:push-refresh',reconcileAlarms);
  window.addEventListener('smart-reminder:badge-refresh',reconcileAlarms);
  window.addEventListener('native:logout',()=>{
    revision++;alarmState.items=[];
    enqueue(()=>bridge.activate({owner:''})).catch(e=>{alarmState.error=e?.message||'退出时清理闹铃失败，请在事件详情检查';});
  });
}
