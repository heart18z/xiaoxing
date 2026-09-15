<template>
  <AppShell :title="mt('AI小醒')" variant="chat">
    <template #leading><button class="chat-toolbar-button" :aria-label="mt('查看我的事件')" :title="mt('提醒列表')" @click="drawerOpen=true"><UiIcon name="menu" /></button></template>
    <template #title><div class="chat-brand" :aria-label="mt('AI小醒')"><strong>{{ mt("AI小醒") }}</strong><small>{{ mt("智能提醒助手") }}</small></div></template>
    <template #action><button class="chat-toolbar-button clear-context-button" :aria-label="mt('清除当前上下文')" :title="mt('清除当前上下文')" :disabled="clearing" @click="requestClear"><UiIcon name="eraser" /></button></template>
    <ReminderDrawer v-model="drawerOpen" />
    <div v-if="recoveryNotice" class="chat-recovery" role="status">{{recoveryNotice}}<template v-if="!thinking"><button @click="resumePending">重新查询</button><button @click="drawerOpen=true">核对事件</button><button @click="dismissPending">结束等待</button></template></div>
    <div class="chat-history">
    <div ref="scroll" class="messages" role="region" :aria-label="mt('对话消息')" tabindex="0" @scroll.passive="trackScroll" @touchstart.passive="stopLayoutFollow" @pointerdown="stopLayoutFollow" @wheel.passive="stopLayoutFollow">
      <div v-if="historyLoading || historyError" class="welcome sr-card" role="status" aria-live="polite">
        <p>{{ historyLoading ? mt('正在加载对话记录…') : mt('对话记录加载失败，请检查网络后重试。') }}</p>
        <button v-if="historyError" class="sr-button" @click="load()">{{ mt("重新加载") }}</button>
      </div>
      <div v-else-if="!messages.length" class="welcome sr-card">
        <b>{{ mt("今天想提醒什么？") }}</b>
        <p>{{ mt("例如：“明天上午10点前提醒我确认合同，如果已完成就不要提醒。”") }}</p>
      </div>
      <div v-for="item in messages" :key="item.id" :data-message-id="item.id" :class="['message',item.messageRole]">
        <span v-if="item.messageRole==='user'&&!avatarState.user" class="chat-avatar avatar-initial">{{ avatarState.initial }}</span>
        <img v-else class="chat-avatar" :src="item.messageRole==='user'?avatarState.user:avatarState.ai" :alt="item.messageRole==='user'?mt('我的头像'):mt('AI头像')" />
        <div v-if="['CANDIDATE','CANDIDATE_CONFIRMED','CANDIDATE_CANCELLED','CANDIDATE_EXPIRED'].includes(item.messageType)" class="candidate-stack">
          <div v-if="item.content||reasoning(item)" :class="['bubble','candidate-reply',{'has-thinking':reasoning(item)}]">
            <ThinkingPanel v-if="reasoning(item)" :content="reasoning(item)" :open="item.thinkingOpen" :streaming="item.streaming&&!item.content" @toggle="item.thinkingOpen=!item.thinkingOpen" />
            <div v-if="item.content" class="markdown-content markdown-body" v-html="renderMarkdown(item.content)"></div>
          </div>
          <div :class="['candidate','sr-card',candidateKind(item)]">
            <div class="card-heading"><span class="card-icon">{{ candidateKind(item)==='macro' ? '◎' : '◷' }}</span><span class="sr-chip">{{ mt(candidateLabel(item)) }}</span></div>
            <p v-if="hasScheduleConflict(item)&&item.messageType==='CANDIDATE'" class="schedule-warning">{{ mt("发现不同事项的时间冲突，只有选择“仍要安排”后才会创建并通知接收人。") }}</p>
            <div v-for="(event,index) in payload(item).events || []" :key="index" :class="['candidate-event',eventKind(event)]">
              <b>{{ event.summary }}</b>
              <p>{{ mt("接收人：") }}{{ (event.recipients || []).map(userName).join('、') }}</p>
              <p>{{ eventKind(event)==='macro' ? mt('目标周期') : mt('事件时间') }}：{{ event.timeDescription || event.eventTime || mt('持续跟进') }}</p>
              <p>{{ eventKind(event)==='macro' ? mt('首次跟进') : mt('首次评估') }}：{{ event.firstEvaluateTime }}</p>
              <label v-if="item.messageType==='CANDIDATE'" class="alarm-choice"><input type="checkbox" :checked="wantsAlarm(item,index,event)" @change="alarmChoices[alarmKey(item,index)]=$event.target.checked" />同时设置本机系统闹铃（iOS 26+）</label>
              <small v-if="item.messageType==='CANDIDATE'&&wantsAlarm(item,index,event)" class="alarm-hint">仅为自己接收的任务设置，需系统授权；其他手机不会自动设置。</small>
            </div>
            <button class="sr-button" :class="{confirmed:item.messageType!=='CANDIDATE'}" :disabled="item.messageType!=='CANDIDATE'||confirmingId===String(payload(item).candidateId)" @click="confirm(payload(item).candidateId,item)">{{ item.messageType==='CANDIDATE_CONFIRMED' ? mt('已创建') : item.messageType==='CANDIDATE_CANCELLED' ? mt('已取消') : item.messageType==='CANDIDATE_EXPIRED' ? mt('已失效') : confirmingId===String(payload(item).candidateId) ? mt('正在创建…') : hasScheduleConflict(item) ? mt('仍要安排') : candidateKind(item)==='macro' ? mt('确认开始跟进') : mt('确认创建') }}</button>
          </div>
        </div>
        <div v-else-if="item.messageType==='EVENT_ASSIGNED'" class="bubble assigned-message">
          <div class="markdown-content" v-html="renderMarkdown(item.content||'')"></div>
          <div class="card-footer"><button type="button" class="event-text-link" @click="openEvent(item)">{{ mt("查看事件详情 ›") }}</button><time>{{ formatMessageTime(item.createTime) }}</time></div>
        </div>
        <div v-else-if="['REMINDER','QUESTION','FEEDBACK','CONFLICT'].includes(item.messageType)" :class="['notify','sr-card','card-'+cardType(item)]" @click="openEvent(item)">
          <ThinkingPanel v-if="reasoning(item)" :content="reasoning(item)" :open="item.thinkingOpen" :streaming="item.streaming&&!item.content" @click.stop @toggle="item.thinkingOpen=!item.thinkingOpen" />
          <div class="card-heading"><span class="card-icon">{{ cardIcon(item) }}</span><span class="sr-chip">{{ mt(cardLabel(item)) }}</span></div>
          <template v-if="item.messageType==='FEEDBACK'">
            <div class="notification-focus">
              <p class="notification-content">{{ payload(item).fact || item.content }}</p>
              <p v-if="payload(item).actor" class="notification-actor">{{ mt("反馈人：") }}{{ payload(item).actor }}</p>
              <p v-if="payload(item).eventSummary" class="notification-event"><span>{{ mt("关联事件") }}</span>{{ payload(item).eventSummary }}</p>
            </div>
          </template>
          <template v-else-if="item.messageType==='CONFLICT'||item.messageType==='QUESTION'">
            <div class="notification-focus">
              <p class="notification-content">{{ item.content }}</p>
              <p v-if="payload(item).eventSummary" class="notification-event"><span>{{ mt("关联事件") }}</span>{{ payload(item).eventSummary }}</p>
            </div>
          </template>
          <template v-else-if="item.messageType==='REMINDER'">
            <div class="reminder-focus">
              <strong>{{ item.content }}</strong>
              <p v-if="payload(item).eventSummary"><span>{{ mt("关联事件") }}</span>{{ payload(item).eventSummary }}</p>
            </div>
          </template>
          <template v-else>
            <div class="candidate-event near structured-event">
              <b v-if="payload(item).eventSummary">{{ payload(item).eventSummary }}</b>
              <p>{{ item.content }}</p>
            </div>
          </template>
          <div class="card-footer"><p v-if="item.eventId" class="card-link">{{ mt("查看事件详情 ›") }}</p><time>{{ formatMessageTime(item.createTime) }}</time></div>
        </div>
        <div v-else :class="['bubble',{'is-streaming':item.streaming,'has-thinking':reasoning(item)}]">
          <ThinkingPanel v-if="item.messageRole==='assistant'&&reasoning(item)" :content="reasoning(item)" :open="item.thinkingOpen" :streaming="item.streaming&&!item.content" @toggle="item.thinkingOpen=!item.thinkingOpen" />
          <div v-if="item.messageRole==='assistant'&&item.content" class="markdown-content" v-html="renderMarkdown(item.content)"></div>
          <template v-else>{{ item.content }}</template>
          <div v-if="item.messageType==='SYSTEM'&&payload(item).eventIds?.length" class="created-event-links"><button v-for="eventId in payload(item).eventIds" :key="eventId" type="button" class="event-text-link" @click="router.push('/app/event/'+eventId)">查看事件 / 设置本机闹铃 ›</button></div>
          <small v-if="item.messageRole==='user'&&payload(item).fileNames?.length" class="sent-files">{{ mt("附件：") }}{{ payload(item).fileNames.join('、') }}</small>
          <i v-if="item.streaming&&item.content" class="streaming-cursor"></i><ReplyProgress v-if="item.streaming&&!item.content&&!reasoning(item)" />
        </div>
      </div>
    </div>
    <button v-if="!followLatest && messages.length" type="button" class="back-to-latest" @click="scrollToLatest(true)"><UiIcon name="arrowDown" />{{ mt('回到最新') }}</button>
    </div>
    <form ref="composer" class="composer" @submit.prevent="send">
      <div v-if="files.length" class="file-strip">
        <span v-for="file in files" :key="file.key" :class="{failed:file.status==='error'}">
          <img v-if="file.previewUrl" :src="file.previewUrl" :alt="mt('图片附件')" />
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M8.5 12.5 14 7a3 3 0 0 1 4.2 4.2l-7.1 7.1a5 5 0 0 1-7.1-7.1l7.4-7.4" /></svg>
          {{ file.name }} <small>{{ file.status==='uploading'?mt('解析中…'):file.status==='error'?mt('解析失败'):mt('已就绪') }}</small>
          <button v-if="file.status==='error'" type="button" :aria-label="mt('重试上传')" @click="uploadOne(file)">{{ mt("重试") }}</button>
          <button type="button" :aria-label="mt('移除文件')" @click="removeFile(file.key)">×</button>
        </span>
      </div>
      <div class="composer-box">
        <div class="composer-input-row"><span class="composer-orb" aria-hidden="true"></span><textarea ref="textarea" v-model="text" :readonly="voiceBusy" rows="1" :placeholder="mt('说说你想提醒的事…')" @focus="followInputLayout" @input="resizeInput" @paste="pasteFiles" @keydown="inputKeydown"></textarea></div>
        <div class="composer-actions">
        <label class="upload" :title="mt('上传文件')" :aria-label="mt('上传文件')">
          <UiIcon name="plus" />
          <input type="file" multiple accept=".png,.jpg,.jpeg,.webp,.gif,.txt,.md,.doc,.docx,.pdf,.xlsx" @change="upload" />
        </label>
        <VoiceInput :disabled="thinking" @begin="beginSpeech" @partial="updateSpeech" @cancel="cancelSpeech" @busy="voiceBusy=$event" />
        <button v-if="thinking" type="button" class="send-button stop-button" :aria-label="mt('停止生成')" :title="mt('停止生成')" :disabled="stopping" @click="stop"><span aria-hidden="true">■</span></button>
        <button v-else class="send-button" :aria-label="mt('发送')" :title="mt('发送')" :disabled="voiceBusy||files.some(file=>file.status!=='ready')"><UiIcon name="arrowUp" /></button>
        </div>
      </div>
    </form>
    <el-dialog v-model="clearDialog" :title="mt('清除当前上下文')" width="min(360px, calc(100vw - 40px))" append-to-body align-center class="clear-context-dialog" :close-on-click-modal="!clearing" :close-on-press-escape="!clearing" :show-close="!clearing">
      <p>{{ mt('清除后，对话页将不再显示之前的消息，AI 也不再沿用此前的聊天上下文和未确认操作。') }}</p>
      <p class="clear-retained">{{ mt('后台聊天记录仍保留，好友、人员别称和已创建事件不受影响，提醒继续执行。') }}</p>
      <template #footer><button type="button" class="clear-cancel" :disabled="clearing" @click="clearDialog=false">{{ mt('取消') }}</button><button type="button" class="sr-button" :disabled="clearing" @click="clearCurrentContext">{{ clearing?mt('正在清除…'):mt('确认清除') }}</button></template>
    </el-dialog>
  </AppShell>
