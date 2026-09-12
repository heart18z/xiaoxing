<template>
  <AppShell :title="mt('我的事件')" :subtitle="mt('帮你持续跟进每一件事')" variant="events">
    <template #action><button class="all-toggle" :aria-pressed="status==='ALL'" @click="changeStatus(status==='ALL'?'ACTIVE':'ALL')"><UiIcon name="filter" />{{ status==='ALL'?mt('只看进行中'):mt('显示全部') }}</button></template>
    <div class="sr-segment type-tabs">
      <button :class="{active:type==='sent'}" @click="changeType('sent')">{{ mt("我发起的") }}<span>{{ counts.sent }}</span></button>
      <button :class="{active:type==='received'}" @click="changeType('received')">{{ mt("我收到的") }}<span>{{ counts.received }}</span></button>
    </div>
    <div class="event-search"><UiIcon name="search" /><input v-model="keyword" type="search" :placeholder="mt('搜索事件内容、编号或发起人')" :aria-label="mt('搜索事件')" maxlength="100" /><button v-if="keyword" type="button" @click="keyword=''" :aria-label="mt('清空搜索')">×</button></div>
    <div class="event-list-scroll" role="region" :aria-label="mt('事件列表')" tabindex="0">
    <div v-if="loading" class="sr-empty">{{ mt("正在读取…") }}</div>
    <div v-else-if="!filteredEvents.length" class="sr-empty">{{ mt("当前筛选下还没有事件") }}</div>
    <article v-for="item in filteredEvents" :key="item.id" class="sr-card event" tabindex="0" role="button" :aria-label="mt('查看事件：')+item.eventSummary" @click="openEvent(item)" @keydown.enter.prevent="openEvent(item)" @keydown.space.prevent="openEvent(item)">
      <div class="sr-row event-top">
        <span :class="['status',(item.branchStatus||item.eventStatus).toLowerCase()]">{{ mt(statusText(item.branchStatus||item.eventStatus)) }}</span>
        <small>{{ item.eventNo }}</small><UiIcon name="chevron" class="event-chevron" />
      </div>
      <h3>{{ item.eventSummary }}</h3>
      <p class="event-people" v-if="type==='received'"><UiIcon name="people" />{{ mt("发起人：") }}{{ item.creatorName || item.creatorRealName }}</p>
      <p class="event-people" v-else><UiIcon name="people" />{{ mt("接收人") }}{{ item.recipientCount }}{{ mt("人 · 进行中") }}{{ item.activeBranchCount }}{{ mt("人") }}</p>
      <div class="latest"><UiIcon name="note" /><span>{{ mt("最新进展") }}</span><b>{{ item.latestFact || mt('暂无反馈，等待下一次评估') }}</b></div>
      <div class="event-dates"><p class="created"><UiIcon name="clock" /><span>{{ mt("创建时间") }}</span><time>{{ formatTime(item.createTime) }}</time></p>
      <p class="next"><UiIcon name="calendar" /><span>{{ mt("下次评估") }}</span><time>{{ item.nextEvaluateTime || mt('无') }}</time></p></div>
    </article>
    </div>
  </AppShell>
</template>

<script setup>
import {mt} from './mobileLocale';
import { computed,onMounted,reactive,ref,watch } from 'vue';
import { useRoute,useRouter } from 'vue-router';
import AppShell from './AppShell.vue';
import UiIcon from './UiIcon.vue';
import { bootstrap,getEvents } from '@/api/smartReminder';
import { withMobileLoading } from './mobileLoading';

