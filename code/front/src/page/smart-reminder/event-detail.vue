<template>
  <AppShell :title="mt('事件详情')" :subtitle="mt('决策过程全程留痕')" variant="event-detail" back :back-to="backTarget">
    <template #action><button v-if="detail.creator&&detail.event?.event_status==='ACTIVE'" class="sr-button danger" @click="stop">{{ mt("停止") }}</button></template>

    <section v-if="detail.event" class="sr-card hero">
      <span class="sr-chip">{{ detail.event.event_no }}</span>
      <span class="summary-label">{{ mt(detail.creator?'事件概述':'我的任务') }}</span>
      <h2 :class="{'overview-collapsed':overviewLong&&!overviewExpanded}">{{ detail.event.latest_summary || detail.event.event_summary }}</h2>
      <button v-if="overviewLong" class="overview-toggle" type="button" :aria-expanded="overviewExpanded" @click="overviewExpanded=!overviewExpanded">{{ mt(overviewExpanded?'收起':'展开完整概述') }}</button>
      <div v-if="progressItems.length" class="overview-progress">
        <b>{{ mt('最新进展') }}</b>
        <p v-for="b in visibleProgress" :key="b.id"><span v-if="detail.creator">{{ b.name||b.realName||b.account }}： </span>{{ b.currentFact }}</p>
        <button v-if="progressItems.length>2" class="overview-toggle" type="button" :aria-expanded="progressExpanded" @click="progressExpanded=!progressExpanded">{{ mt(progressExpanded?'收起':'查看全部进展') }}</button>
      </div>
      <p>{{ mt("事件时间：") }}{{ detail.event.event_time || mt('未设定') }}</p>
      <p>{{ mt("截止时间：") }}{{ detail.event.deadline_time || mt('未设定') }}</p>
      <div v-if="detail.creator" class="conversation-entry creator-entry">
        <div><b>{{ mt("发起人对话") }}</b><small>{{ mt("查看事件创建、调整及反馈记录") }}</small></div>
        <button type="button" @click="viewConversation(detail.event.creator_user_id,detail.event.creatorName||detail.event.creatorRealName||'发起人')">{{ mt("查看对话") }}</button>
      </div>
    </section>

    <section v-if="detail.relatedEvents?.length" class="sr-card related-events"><b>{{ mt("合并关联") }}</b><router-link v-for="item in detail.relatedEvents" :key="item.id" :to="'/app/event/'+item.id">{{ item.summary }} ›</router-link></section>
    <h3 class="section-title">{{ mt("接收人分支") }}</h3>
    <section v-for="b in detail.branches||[]" :key="b.id" class="sr-card branch">
      <div class="sr-row">
        <div class="sr-avatar"><img v-if="b.avatar" :src="b.avatar" alt=""/><template v-else>{{ (b.name||b.realName||b.account||'?').slice(0,1) }}</template></div>
        <div class="branch-main"><h3>{{ b.name||b.realName||b.account }}</h3><p>{{ mt(branchStatus(b.branchStatus)) }}{{ mt("· 下次评估") }}{{ b.nextEvaluateTime||mt('无') }}</p></div>
        <button type="button" class="conversation-button" @click="viewConversation(b.recipientUserId,b.name||b.realName||b.account)">{{ mt("查看对话") }}</button>
      </div>
      <p v-if="b.latestSummary" class="branch-summary">{{ mt('当前任务：') }}{{ b.latestSummary }}</p>
      <p v-if="b.currentFact">{{ mt("最新事实：") }}{{ b.currentFact }}</p>
    </section>

    <div class="timeline-head">
      <div class="timeline-title-row"><h3 class="section-title">{{ mt("时间轴") }}</h3><label class="ai-toggle"><span>{{ mt("AI节点") }}</span><input v-model="showAiNodes" type="checkbox"/><i></i></label></div>
      <div class="timeline-tabs"><button :class="{active:timelineBranch==='all'}" @click="timelineBranch='all'">{{ mt("全部") }}</button><button v-for="b in detail.branches||[]" :key="b.id" :class="{active:timelineBranch===String(b.id)}" @click="timelineBranch=String(b.id)">{{ b.name||b.realName||b.account }}</button></div>
    </div>
    <div class="timeline"><div v-for="item in displayTimeline" :key="item.id" :class="['node',{ai:isAiAction(item)}]"><i></i><div><b><span v-if="isAiAction(item)" class="ai-mark">AI</span>{{ item.nodeType==='BRANCH_TASK_UPDATED'?mt('任务已更新'):mt(nodeName(item)) }}</b><p>{{ item.content }}</p><small>{{ mt("操作人：") }}{{ timelineActor(item) }} · </small><small>{{ item.createTime }}</small></div></div><div v-if="!displayTimeline.length" class="sr-empty">{{ mt("当前分支暂无时间轴记录") }}</div></div>

    <el-dialog v-model="conversationVisible" :title="conversationTitle" width="min(92vw,560px)" top="8vh" append-to-body destroy-on-close class="event-conversation-dialog">
      <div v-if="conversationLoading" class="conversation-empty">{{ mt("正在读取对话…") }}</div>
      <div v-else-if="!conversation.messages?.length" class="conversation-empty">{{ mt("该事件暂时没有对话记录") }}</div>
      <div v-else class="conversation-list">
        <div v-for="message in conversation.messages" :key="message.id" :class="['conversation-message',message.messageRole]">
          <div :class="['conversation-avatar',{assistant:message.messageRole==='assistant'}]" :aria-label="message.messageRole==='assistant'?'AI':conversationName">
            <img v-if="message.messageRole==='assistant'||conversation.participant?.avatar" :src="message.messageRole==='assistant'?(conversation.aiAvatar||'/avatars/assistant/A3.png'):conversation.participant.avatar" :alt="mt('用户头像')" />
            <span v-else>{{ message.messageRole==='assistant'?'AI':(conversationName||mt('我')).slice(0,1) }}</span>
          </div>
          <div class="conversation-bubble">
            <div v-if="message.messageRole==='assistant'" class="dialog-markdown" v-html="renderMarkdown(message.content||'')"></div>
            <div v-else class="dialog-text">{{ message.content }}</div>
            <time>{{ formatTime(message.createTime) }}</time>
          </div>
        </div>
      </div>
    </el-dialog>
  </AppShell>
