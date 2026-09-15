<template>
  <el-drawer v-model="opened" direction="ltr" size="min(92vw,440px)" :with-header="false" append-to-body class="reminder-drawer" @open="refresh" @closed="rows=[]">
    <header class="drawer-heading"><div><h2>提醒列表</h2><p>查看与管理全部提醒</p></div><button aria-label="关闭提醒列表" @click="opened=false">×</button></header>
    <div class="drawer-filter"><button class="drawer-date-trigger" :class="{'has-range':startDate||endDate}" :aria-expanded="datesOpen" @click="datesOpen=!datesOpen"><span class="drawer-date-icon"><UiIcon name="calendar"/></span><span class="drawer-date-label">{{dateLabel}}</span><UiIcon class="drawer-date-chevron" name="chevron"/></button><button class="drawer-reset" @click="reset"><UiIcon name="reset"/><span>还原</span></button></div>
    <div v-if="datesOpen" class="drawer-date-fields"><label>开始日期<input v-model="startDate" type="date" :max="endDate||undefined" aria-label="开始日期"/></label><label>结束日期<input v-model="endDate" type="date" :min="startDate||undefined" aria-label="结束日期"/></label><p v-if="invalidRange" role="alert">结束日期不能早于开始日期</p><p v-else>按卡片显示的提醒日期筛选</p></div>
    <div class="drawer-tabs" role="tablist" aria-label="提醒归属"><button v-for="tab in tabs" :key="tab.key" role="tab" :aria-selected="type===tab.key" @click="type=tab.key">{{tab.title}}</button></div>
    <div class="drawer-events" role="region" aria-label="分组提醒列表" tabindex="0">
      <p v-if="loading" class="drawer-empty" role="status">正在读取提醒…</p>
      <div v-else-if="error" class="drawer-empty" role="alert">提醒列表加载失败<button @click="refresh">重新加载</button></div>
      <template v-else><section v-for="group in groups" :key="group.key" :class="['reminder-group',group.key]"><h3>{{group.title}} <span>{{group.items.length}}</span></h3>
        <p v-if="!group.items.length" class="group-empty">暂无事件</p>
        <article v-for="item in group.items" :key="item.eventId" class="drawer-event"><span class="drawer-symbol"><UiIcon :name="group.icon"/></span><div><strong>{{item.summary}}</strong><p>{{item.source==='received'?'来自'+item.creatorName:'发给'+[...new Set(item.people)].join('、')}} · {{item.differentTimes?'各接收人时间不同':reminderDateLabel(item.displayTime)}}</p></div><span class="drawer-status">{{group.status}}</span><button :aria-label="'查看事件：'+item.summary" @click="openEvent(item)"><UiIcon name="chevron"/></button></article>
      </section></template>
    </div>
    <footer>按北京时间分组 · 待提醒按计划评估时间展示</footer>
  </el-drawer>