</template>

<script setup>
import {mt} from './mobileLocale';
import {mobileError} from './mobileError.mjs';
defineOptions({name:'SmartReminderChat'});
import { nextTick, onActivated, onDeactivated, onMounted, onBeforeUnmount, reactive, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import UiIcon from './UiIcon.vue';
import VoiceInput from './VoiceInput.vue';
import { ElMessage } from 'element-plus';
import AppShell from './AppShell.vue';
import ReminderDrawer from './ReminderDrawer.vue';
import {pendingChat,rememberPendingChat,clearPendingChat,followChatJob} from './pendingChat';
import {ElMessageBox} from 'element-plus';
import {setEventAlarm} from '@/native/alarms';
import {getEventDetail} from '@/api/smartReminder';
import ThinkingPanel from './ThinkingPanel.vue';
import ReplyProgress from './ReplyProgress.vue';
import { avatarState } from './avatarState';
import { clearChatContext, confirmCandidate, getMessages, syncMessages, readMessages, streamMessage, stopMessage, uploadFile } from '@/api/smartReminder';
import { renderMarkdown } from './markdown';

const router=useRouter(), messages=ref([]), text=ref(''), files=ref([]), thinking=ref(false), confirmingId=ref(''), scroll=ref();
const drawerOpen=ref(false),recoveryNotice=ref('');let disposed=false;
const textarea=ref(),composer=ref(),followLatest=ref(true);let composerObserver,historyLoaded=false,layoutFrame=0,layoutUntil=0,lastMessagesHeight=0;
const voiceBusy=ref(false);
const clearDialog=ref(false),clearing=ref(false);let historyVersion=0;
const requestClear=()=>{
  if(thinking.value||confirmingId.value||voiceBusy.value||files.value.some(file=>file.status==='uploading')){ElMessage.info(mt('请先结束当前操作，再清除上下文'));return;}
  clearDialog.value=true;
};
const clearCurrentContext=async()=>{
  if(clearing.value||thinking.value||confirmingId.value)return;
  clearing.value=true;historyVersion++;historyRequest=null;historyLoading.value=false;historyError.value=false;
  try{
    await clearChatContext();
    messages.value=[];text.value='';files.value.forEach(file=>{if(file.previewUrl)URL.revokeObjectURL(file.previewUrl);});files.value=[];
    historyLoaded=true;followLatest.value=true;clearDialog.value=false;syncRevision='';
    window.dispatchEvent(new Event('smart-reminder:badge-refresh'));
    ElMessage.success(mt('当前上下文已清除，后台记录仍保留'));
  }catch(e){ElMessage.error(mt(e?.response?.data?.msg||'清除失败，请重试'));}
  finally{clearing.value=false;}
};
const stopping=ref(false);let activeRequest,abortController,stopped=false;
const inputKeydown=e=>{if(e.key==='Enter'&&!e.shiftKey&&!e.isComposing&&e.keyCode!==229){e.preventDefault();send();}};
const stop=async()=>{if(!activeRequest||stopping.value)return;stopping.value=true;try{const response=await stopMessage(activeRequest);if(response.data.data?.stopped){stopped=true;recoveryNotice.value='正在停止，请等待服务器确认…';}else{ElMessage.info(mt('本轮已完成生成或正在执行操作，无法撤回，请等待结果'));}}catch{ElMessage.error(mt('停止失败，请重试'));}finally{stopping.value=false;}};
let speechDraft='';
const beginSpeech=()=>{speechDraft=text.value;textarea.value?.blur();};
const updateSpeech=value=>{text.value=speechDraft+(speechDraft.trim()?'\n':'')+value;nextTick(resizeInput);};
const cancelSpeech=()=>{nextTick(resizeInput);}; // Cancelling recording never erases the user's draft.
const stopLayoutFollow=()=>{layoutUntil=0;cancelAnimationFrame(layoutFrame);layoutFrame=0;};
const followInputLayout=()=>{
  if(!pageActive)return;
  stopLayoutFollow();followLatest.value=true;layoutUntil=performance.now()+450;
  const follow=()=>{if(!pageActive)return;scrollToLatest(true);if(performance.now()<layoutUntil)layoutFrame=requestAnimationFrame(follow);else{layoutUntil=0;layoutFrame=0;}};
  layoutFrame=requestAnimationFrame(follow);
};
const keyboardLayout=()=>{if(pageActive&&followLatest.value)followInputLayout();};
const trackScroll=()=>{
  const el=scroll.value;if(!el)return;
  const resized=lastMessagesHeight!==el.clientHeight;lastMessagesHeight=el.clientHeight;
  // Viewport shrink is layout, not a user's request to leave the latest message.
  if(layoutUntil>performance.now()||(resized&&followLatest.value)){scrollToLatest(true);return;}
  followLatest.value=el.scrollHeight-el.scrollTop-el.clientHeight<48;
};
const scrollToLatest=(force=false)=>{const el=scroll.value;if(el&&(force||followLatest.value)){el.scrollTop=el.scrollHeight;followLatest.value=true;}};
const resizeInput=()=>{const el=textarea.value;if(!el)return;el.style.height='auto';const height=Math.min(el.scrollHeight,150);el.style.height=Math.max(40,height)+'px';el.style.overflowY=el.scrollHeight>150?'auto':'hidden';};
watch(text,()=>nextTick(resizeInput));
const payload=item=>{try{return item.payload || (item.payloadJson ? JSON.parse(item.payloadJson) : {});}catch{return {};}};
const hasScheduleConflict=item=>(payload(item).events||[]).some(event=>event.scheduleConflicts?.length);
const reasoning=item=>item.reasoningContent||payload(item).reasoningContent||'';
const userName=u=>u.friendRemark || u.name || u.realName || u.account;
const eventKind=event=>{const description=String(event?.timeDescription||'');if(!event?.eventTime&&(description.includes('年')||description.includes('长期')||description.includes('目标')||!event?.deadlineTime))return 'macro';if(event?.deadlineTime&&new Date(event.deadlineTime)-Date.now()>30*86400000)return 'macro';return 'near';};
const candidateKind=item=>(payload(item).events||[]).some(event=>eventKind(event)==='macro')?'macro':'near';
const candidateLabel=item=>item.messageType==='CANDIDATE_EXPIRED'?'操作已失效':item.messageType==='CANDIDATE_CONFIRMED'?'事件已创建':item.messageType==='CANDIDATE_CANCELLED'?'事件已取消':candidateKind(item)==='macro'?'长期目标确认':'近期事件确认';
const cardType=item=>item.messageType==='REMINDER'?'reminder':item.messageType==='FEEDBACK'?'feedback':item.messageType==='EVENT_ASSIGNED'?'event':item.messageType==='QUESTION'?'exception':'conflict';
const cardLabel=item=>({reminder:'提醒消息',feedback:'进展反馈',event:'事件确认',exception:'异常提醒',conflict:'冲突处理'}[cardType(item)]);
const cardIcon=item=>({reminder:'◷',feedback:'✓',event:'◇',exception:'!',conflict:'!'}[cardType(item)]);
const formatMessageTime=value=>{const text=String(value||'').replace('T',' ');if(!text)return '';const date=text.slice(0,10),time=text.slice(11,16),today=new Date(),pad=n=>String(n).padStart(2,'0'),todayText=`${today.getFullYear()}-${pad(today.getMonth()+1)}-${pad(today.getDate())}`;return date===todayText?`${mt('今天')} ${time}`:`${date.slice(5)} ${time}`;};
const historyLoading=ref(false),historyError=ref(false);
let historyRequest;
let pageActive=false,syncTimer,syncRequest=false,syncRevision='',syncFailures=0;
const notificationTypes=new Set(['REMINDER','QUESTION','FEEDBACK','CONFLICT','EVENT_ASSIGNED']);
const applyHistory=async(rows,completed=false)=>{
  const shouldFollow=!historyLoaded||followLatest.value,el=scroll.value;
  const oldTop=el?.scrollTop||0;
  const anchor=el&&!shouldFollow?Array.from(el.querySelectorAll('[data-message-id]')).find(node=>node.getBoundingClientRect().bottom>el.getBoundingClientRect().top):null;
  const anchorId=anchor?.dataset.messageId,anchorTop=anchor?.getBoundingClientRect().top;
  const previous=new Map(messages.value.map(item=>[String(item.id),item]));
  if(thinking.value&&!completed){
    // Keep the live answer/optimistic user bubble; independently deliver incoming event notifications.
    for(const item of rows)if(notificationTypes.has(item.messageType)&&!previous.has(String(item.id)))messages.value.push({...item,thinkingOpen:false});
  }else{
    messages.value=rows.map(item=>({...item,thinkingOpen:previous.get(String(item.id))?.thinkingOpen||false}));
  }
  if(pageActive&&!document.hidden){
    const ids=rows.filter(item=>!item.isRead&&(!thinking.value||notificationTypes.has(item.messageType))).map(item=>item.id);
    if(ids.length)readMessages(ids).catch(()=>{});
  }
  await nextTick();
  if(shouldFollow)scrollToLatest(true);
  else if(el){
    // Appending at the end must not pull someone reading old messages downwards.
    const retained=Array.from(el.querySelectorAll('[data-message-id]')).find(node=>node.dataset.messageId===anchorId);
    el.scrollTop=retained?el.scrollTop+retained.getBoundingClientRect().top-anchorTop:oldTop;
    followLatest.value=false;
  }
  historyLoaded=true;
};
const scheduleSync=(wait=3000)=>{clearTimeout(syncTimer);if(pageActive&&!document.hidden)syncTimer=setTimeout(refreshMessages,wait);};
const refreshMessages=async()=>{
  if(!pageActive||document.hidden)return;
  if(syncRequest||historyRequest||clearing.value||confirmingId.value){scheduleSync();return;}
  syncRequest=true;const version=historyVersion;
  try{
    const data=(await syncMessages(syncRevision)).data.data||{};
    if(version!==historyVersion||!pageActive||document.hidden)return;
    if(Array.isArray(data.messages)){
      await applyHistory(data.messages);historyError.value=false;
      window.dispatchEvent(new Event('smart-reminder:badge-refresh'));
    }
    syncRevision=data.revision||'';syncFailures=0;
  }catch{syncFailures=Math.min(syncFailures+1,4);}
  finally{syncRequest=false;scheduleSync(Math.min(30000,3000*2**syncFailures));}
};
const resumeSync=()=>{if(pageActive&&!document.hidden)scheduleSync(0);else clearTimeout(syncTimer);};
onMounted(()=>window.addEventListener('smart-reminder:push-refresh',resumeSync));
onBeforeUnmount(()=>window.removeEventListener('smart-reminder:push-refresh',resumeSync));
const activate=()=>{pageActive=true;void resumePending();load();scheduleSync();};
const load=(force=false)=>{
  if(clearing.value)return Promise.resolve();
  if(thinking.value&&!force)return Promise.resolve();
  if(historyRequest)return historyRequest;
  historyLoading.value=!historyLoaded;historyError.value=false;
  const version=historyVersion;
  historyRequest=(async()=>{
    try{
      const res=await getMessages(150);
      if(version!==historyVersion)return;
      // A forced reload is used only after the stream has completed.
      await applyHistory(res.data.data||[],force);
      syncRevision='';
    }catch{if(version===historyVersion&&!historyLoaded)historyError.value=true;}
    finally{if(version===historyVersion){historyLoading.value=false;historyRequest=null;}}
  })();
  return historyRequest;
};
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
const send=async()=>{
  if((!text.value.trim()&&!files.value.length)||thinking.value||clearing.value||clearDialog.value||voiceBusy.value||files.value.some(file=>file.status!=='ready'))return;
  if(pendingChat()){void resumePending();ElMessage.info('请先确认上一条消息的处理结果');return;}
  const content=text.value.trim(),fileIds=files.value.map(x=>x.id),now=Date.now();
  historyVersion++;historyRequest=null;historyLoading.value=false;
  const submittedFiles=[...files.value];text.value='';files.value=[];thinking.value=true;
  stopped=false;abortController=new AbortController();activeRequest=Array.from(crypto.getRandomValues(new Uint8Array(16)),n=>n.toString(16).padStart(2,'0')).join('');
	const assistant=reactive({id:`stream-${now}`,messageRole:'assistant',messageType:'TEXT',content:'',reasoningContent:'',thinkingOpen:true,streaming:true});
  messages.value.push({id:`user-${now}`,messageRole:'user',messageType:'TEXT',content,payload:{fileNames:submittedFiles.map(file=>file.name)}},assistant);
  await nextTick();scrollToLatest(true);
  try{
    const request=await rememberPendingChat({content,fileIds,requestId:activeRequest,fileNames:submittedFiles.map(file=>file.name)});
    const result=await followChatJob(request,{signal:abortController.signal,onReasoning:value=>{assistant.reasoningContent=value;nextTick(()=>scrollToLatest());},onWaiting:value=>{recoveryNotice.value=value;}});
    assistant.content=result?.reply||'已处理';assistant.thinkingOpen=false;recoveryNotice.value='';
    assistant.streaming=false;
    window.dispatchEvent(new Event('smart-reminder:badge-refresh'));
    // History enriches cards in the background; it must not lock the composer.
    void load(true);
    submittedFiles.forEach(file=>{if(file.previewUrl)URL.revokeObjectURL(file.previewUrl);});
  }catch(e){
    while(stopping.value)await delay(20);
    assistant.streaming=false;
    if(e.pending||disposed){recoveryNotice.value=e.message;assistant.content='正在等待处理结果，返回 App 后将继续查询。';return;}
    recoveryNotice.value='';
    if(stopped){assistant.content+=(assistant.content?'\n':'') +mt('已停止生成');assistant.thinkingOpen=false;submittedFiles.forEach(file=>{if(file.previewUrl)URL.revokeObjectURL(file.previewUrl);});return;}
    if(!assistant.content)messages.value=messages.value.filter(x=>x!==assistant);
    ElMessage.error(mt(mobileError(e,'发送未完成，请检查事件状态后重试')));
    if(!text.value)text.value=content;
    files.value.push(...submittedFiles);
  }finally{thinking.value=false;activeRequest=null;abortController=null;syncRevision='';scheduleSync(0);}
};
const resumePending=async()=>{
  const request=pendingChat();if(!request||thinking.value||disposed)return;
  thinking.value=true;activeRequest=request.requestId;abortController=new AbortController();recoveryNotice.value='正在恢复上一条消息的处理结果…';
  try{const result=await followChatJob(request,{signal:abortController.signal,onWaiting:v=>{recoveryNotice.value=v;}});recoveryNotice.value='';messages.value.push({id:'recovered-'+request.requestId,messageRole:'assistant',messageType:'TEXT',content:result?.reply||'已处理',streaming:false});void load(true);window.dispatchEvent(new Event('smart-reminder:badge-refresh'));}
  catch(e){recoveryNotice.value=e.message;if(e.definite){recoveryNotice.value='';ElMessage.info(e.message);}}
  finally{thinking.value=false;activeRequest=null;abortController=null;scheduleSync(0);}
};
const dismissPending=async()=>{const request=pendingChat();if(!request)return;try{await ElMessageBox.confirm('这只结束本机等待，不会取消服务器任务。请先核对事件，避免重复发送。','结束等待？',{confirmButtonText:'已核对，结束等待',cancelButtonText:'继续等待',closeOnClickModal:false});}catch{return;}clearPendingChat(request.requestId);recoveryNotice.value='';void load(true);};
const uploadOne=async item=>{item.status='uploading';try{const res=await uploadFile(item.raw);if(res.data.data.extractStatus!=='SUCCESS')throw new Error(res.data.data.extractMessage||'解析失败');Object.assign(item,res.data.data,{status:'ready'});}catch(error){item.status='error';ElMessage.error(error.message||'附件解析失败');}};
const addFiles=selected=>{for(const raw of selected){if(files.value.length>=6){ElMessage.warning(mt('每条消息最多附加6个文件'));break;}const ext=raw.name.split('.').pop().toLowerCase(),isImage=['png','jpg','jpeg','gif','webp'].includes(ext);if(!['png','jpg','jpeg','gif','webp','pdf','doc','docx','txt','md','xlsx'].includes(ext)){ElMessage.warning(mt('暂不支持该文件格式'));continue;}if(raw.size>(isImage?8:20)*1024*1024){ElMessage.warning(isImage?'图片最大8MB':'文档最大20MB');continue;}const item=reactive({key:Date.now().toString(36)+Math.random().toString(36).slice(2),name:raw.name,raw,status:'uploading',previewUrl:isImage?URL.createObjectURL(raw):''});files.value.push(item);uploadOne(item);}};
const upload=e=>{addFiles(Array.from(e.target.files||[]));e.target.value='';};
const pasteFiles=e=>{const images=Array.from(e.clipboardData?.files||[]).filter(file=>file.type.startsWith('image/'));if(images.length){e.preventDefault();addFiles(images);}};
const removeFile=key=>{const file=files.value.find(item=>item.key===key);if(file?.previewUrl)URL.revokeObjectURL(file.previewUrl);files.value=files.value.filter(item=>item.key!==key);};
const alarmChoices=reactive({});
const alarmKey=(item,index)=>String(payload(item).candidateId)+':'+index;
const wantsAlarm=(item,index,event)=>alarmChoices[alarmKey(item,index)]??(event.alarmRequested===true);
const confirm=async(id,item)=>{
  const key=String(id||'');if(!key||confirmingId.value||item.messageType!=='CANDIDATE')return;
  confirmingId.value=key;
  const choices=(payload(item).events||[]).map((event,index)=>wantsAlarm(item,index,event));
  try{
    const res=await confirmCandidate(id,hasScheduleConflict(item));
    if(res.data.data?.confirmationRequired){ElMessage.warning(mt('发现新的日程冲突，请查看后确认是否仍要安排'));await load();return;}
    item.messageType='CANDIDATE_CONFIRMED';
    window.dispatchEvent(new Event('smart-reminder:badge-refresh'));
    ElMessage.success(mt('智能提醒已创建'));
    const ids=res.data.data?.eventIds||[];
    for(let index=0;index<choices.length;index++){
      if(!choices[index])continue;
      try{
        if(!ids[index])throw Error('未返回事件编号，请到事件详情设置闹铃');
        await setEventAlarm((await getEventDetail(String(ids[index]))).data.data);
        ElMessage.success('本机系统闹铃已设置，可在事件详情取消');
      }catch(e){ElMessage.warning({message:'事件已创建，但闹铃未设置：'+(e?.message||'请到事件详情重试'),duration:6000});}
    }
    await load();
  }catch(e){await load();ElMessage.warning(e?.response?.data?.msg||mt('操作卡状态已变化，请刷新后重试'));}
  finally{confirmingId.value='';}
};
const openEvent=item=>{if(item.eventId)router.push('/app/event/'+item.eventId);};
onMounted(()=>{activate();resizeInput();composerObserver=new ResizeObserver(()=>{if(pageActive){scrollToLatest();lastMessagesHeight=scroll.value?.clientHeight||0;}});if(composer.value)composerObserver.observe(composer.value);if(scroll.value)composerObserver.observe(scroll.value);document.addEventListener('visibilitychange',resumeSync);window.addEventListener('online',resumeSync);window.addEventListener('native:keyboard-layout',keyboardLayout);window.visualViewport?.addEventListener('resize',keyboardLayout);});
onActivated(activate);
onDeactivated(()=>{pageActive=false;drawerOpen.value=false;stopLayoutFollow();historyVersion++;historyRequest=null;clearTimeout(syncTimer);});
onBeforeUnmount(()=>{disposed=true;pageActive=false;stopLayoutFollow();historyVersion++;clearTimeout(syncTimer);document.removeEventListener('visibilitychange',resumeSync);window.removeEventListener('online',resumeSync);window.removeEventListener('native:keyboard-layout',keyboardLayout);window.visualViewport?.removeEventListener('resize',keyboardLayout);abortController?.abort();composerObserver?.disconnect();files.value.forEach(file=>{if(file.previewUrl)URL.revokeObjectURL(file.previewUrl);});});
</script>

<style scoped>
.alarm-choice{display:flex;align-items:center;gap:7px;margin-top:10px;font-size:12px;color:#5e6ca4}.alarm-choice input{width:17px;height:17px;accent-color:#576dff}.alarm-hint{display:block;font-size:11px;color:#8d97b0;margin-top:5px;line-height:1.6}
.chat-history{position:relative;display:flex;flex-direction:column;flex:1;min-height:0;overflow:hidden}
.back-to-latest{position:absolute;right:16px;bottom:12px;z-index:3;display:flex;align-items:center;gap:5px;min-height:36px;padding:7px 12px;border:1px solid #e1e9fa;border-radius:20px;background:#fffffff5;color:#5869b0;font-size:12px;box-shadow:0 4px 18px #5675ac25;cursor:pointer}
.back-to-latest .ui-icon{width:16px;height:16px}.back-to-latest:focus-visible,.clear-context-button:focus-visible{outline:2px solid #6476ff;outline-offset:3px}.clear-context-button:disabled{opacity:.5;cursor:wait}
.clear-context-dialog p{font-size:14px;line-height:1.75;margin:0;color:#47546e}.clear-context-dialog .clear-retained{margin-top:12px;padding:12px;background:#f3f6ff;border-radius:12px;color:#677798;font-size:13px}
.clear-cancel{min-height:40px;padding:0 20px;border:1px solid #e5eaf4;border-radius:12px;background:#f7f9ff;color:#71809a;margin-right:10px;cursor:pointer}
.notify{--notice-bg:#fff3dc;--notice-accent:#d98300;--notice-border:#f3dfb8;--notice-fill:#fffaf0}
.notify.card-feedback{--notice-bg:#e7f8f2;--notice-accent:#15966c;--notice-border:#cde9df;--notice-fill:#f4fbf8}
.notify.card-conflict{--notice-bg:#fff0ef;--notice-accent:#dc4f49;--notice-border:#f0d9d6;--notice-fill:#fff8f7}
.notify.card-exception{--notice-bg:#fff0ef;--notice-accent:#d45c4c;--notice-border:#efd9d5;--notice-fill:#fff8f6;background:linear-gradient(145deg,#fff,#fff7f4);border-color:#f0ded6}
.notify .card-heading,.candidate .card-heading{display:inline-flex;gap:0;border-radius:14px;background:var(--notice-bg,#edf2ff);padding:0 9px 0 3px;min-height:27px}
.notify .card-heading .card-icon,.candidate .card-heading .card-icon{width:24px;height:25px;background:transparent;border-radius:0;color:var(--notice-accent,#315bff)}
.notify .card-heading .sr-chip,.candidate .card-heading .sr-chip{padding:2px 3px;background:transparent;color:var(--notice-accent,#315bff);font-size:11px}
.candidate.macro .card-heading{--notice-bg:#f0e9ff;--notice-accent:#7953d6}
.notify .notification-focus{padding:12px 13px;border:1px solid var(--notice-border);border-radius:14px;background:var(--notice-fill)}
.notify .notification-focus .notification-content{font-size:15px;line-height:1.65;font-weight:700;color:#1c2941}
.notify .notification-focus .notification-event{margin:10px 0 0;padding-top:9px;border-top:1px dashed var(--notice-border);color:#778197;font-size:12px;line-height:1.6}
.notify .notification-focus .notification-event span{color:var(--notice-accent);font-weight:700}
.notification-focus{padding:3px 1px 0;overflow-wrap:anywhere}
.notification-focus .notification-content{margin:0;color:#243149;font-size:16px;font-weight:600;line-height:1.75;white-space:pre-wrap}
.notification-focus .notification-actor{margin:9px 0 0;color:#698477;font-size:12px;line-height:1.6}
.notification-focus .notification-event{margin:12px 0 0;padding-top:10px;border-top:1px solid #e5ebe9;color:#7b8496;font-size:12px;font-weight:400;line-height:1.6}
.notification-event span{margin-right:7px;color:#8993a3}
.card-conflict .notification-event{border-top-color:#f0e4e2}
.avatar-initial{display:grid;place-items:center;background:#e8edff;color:#315bff;font-size:14px;font-weight:700}.sent-files{display:block;opacity:.8;font-size:11px;margin-top:6px}.file-strip span>img{width:34px;height:34px;border-radius:7px;object-fit:cover}.file-strip .failed{background:#fff1ee;color:#ba5146}.file-strip small{font-size:10px}.composer .composer-box textarea{max-height:150px;overflow-y:hidden;box-sizing:border-box}.composer .file-strip{max-height:90px}.message{animation:message-in .22s ease-out}@keyframes message-in{from{opacity:0;translate:0 5px}to{opacity:1;translate:0 0}}@media(prefers-reduced-motion:reduce){.message{animation:none}}
.schedule-warning{padding:10px;border-radius:10px;background:#fff2dd;color:#a36a16;font-size:12px;line-height:1.6}
.message{position:relative}.message.assistant{padding-left:37px}.message.user{padding-right:37px}.chat-avatar{position:absolute;left:0;top:3px;width:29px;height:29px;object-fit:cover;border-radius:10px}.message.user .chat-avatar{left:auto;right:0}.message .bubble{max-width:94%}
.assistant .bubble{white-space:normal}.markdown-content{overflow-wrap:anywhere}.markdown-content :deep(p){margin:.2em 0 .7em}.markdown-content :deep(:first-child){margin-top:0}.markdown-content :deep(:last-child){margin-bottom:0}.markdown-content :deep(pre){white-space:pre;max-width:100%;overflow:auto}
.event-text-link{border:0;background:none;padding:0;color:#7184a5;font:inherit;font-size:12px;cursor:pointer}.assigned-message .card-footer{margin-top:12px}
.messages{padding-bottom:104px}.message{display:flex;margin:10px 0}.message.user{justify-content:flex-end}.bubble{box-sizing:border-box;max-width:82%;padding:12px 15px;border-radius:17px;background:#fff;line-height:1.65;white-space:pre-wrap;box-shadow:0 3px 14px #1c2b5210}.user .bubble{background:linear-gradient(135deg,#315bff,#5f7dff);color:#fff;border-bottom-right-radius:5px}.assistant .bubble{border-bottom-left-radius:5px}.candidate-stack{display:flex;flex-direction:column;align-items:flex-start;width:94%;max-width:620px}.candidate-reply{margin-bottom:8px;max-width:88%}.candidate{width:100%}.notify{width:auto;min-width:250px;max-width:90%}.candidate,.notify{cursor:pointer;overflow:hidden;padding:13px 14px;border:1px solid #e8edf5;box-shadow:0 8px 24px rgba(35,65,110,.07)}.card-heading{display:flex;align-items:center;gap:7px;margin-bottom:9px}.card-heading .sr-chip{margin:0}.card-icon{width:27px;height:27px;border-radius:10px;display:inline-flex;align-items:center;justify-content:center;background:#edf2ff;color:#315bff;font-weight:800}.candidate.macro{background:linear-gradient(145deg,#fff,#faf8ff);border-color:#ebe4fb}.candidate.macro .card-icon,.candidate.macro .sr-chip{background:#f0e9ff;color:#7953d6}.candidate-event{padding:10px 11px;margin:9px 0;border:1px solid #edf1f7;border-radius:13px;background:#f8faff}.candidate-event.macro{border-color:#eee7fb;background:#faf7ff;padding-right:11px}.candidate-event b{font-size:15px}.candidate-event p{margin:2px 0}.candidate button{width:100%;margin-top:3px;min-height:38px}.candidate.macro button:not(.confirmed){background:linear-gradient(135deg,#7658d5,#9b7cec)}.candidate button.confirmed{background:#e9edf5;color:#7e8799;box-shadow:none}.notify .card-content{line-height:1.55;color:#263149}.notify h3{margin-bottom:5px}.notify .card-link{margin:9px 2px 1px;color:#7d8699}.card-meta{margin-top:2px!important}.card-reminder{background:linear-gradient(145deg,#fff,#fffaf2);border-color:#f7e7ca}.card-reminder .card-icon,.card-reminder .sr-chip{background:#fff3dc;color:#d98300}.card-feedback{max-width:92%;background:linear-gradient(145deg,#fff,#f5fcfa);border-color:#dcefe9}.card-feedback .card-icon,.card-feedback .sr-chip{background:#e7f8f2;color:#15966c}.card-event{background:linear-gradient(145deg,#fff,#f7f9ff);border-color:#e2e8fb}.card-conflict{background:linear-gradient(145deg,#fff,#fff7f6);border-color:#f4dfdd}.card-conflict .card-icon,.card-conflict .sr-chip{background:#fff0ef;color:#dc4f49}.card-detail{margin-top:8px;padding:9px 10px;border-radius:11px;background:#f1faf7;display:flex;flex-direction:column;gap:3px}.card-detail span{font-size:12px;color:#6e8f83}.card-detail b{font-size:14px;color:#193d31;line-height:1.5}
.message.assistant{flex-direction:column;align-items:flex-start}.has-thinking{min-width:0}.message.assistant>.bubble.has-thinking{width:94%}.candidate-stack>.bubble.has-thinking{width:100%;max-width:100%}.thinking-panel+.markdown-content{margin-top:10px;padding-top:10px;border-top:1px solid #edf0f5}.notify>.thinking-panel{margin-bottom:11px;padding-bottom:10px;border-bottom:1px solid #edf0f5}
.composer{position:fixed;z-index:8;bottom:68px;left:50%;transform:translateX(-50%);width:min(100%,760px);background:#fff;border-top:1px solid #edf0f5;padding:9px 12px 11px}.composer-box{display:flex;gap:5px;align-items:flex-end;min-height:48px;padding:4px 5px;border:1px solid #dfe4ee;background:#f7f9fc;border-radius:20px;box-shadow:0 4px 16px rgba(26,40,76,.05);transition:.2s}.composer-box:focus-within{border-color:#8da3ff;background:#fff;box-shadow:0 0 0 3px #edf1ff}.composer textarea{flex:1;min-width:0;min-height:40px;max-height:100px;resize:none;border:0;background:transparent;border-radius:0;padding:9px 4px;font:inherit;line-height:22px;outline:none}.send-button{flex:0 0 auto;border:0;background:#315bff;color:#fff;border-radius:15px;height:40px;padding:0 16px;font-weight:600}.send-button:disabled{opacity:.55}.upload{flex:0 0 40px;width:40px;height:40px;display:flex;align-items:center;justify-content:center;color:#64718a;border-radius:14px;cursor:pointer;transition:.2s}.upload:hover{color:#315bff;background:#eaf0ff}.upload svg{width:22px;height:22px;fill:none;stroke:currentColor;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}.upload input{display:none}
.file-strip{display:flex;gap:6px;overflow-x:auto;padding:0 2px 7px}.file-strip span{display:inline-flex;align-items:center;gap:5px;max-width:100%;background:#eef2ff;color:#315bff;border:1px solid #dce4ff;border-radius:10px;padding:5px 7px;font-size:12px;white-space:nowrap}.file-strip span>svg{width:15px;height:15px;fill:none;stroke:currentColor;stroke-width:1.8}.file-strip span>button{border:0;background:transparent;color:#7f8aaa;padding:0 2px;font-size:16px;line-height:1;cursor:pointer}.dots{display:inline-flex;gap:5px;padding:4px}.dots i{width:6px;height:6px;background:#8993a8;border-radius:50%;animation:b 1s infinite alternate}.dots i:nth-child(2){animation-delay:.2s}.dots i:nth-child(3){animation-delay:.4s}.streaming-cursor{display:inline-block;width:2px;height:1.1em;margin-left:3px;background:#315bff;vertical-align:-2px;animation:cursor .8s infinite}.is-streaming{min-height:24px}@keyframes cursor{50%{opacity:0}}@keyframes b{to{opacity:.25;transform:translateY(-3px)}}
.structured-event{margin:7px 0 2px;padding:11px 12px;border:1px solid #edf1f6;border-radius:13px;background:#f8fafd}.structured-event p{white-space:pre-wrap;line-height:1.5}.card-reminder .structured-event{background:#fffcf7;border-color:#f7ead3}.card-feedback .structured-event{background:#f5fbf9;border-color:#e0f0eb}.card-event .structured-event{background:#f7f9ff;border-color:#e5eafd}.conflict-event{background:#fff8f7;border-color:#f3e2e0}
.markdown-body{white-space:normal;overflow-wrap:anywhere}.markdown-content>:first-child,.candidate-reply>:first-child{margin-top:0}.markdown-content>:last-child,.candidate-reply>:last-child{margin-bottom:0}.markdown-content p,.candidate-reply p{margin:.2em 0 .7em}.markdown-content h1,.markdown-content h2,.markdown-content h3,.markdown-content h4,.candidate-reply h1,.candidate-reply h2,.candidate-reply h3,.candidate-reply h4{margin:.65em 0 .35em;line-height:1.35}.markdown-content h1,.candidate-reply h1{font-size:1.3em}.markdown-content h2,.candidate-reply h2{font-size:1.18em}.markdown-content h3,.candidate-reply h3{font-size:1.08em}.markdown-content ul,.markdown-content ol,.candidate-reply ul,.candidate-reply ol{margin:.35em 0 .75em;padding-left:1.35em}.markdown-content li+li,.candidate-reply li+li{margin-top:.22em}.markdown-content blockquote,.candidate-reply blockquote{margin:.6em 0;padding:.45em .8em;border-radius:9px;background:#f2f5fb;color:#59667e}.markdown-content code,.candidate-reply code{padding:.12em .35em;border-radius:5px;background:#eef1f6;font-family:Consolas,monospace;font-size:.9em}.markdown-content pre,.candidate-reply pre{max-width:100%;overflow:auto;margin:.7em 0;padding:11px;border-radius:10px;background:#202638;color:#eef3ff}.markdown-content pre code,.candidate-reply pre code{padding:0;background:transparent;color:inherit}.markdown-content table,.candidate-reply table{display:block;max-width:100%;overflow-x:auto;border-collapse:collapse;margin:.65em 0}.markdown-content th,.markdown-content td,.candidate-reply th,.candidate-reply td{padding:6px 8px;border:1px solid #dfe5ef;white-space:nowrap}.markdown-content a,.candidate-reply a{color:#315bff;text-decoration:none}.markdown-content hr,.candidate-reply hr{border:0;border-top:1px solid #e3e7ef;margin:.8em 0}
.reminder-focus{padding:12px 13px;border:1px solid #f3dfb8;border-radius:14px;background:linear-gradient(145deg,#fffdfa,#fff8ea)}.reminder-focus>strong{display:block;color:#1c2941;font-size:15px;line-height:1.65;white-space:pre-wrap}.reminder-focus>p{margin:10px 0 0;padding-top:9px;border-top:1px dashed #efd9ad;color:#778197;font-size:12px}.reminder-focus>p span{margin-right:7px;color:#c67600;font-weight:700}
.card-footer{display:flex;align-items:center;justify-content:space-between;gap:14px;margin-top:9px}.card-footer .card-link{margin:0}.card-footer time{margin-left:auto;color:#a1a9b8;font-size:11px;white-space:nowrap}
</style>
<style>
.clear-context-dialog.el-dialog{border-radius:22px;padding:22px;box-shadow:0 20px 70px #42537833}.clear-context-dialog .el-dialog__title{font-size:17px;font-weight:650;color:#293958}.clear-context-dialog .el-dialog__header{padding-bottom:16px}.clear-context-dialog .el-dialog__footer{padding-top:20px}.clear-context-dialog .sr-button{border:0;border-radius:12px;min-height:40px;padding:0 20px;background:#4d65ff;color:#fff;cursor:pointer}.clear-context-dialog button:disabled{opacity:.55;cursor:wait}
</style>