const route=useRoute(),router=useRouter();
const initialType=route.query.type==='received'?'received':'sent',initialStatus=route.query.status==='ALL'?'ALL':'ACTIVE';
const type=ref(initialType),status=ref(initialStatus),events=ref([]),loading=ref(false),counts=reactive({sent:0,received:0});
const keyword=ref(String(route.query.q||''));
const filteredEvents=computed(()=>{const q=keyword.value.trim().toLocaleLowerCase();return q?events.value.filter(item=>[item.eventSummary,item.eventNo,item.creatorName,item.creatorRealName,item.latestFact].some(v=>String(v||'').toLocaleLowerCase().includes(q))):events.value;});
const loadCounts=async()=>{const data=(await bootstrap()).data.data||{};counts.sent=Number(data.sentActiveEventCount||0);counts.received=Number(data.receivedActiveEventCount||0);};
let loadTicket=0;
const load=async(showPageLoading=false)=>{const task=async()=>{const ticket=++loadTicket;loading.value=true;try{const [,response]=await Promise.all([loadCounts(),getEvents(type.value,status.value==='ALL'?'':status.value)]);if(ticket===loadTicket)events.value=response.data.data||[];}finally{if(ticket===loadTicket)loading.value=false;}};return showPageLoading?withMobileLoading(task):task();};
const syncLocation=()=>router.replace({path:'/app/events',query:{type:type.value,...(keyword.value?{q:keyword.value}:{}),...(status.value?{status:status.value}:{})}});
const changeType=value=>{type.value=value;syncLocation();load();};
const changeStatus=value=>{status.value=value;syncLocation();load();};
const openEvent=item=>router.push({path:'/app/event/'+item.id,query:{from:type.value,...(keyword.value?{q:keyword.value}:{}),...(status.value?{status:status.value}:{})}});
const statusText=value=>({ACTIVE:'进行中',STOPPED:'已停止',COMPLETED:'已完成'}[value]||value);
const formatTime=value=>String(value||'').replace('T',' ').slice(0,16)||'—';
watch(keyword,syncLocation);
onMounted(()=>load(true));
</script>

<style scoped>
.event-search{flex:0 0 auto;display:flex;align-items:center;gap:9px;padding:10px 13px;margin:0 0 12px;border:1px solid #fff;border-radius:16px;background:#ffffffdd;color:#8995b2}.event-search input{flex:1;min-width:0;border:0;outline:0;background:none;color:#263551;font:inherit;font-size:13px}.event-search input::-webkit-search-cancel-button{display:none}.event-search button{border:0;background:none;font-size:20px;color:#8995b2;cursor:pointer}
.all-toggle{border:1px solid #ffffff60;background:#ffffffed;color:#2565b4;border-radius:12px;padding:8px 12px;font:inherit;font-size:12px;cursor:pointer;white-space:nowrap}
.type-tabs{margin-bottom:10px}.sr-segment button span{display:inline-flex;align-items:center;justify-content:center;min-width:20px;height:20px;margin-left:5px;padding:0 6px;border-radius:10px;background:#dce3f5;color:#68758f;font-size:11px}.sr-segment button.active span{background:#315bff;color:#fff}.status-tabs{display:flex;gap:9px;margin:0 0 14px;overflow-x:auto;padding:1px}.status-tabs button{display:flex;align-items:center;justify-content:center;gap:5px;min-width:78px;height:34px;padding:0 14px;border:0;border-radius:18px;background:rgba(255,255,255,.82);color:#727d91;box-shadow:0 3px 12px rgba(35,69,115,.05)}.status-tabs button span{display:inline-flex;align-items:center;justify-content:center;min-width:18px;height:18px;padding:0 5px;border-radius:10px;background:#edf1f8;color:#71809a;font-size:10px}.status-tabs button.active{background:linear-gradient(135deg,#1687f8,#315bff);color:#fff;box-shadow:0 7px 16px rgba(39,108,244,.25)}.status-tabs button.active span{background:rgba(255,255,255,.24);color:#fff}.event{cursor:pointer;padding:14px 15px}.event h3{margin-top:9px}.event small{color:#a0a7b6;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.event-top>small{flex:0 1 auto}.event-top>i{font-style:normal;color:#a4adbd;font-size:21px;line-height:1}.status{flex:0 0 auto!important;font-size:12px;color:#315bff;background:#eef2ff;border-radius:20px;padding:4px 9px}.status.stopped,.status.completed{color:#7c8494;background:#eff1f4}.latest{margin-top:10px;padding:9px 10px;border-radius:11px;background:linear-gradient(90deg,#f0f6ff,#f7faff);display:flex;gap:8px;align-items:flex-start}.latest span{flex:0 0 auto;color:#5576a8;font-size:12px}.latest b{font-size:13px;line-height:1.45;color:#334159;font-weight:500;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden}.event .created{margin-top:9px;color:#8b96aa;font-size:12px}.event .next{margin-top:5px}
</style>