</template>

<script setup>
import {mt} from './mobileLocale';
import { computed,onMounted,onBeforeUnmount,ref,watch } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessageBox,ElMessage } from 'element-plus';
import AppShell from './AppShell.vue';
import { getEventConversation,getEventDetail,stopEvent } from '@/api/smartReminder';
import { renderMarkdown } from './markdown';
import { withMobileLoading } from './mobileLoading';

const route=useRoute(),detail=ref({}),timelineBranch=ref('all'),showAiNodes=ref(true);
const overviewExpanded=ref(false),progressExpanded=ref(false);
const overviewLong=computed(()=>String(detail.value.event?.latest_summary||detail.value.event?.event_summary||'').length>85);
const progressItems=computed(()=>(detail.value.branches||[]).filter(b=>String(b.currentFact||'').trim()));
const visibleProgress=computed(()=>progressExpanded.value?progressItems.value:progressItems.value.slice(0,2));
watch(()=>route.params.id,()=>{overviewExpanded.value=false;progressExpanded.value=false;});
const conversationVisible=ref(false),conversationLoading=ref(false),conversation=ref({messages:[]}),conversationName=ref('');
const backTarget=computed(()=>['sent','received'].includes(String(route.query.from||''))?{path:'/app/events',query:{type:route.query.from,...(route.query.q?{q:route.query.q}:{}),...(route.query.status?{status:route.query.status}:{})}}:null);
const conversationTitle=computed(()=>`${conversationName.value||conversation.value.participant?.displayName||mt('用户')} · ${mt('事件对话')}`);
const aiTypes=new Set(['AI_BRANCH_RESUMED','FIRST_EVALUATION_PLANNED','EVALUATION_TIME_ADJUSTED','NEXT_EVALUATION_PLANNED','ASK_CREATOR','ASK_RECIPIENT','REMINDER_SENT','AI_STOPPED','RECIPIENT_NOTIFIED','AI_RECIPIENT_ADDED','TIME_CONFLICT_DETECTED']);
const globalTypes=new Set(['EVENT_CREATED','EVENT_UPDATED','EVENT_STOPPED','EVENT_MERGED']);
const isAiAction=item=>Boolean(item.aiAction)||aiTypes.has(item.nodeType),timelineActor=item=>isAiAction(item)?'AI':(item.actorName||'系统');
let detailTicket=0,detailTimer,detailBusy=false,disposed=false;
const load=async(showPageLoading=false)=>{const id=String(route.params.id),ticket=++detailTicket;detailBusy=true;const task=async()=>{const data=(await getEventDetail(id)).data.data||{};if(!disposed&&ticket===detailTicket&&id===String(route.params.id))detail.value=data;};try{return await(showPageLoading?withMobileLoading(task):task());}finally{if(ticket===detailTicket)detailBusy=false;}};
const refreshDetail=async()=>{clearTimeout(detailTimer);if(disposed||document.hidden)return;try{if(!detailBusy)await load();}catch{}finally{if(!disposed&&!document.hidden)detailTimer=setTimeout(refreshDetail,5000);}};
const visibilityChanged=()=>{clearTimeout(detailTimer);if(!document.hidden)refreshDetail();};
const conversationCache=new Map();let conversationTicket=0;
watch(conversationVisible,visible=>{if(!visible)conversationTicket++;});
const viewConversation=async(userId,name)=>{
  const eventId=String(route.params.id),key=eventId+':'+userId,ticket=++conversationTicket;
  const cached=conversationCache.get(key);
  conversationName.value=name||'';conversationVisible.value=true;
  if(cached&&Date.now()-cached.time<15000){conversation.value=cached.data;conversationLoading.value=false;return;}
  conversation.value={messages:[]};conversationLoading.value=true;
  try{
    const data=(await getEventConversation(eventId,userId)).data.data||{messages:[]};
    if(ticket!==conversationTicket||eventId!==String(route.params.id))return;
    conversationCache.set(key,{data,time:Date.now()});
    conversation.value=data;conversationName.value=data.participant?.displayName||conversationName.value;
  }catch(error){if(ticket===conversationTicket){conversationVisible.value=false;ElMessage.error(mt(error?.message||'读取对话失败'));}}
  finally{if(ticket===conversationTicket)conversationLoading.value=false;}
};
const displayTimeline=computed(()=>{const seen=new Set(),selected=timelineBranch.value;return(detail.value.timeline||[]).filter(item=>showAiNodes.value||!isAiAction(item)).filter(item=>selected==='all'||String(item.branchId||'')===selected||(!item.branchId&&globalTypes.has(item.nodeType))).filter(item=>{if(item.nodeType!=='NOTIFICATION_READ')return true;const minute=String(item.createTime||'').slice(0,16),actor=item.actorUserId||item.actorName||'',key=`${item.branchId||''}:${actor}:${minute}`;if(seen.has(key))return false;seen.add(key);return true;});});
const stop=async()=>{await ElMessageBox.confirm(mt('确定停止这个智能提醒吗？'),mt('停止提醒'),{confirmButtonText:mt('确定'),cancelButtonText:mt('取消')});await stopEvent({eventId:route.params.id,reason:'用户在事件详情手动停止'});window.dispatchEvent(new Event('smart-reminder:badge-refresh'));ElMessage.success(mt('已停止'));await load();};
const nodeName=item=>{const actor=!isAiAction(item)&&item.actorName&&item.actorName!=='系统'?item.actorName:'';if(item.nodeType==='NOTIFICATION_READ')return actor?`${actor} · ${mt('已读')}`:'提醒已读';if(item.nodeType==='RECIPIENT_FEEDBACK')return actor?`${actor} · ${mt('反馈')}`:'接收人反馈';if(item.nodeType==='EVENT_UPDATED')return actor?`${actor} · ${mt('更新事件')}`:'事件更新';if(item.nodeType==='EVENT_STOPPED')return actor?`${actor} · ${mt('停止事件')}`:'事件停止';if(item.nodeType==='BRANCH_STOPPED')return actor?`${actor} · ${mt('停止分支')}`:'分支停止';return({CREATOR_EVALUATION_REQUESTED:'发起人反馈触发评估',AI_BRANCH_RESUMED:'恢复提醒分支',EVENT_CREATED:'事件创建',EVENT_MERGED:'事件合并',FIRST_EVALUATION_PLANNED:'首次评估',EVALUATION_TIME_ADJUSTED:'评估时间优化',NEXT_EVALUATION_PLANNED:'规划下次评估',RECIPIENT_NOTIFIED:'通知接收人',REMINDER_SENT:'发出提醒',AI_RECIPIENT_ADDED:'新增接收人',TIME_CONFLICT_DETECTED:'发现时间冲突',ASK_CREATOR:'询问发起人',ASK_RECIPIENT:'询问接收人',AI_STOPPED:'AI停止分支'}[item.nodeType]||item.nodeType);};
const messageLabel=type=>({CANDIDATE:'事件确认',CANDIDATE_CONFIRMED:'事件确认',SYSTEM:'系统消息',TEXT:'AI回复',REMINDER:'提醒消息',QUESTION:'异常提醒',FEEDBACK:'进展反馈',EVENT_ASSIGNED:'事件通知',CONFLICT:'冲突处理'}[type]||'AI消息');
const branchStatus=s=>({ACTIVE:'进行中',STOPPED:'已停止',COMPLETED:'已完成'}[s]||s);
const formatTime=value=>String(value||'').replace('T',' ').slice(0,19);
onMounted(()=>{load(true).catch(()=>{});detailTimer=setTimeout(refreshDetail,5000);document.addEventListener('visibilitychange',visibilityChanged);});
onBeforeUnmount(()=>{disposed=true;detailTicket++;clearTimeout(detailTimer);document.removeEventListener('visibilitychange',visibilityChanged);});
watch(()=>route.params.id,()=>{conversationVisible.value=false;timelineBranch.value='all';load(true);});
</script>