</template>
<script setup>
import {computed,ref,watch} from 'vue';
import {useRouter} from 'vue-router';
import UiIcon from './UiIcon.vue';
import {getReminderDrawer} from '@/api/smartReminder';
import {reminderGroups,reminderDateLabel} from './reminderGroups.mjs';
import {useSilentRefresh} from './useSilentRefresh';
const props=defineProps({modelValue:Boolean}),emit=defineEmits(['update:modelValue']);
const opened=computed({get:()=>props.modelValue,set:v=>emit('update:modelValue',v)}),router=useRouter();
const tabs=[{key:'all',title:'全部'},{key:'sent',title:'我发起的'},{key:'received',title:'我收到的'}],type=ref('all'),rows=ref([]),loading=ref(false),error=ref(false),now=ref(Date.now());let ticket=0;
const startDate=ref(''),endDate=ref(''),datesOpen=ref(false);
const invalidRange=computed(()=>Boolean(startDate.value&&endDate.value&&startDate.value>endDate.value));
const dateLabel=computed(()=>startDate.value||endDate.value?`${startDate.value.replaceAll('-','/')||'不限'} — ${endDate.value.replaceAll('-','/')||'不限'}`:'选择日期范围');
const groups=computed(()=>reminderGroups(invalidRange.value?[]:rows.value,now.value,[startDate.value,endDate.value]));
const reset=()=>{startDate.value='';endDate.value='';datesOpen.value=false;type.value='all';};
const refresh=async(silent=false)=>{if(!props.modelValue)return;const current=++ticket,requested=type.value;if(!silent)loading.value=true;error.value=false;try{
  const sources=requested==='all'?['sent','received']:[requested];
  const results=await Promise.all(sources.map(async source=>{const response=await getReminderDrawer(source);return(response.data.data||[]).map(row=>({...row,source}));}));
  if(current===ticket&&props.modelValue){rows.value=results.flat();now.value=Date.now();}
}catch{if(current===ticket&&!silent)error.value=true;}finally{if(current===ticket)loading.value=false;}};
watch(type,()=>{rows.value=[];void refresh();});watch(()=>props.modelValue,value=>{if(!value){ticket++;loading.value=false;}});
useSilentRefresh(()=>refresh(true));
const openEvent=item=>{opened.value=false;router.push({path:'/app/event/'+item.eventId,query:{from:item.source}});};
</script>
<style>
.reminder-drawer.el-drawer{background:#f2f7ff url('/images/login-background.png') center top/cover;max-width:100vw;border-radius:0 26px 26px 0;padding-top:env(safe-area-inset-top);padding-bottom:env(safe-area-inset-bottom);box-sizing:border-box;color:#233250}
.reminder-drawer .el-drawer__body{display:flex;flex-direction:column;overflow:hidden;min-height:0;padding:22px 16px;background:#f6faffee;touch-action:manipulation}
.reminder-drawer *{box-sizing:border-box;touch-action:manipulation}
.drawer-heading{display:flex;align-items:center;justify-content:space-between;gap:12px;flex:none}.drawer-heading h2{margin:0;font-size:24px}.drawer-heading p{margin:7px 0 0;color:#8793ad;font-size:13px}.drawer-heading button{width:42px;height:42px;border:1px solid #fff;border-radius:50%;background:#ffffffa6;color:#8790ad;font-size:28px;flex:none}
.drawer-tabs{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin:22px 0 16px;flex:none}.drawer-tabs button{min-height:42px;border:0;border-radius:22px;color:#435376;background:#eaf0fb;font:inherit;font-size:14px}.drawer-tabs button[aria-selected=true]{color:#fff;background:linear-gradient(125deg,#29b1f7,#365eff);box-shadow:0 5px 15px #4b8af92b}
.drawer-events{min-height:0;flex:1;overflow-y:auto;overscroll-behavior:contain;scrollbar-width:thin;scrollbar-color:#c3d3ee transparent;padding-right:3px}.reminder-group{--group-color:#1675e9;--group-bg:#eaf3ff;padding:14px 10px;border-radius:20px;background:linear-gradient(130deg,#eaf2ff8f,#f4f8ff);margin-bottom:13px}.reminder-group.sent{--group-color:#11a876;--group-bg:#e7faf0}.reminder-group.tomorrow{--group-color:#8655e9;--group-bg:#f0eaff}.reminder-group.later{--group-color:#e79a29;--group-bg:#fff5e7}.reminder-group h3{margin:0 0 12px;font-size:16px}.reminder-group h3 span{margin-left:8px;color:var(--group-color)}
.drawer-event{display:flex;align-items:center;gap:10px;background:#ffffffeb;border:1px solid #fff;border-radius:17px;padding:14px 10px;margin-top:9px}.drawer-symbol{display:grid;place-items:center;width:36px;height:36px;flex:none;border-radius:50%;color:var(--group-color);background:var(--group-bg)}.drawer-event>div{flex:1;min-width:0}.drawer-event strong{font-size:14px;line-height:1.6;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;overflow-wrap:anywhere}.drawer-event p{color:#8693ae;font-size:12px;line-height:1.6;margin:5px 0 0;overflow-wrap:anywhere}.drawer-event button{flex:none;width:34px;min-height:44px;border:0;background:transparent;color:#8a96b0;border-radius:12px}.drawer-event button:active{background:#edf2ff}.drawer-empty,.group-empty{color:#8a96ae;font-size:13px;line-height:1.7}.drawer-empty button{display:block;margin:12px auto;padding:10px;border:0;border-radius:12px;background:#e9efff;color:#4564c8}.group-empty{margin:8px 0}.reminder-drawer footer{flex:none;text-align:center;padding-top:16px;margin-top:8px;border-top:1px solid #dfe6f3;color:#8a96af;font-size:11px}
@media(prefers-reduced-motion:reduce){.reminder-drawer{transition:none!important}}
.reminder-drawer.el-drawer{color:#13234b;background:radial-gradient(ellipse at 80% 20%,#dceaff,transparent 65%),radial-gradient(ellipse at 10% 70%,#e4f2ff,transparent 60%),#f5f9ff}
.reminder-drawer .el-drawer__body{padding:28px 12px 24px;background:linear-gradient(125deg,#f8fbfff0,#eff6ffe0 55%,#f9fbfff5)}
.drawer-heading{padding:0 6px}.drawer-heading h2{font-size:23px;font-weight:750}.drawer-heading p{color:#7b84a6}.drawer-heading button{background:#eef6ff75}
.drawer-filter{display:flex;gap:7px;margin-top:22px;flex:none}.drawer-filter button{min-height:46px;border:1px solid #d9e2f5;border-radius:12px;box-shadow:0 2px 5px #778db510;font:inherit}
.drawer-date-trigger{min-width:0;flex:1;display:flex;align-items:center;justify-content:space-between;gap:5px;padding:0 10px;background:#ffffffcf;color:#7d87ac;font-size:12px!important}.drawer-date-trigger span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.drawer-date-trigger svg{width:19px;flex:none}.drawer-reset{flex:none;padding:0 10px;background:#edf5ffb8;color:#0874e8;font-size:14px!important}.drawer-reset span{font-size:24px;vertical-align:-2px}
.drawer-date-fields{flex:none;display:grid;grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:8px;padding:12px 3px 0}.drawer-date-fields label{min-width:0;font-size:12px;color:#727f9d}.drawer-date-fields input{display:block;width:100%;min-width:0;margin-top:5px;padding:7px 4px;border:1px solid #dae3f4;border-radius:9px;background:#fff;color:#293c63;font-size:16px}.drawer-date-fields p{grid-column:1/-1;margin:0;color:#8692aa;font-size:11px}.drawer-date-fields [role=alert]{color:#ce4646}
.drawer-tabs{grid-template-columns:1fr 1fr 1fr;gap:8px;margin:16px 0}.drawer-tabs button{background:#eaf2fb99;color:#192b50}.drawer-tabs button[aria-selected=true]{background:linear-gradient(125deg,#28b6f4,#0761fa);box-shadow:inset 0 2px 8px #ffffff35,0 5px 12px #2295f518}
.reminder-group{padding:14px 6px 7px;border-radius:19px;background:linear-gradient(125deg,#e8f1fcaa,#eaf2fc75);margin-bottom:17px}.reminder-group h3{padding:0 6px;margin-bottom:12px;font-size:15px}.drawer-event{gap:7px;padding:13px 6px;border-radius:16px;min-height:76px}.drawer-symbol{width:30px;height:30px}.drawer-symbol svg{width:20px;height:20px}.drawer-event strong{font-size:14px;font-weight:650;line-height:1.5}.drawer-event p{font-size:12px;line-height:1.45;margin-top:5px}.drawer-status{flex:none;white-space:nowrap;font-size:11px;font-weight:600;padding:7px 9px;border-radius:18px;color:var(--group-color);background:var(--group-bg)}.drawer-event button{width:24px;padding:0;min-height:44px}.drawer-event button svg{width:18px}.reminder-drawer footer{font-size:11px;margin:10px 7px 0;padding-top:18px;line-height:1.6}
.drawer-filter button:active,.drawer-heading button:active{transform:scale(.97)}
@media(max-width:360px){.drawer-event{gap:5px}.drawer-status{padding:6px;font-size:10px}.drawer-symbol{width:26px;height:26px}.drawer-event strong{font-size:13px}}
.drawer-filter{gap:8px;margin-top:19px;align-items:stretch}.drawer-filter button{min-height:46px;border-radius:15px;border:1px solid #e2e9f6;box-shadow:0 3px 12px #7b94be0a,inset 0 1px 0 #fff}
.drawer-date-trigger{padding:0 10px 0 7px;gap:8px;background:linear-gradient(145deg,#fff,#f7faff);font-size:12px!important;color:#8894ae;text-align:left}
.drawer-date-trigger .drawer-date-icon{display:grid;place-items:center;flex:none;width:29px;height:29px;border-radius:10px;background:#edf3ff;color:#7288c9}.drawer-date-trigger .drawer-date-icon svg{width:17px;height:17px}
.drawer-date-trigger .drawer-date-label{flex:1;min-width:0;font-variant-numeric:tabular-nums;white-space:normal;overflow-wrap:anywhere;line-height:1.5;padding:7px 0}
.drawer-date-trigger .drawer-date-chevron{width:12px;height:12px;color:#a6b1c7;transform:rotate(90deg);transition:transform .18s}.drawer-date-trigger[aria-expanded=true]{border-color:#c3d2f4;box-shadow:0 0 0 3px #e7efff80}.drawer-date-trigger[aria-expanded=true] .drawer-date-chevron{transform:rotate(-90deg)}.drawer-date-trigger.has-range{color:#4d6292}
.drawer-reset{display:flex;align-items:center;justify-content:center;gap:5px;min-width:64px;padding:0 10px;background:linear-gradient(140deg,#f4f8ff,#eef2ff);color:#6a7fb6;font-size:12px!important}.drawer-reset .ui-icon{width:17px;height:17px}.drawer-reset span{font-size:12px;vertical-align:baseline;font-weight:550}
.drawer-tabs{display:flex;gap:5px;margin:10px 0 13px;padding:3px;border-radius:20px;background:#e9eff880;align-self:flex-start;max-width:100%}
.drawer-tabs button{flex:0 1 auto;min-height:32px;min-width:62px;padding:0 15px;border-radius:17px;background:transparent;font-size:12px;color:#78869f;box-shadow:none}
.drawer-tabs button[aria-selected=true]{background:linear-gradient(120deg,#68baf6,#628bfa);box-shadow:0 2px 7px #628cf529,inset 0 1px 0 #ffffff50;color:#fff}
.drawer-date-fields{padding:12px 10px 10px;margin-top:8px;border:1px solid #e3eaf7;border-radius:15px;background:#ffffff8c}
@media(max-width:360px){.drawer-date-trigger{gap:5px;padding-right:7px;font-size:11px!important}.drawer-date-trigger .drawer-date-icon{width:25px;height:27px}.drawer-reset{min-width:57px;padding:0 8px}.drawer-tabs button{padding:0 12px;min-width:57px}}
@media(prefers-reduced-motion:reduce){.drawer-date-trigger .drawer-date-chevron{transition:none}}
</style>