<style scoped>
.hero h2{font-size:17px!important;line-height:1.65!important;font-weight:600;margin:6px 0 12px;white-space:normal!important}
.hero h2.overview-collapsed{display:-webkit-box;-webkit-line-clamp:4;-webkit-box-orient:vertical;overflow:hidden}
.overview-toggle{border:0;background:transparent;color:#526fcb;font-size:12px;padding:3px 0;cursor:pointer}
.overview-progress{padding:12px 14px;margin:14px 0;background:#f0f8f6;border:1px solid #dceee8;border-radius:14px}
.overview-progress>b{font-size:12px;color:#29856d}.overview-progress p{font-size:13px!important;line-height:1.65;color:#43635a!important;margin:6px 0 0!important;overflow-wrap:anywhere}
.summary-label{display:block;margin:14px 0 4px;color:#7c8ba8;font-size:12px}.hero h2{white-space:pre-line;overflow-wrap:anywhere;margin-top:4px}.branch-summary{color:#334b70!important;white-space:pre-line}
:global(.event-conversation-dialog.el-dialog){padding:0!important}
.related-events{display:flex;flex-direction:column;gap:9px}.related-events a{color:#3764d1;font-size:13px;text-decoration:none}
.conversation-message{gap:8px;align-items:flex-start}.conversation-message.user{flex-direction:row-reverse}.conversation-avatar{flex:0 0 30px;width:30px;height:30px;display:flex;align-items:center;justify-content:center;border-radius:10px;background:#e6edff;color:#4165cf;font-weight:700;font-size:13px;overflow:hidden}.conversation-avatar img{width:100%;height:100%;object-fit:cover}.conversation-avatar.assistant{background:linear-gradient(145deg,#358bff,#4dccd5);color:#fff}.conversation-message.user{justify-content:flex-start!important}.conversation-bubble{min-width:0}
.hero h2{font-size:22px;line-height:1.45}.section-title{font-size:15px;margin:22px 4px 10px}.conversation-entry{display:flex;align-items:center;justify-content:space-between;gap:12px;margin-top:14px;padding-top:13px;border-top:1px solid #edf1f7}.conversation-entry>div{display:flex;flex-direction:column;gap:3px}.conversation-entry b{font-size:14px;color:#263551}.conversation-entry small{font-size:11px;color:#97a2b5}.conversation-entry button,.conversation-button{flex:0 0 auto;border:0;border-radius:13px;background:#edf2ff;color:#315bff;font-weight:600;padding:8px 12px;cursor:pointer}.creator-entry button{background:linear-gradient(135deg,#315bff,#607cff);color:#fff;box-shadow:0 5px 12px rgba(49,91,255,.18)}.branch .sr-avatar{width:44px!important;flex:0 0 44px!important;padding:0;overflow:hidden}.branch .sr-avatar img{width:100%;height:100%;object-fit:cover}.branch-main{flex:1;min-width:0}.conversation-button{padding:7px 10px;font-size:12px}.timeline-head{margin-top:22px}.timeline-title-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:9px}.timeline-head .section-title{margin:0 4px}.ai-toggle{display:flex;align-items:center;gap:7px;color:#68758d;font-size:12px;cursor:pointer;user-select:none}.ai-toggle input{position:absolute;opacity:0;pointer-events:none}.ai-toggle i{position:relative;width:38px;height:22px;border-radius:12px;background:#cbd3e1;transition:.2s}.ai-toggle i:after{content:"";position:absolute;left:3px;top:3px;width:16px;height:16px;border-radius:50%;background:#fff;box-shadow:0 2px 5px #26354c33;transition:.2s}.ai-toggle input:checked+i{background:#7059da}.ai-toggle input:checked+i:after{transform:translateX(16px)}.timeline-tabs{display:flex;gap:7px;overflow-x:auto;padding:1px 2px 10px;scrollbar-width:none}.timeline-tabs button{flex:0 0 auto;border:1px solid #e0e7f2;border-radius:15px;background:rgba(255,255,255,.8);color:#778297;padding:6px 13px}.timeline-tabs button.active{border-color:#315bff;background:#315bff;color:#fff}.timeline{padding:4px 8px 0}.node{display:flex;gap:14px;min-height:82px;position:relative}.node>i{width:12px;height:12px;flex:0 0 12px;border:3px solid #315bff;background:#fff;border-radius:50%;margin-top:3px;z-index:1}.node:after{content:"";position:absolute;left:5px;top:15px;bottom:-3px;border-left:2px solid #bfd4ff}.node:last-child:after{display:none}.node p{margin:5px 0;color:#59667e;line-height:1.5}.node small{color:#98a3b6}.node b{color:#1d2c46}.node.ai>i{border-color:#805fe0;background:#eee9ff;box-shadow:0 0 0 4px #f4f1ff}.node.ai:after{border-color:#d3c9f3}.ai-mark{display:inline-flex;align-items:center;justify-content:center;margin-right:6px;padding:2px 6px;border-radius:7px;background:linear-gradient(135deg,#6b56d9,#9473e8);color:#fff;font-size:10px;vertical-align:1px}
.conversation-empty{padding:50px 15px;text-align:center;color:#909caf}.conversation-list{height:min(68vh,620px);overflow-y:auto;padding:4px 3px 18px}.conversation-message{display:flex;margin:11px 0}.conversation-message.user{justify-content:flex-end}.conversation-bubble{max-width:82%;padding:10px 13px;border-radius:15px;border-bottom-left-radius:5px;background:#f2f5fa;color:#253149;line-height:1.55;box-shadow:0 3px 12px rgba(31,55,92,.05)}.conversation-message.user .conversation-bubble{border-bottom-left-radius:15px;border-bottom-right-radius:5px;background:linear-gradient(135deg,#315bff,#607cff);color:#fff}.message-type{display:inline-flex;margin-bottom:5px;padding:2px 7px;border-radius:8px;background:#e4eaff;color:#4264dc;font-size:10px}.dialog-text{white-space:pre-wrap}.dialog-markdown{overflow-wrap:anywhere}.dialog-markdown :deep(p){margin:.15em 0 .55em}.dialog-markdown :deep(:first-child){margin-top:0}.dialog-markdown :deep(:last-child){margin-bottom:0}.conversation-bubble time{display:block;margin-top:6px;color:#9aa5b6;font-size:10px;text-align:left}.conversation-message.user time{color:rgba(255,255,255,.72);text-align:right}
:global(.event-conversation-dialog){border-radius:20px!important;overflow:hidden}:global(.event-conversation-dialog .el-dialog__header){margin:0;padding:17px 19px;border-bottom:1px solid #edf0f5}:global(.event-conversation-dialog .el-dialog__title){font-size:16px;font-weight:700;color:#1e2c45}:global(.event-conversation-dialog .el-dialog__body){padding:10px 14px 0}
</style>
