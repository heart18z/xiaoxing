<template>
  <span class="attachment-inline" :class="'tone-' + tone">
    <span v-if="content">{{ content }}</span><button v-if="ids?.length" class="attachment-link" type="button" @click.stop="openList">{{ mt('查看附件') }}</button>
    <el-dialog v-model="opened" :title="mt('查看附件')" width="min(420px, 92vw)" align-center append-to-body class="attachment-gallery" @close="clear">
      <div class="gallery-body" @click.stop>
        <div v-if="loading" class="gallery-empty">{{ mt('附件加载中…') }}</div>
        <div v-else-if="!files.length" class="gallery-empty">{{ error || mt('暂无附件') }}<button v-if="error" @click="openList">{{ mt('重试') }}</button></div>
        <template v-else>
          <div class="photo-stack" :class="{multiple:files.length > 1}" @keydown.left.prevent="move(-1)" @keydown.right.prevent="move(1)" tabindex="0" :aria-label="mt('附件') + ' ' + (active+1) + ' / ' + files.length">
            <div v-if="files.length > 1" class="stack-back back-far"></div><div v-if="files.length > 1" class="stack-back back-near"></div>
            <div class="photo-face" @pointerdown="touchStart" @pointerup="touchEnd" @pointercancel="touchCancel" @dragstart.prevent @click="enlarge">
              <div v-if="reading" class="gallery-empty">{{ mt('附件加载中…') }}</div>
              <div v-else-if="error" class="gallery-empty" role="alert">{{ error }}<button @click="loadCurrent">{{ mt('重试') }}</button></div>
              <img v-else-if="preview.image" :src="preview.image" :alt="selected.name" draggable="false" />
              <pre v-else class="document-preview">{{ preview.text || mt('暂无文字预览，可下载原文件查看') }}</pre>
              <span v-if="files.length > 1" class="image-count">{{ active+1 }} / {{ files.length }}</span>
            </div>
          </div>
          <div class="gallery-caption"><b>{{ selected.name }}</b><span>{{ mt(selected.kind === 'FEEDBACK' ? '反馈附件' : '发送附件') }}<template v-if="preview.image"> · {{ mt('点击图片放大') }}</template></span></div>
          <div v-if="files.length > 1" class="gallery-navigation">
            <button :disabled="active === 0" :aria-label="mt('上一张')" @click="move(-1)">‹</button>
            <div class="page-dots"><span v-for="(_,i) in files" :key="i" :class="{active:i===active}"></span></div>
            <button :disabled="active === files.length-1" :aria-label="mt('下一张')" @click="move(1)">›</button>
          </div>
          <p v-if="files.length > 1" class="swipe-hint">{{ mt('左右滑动查看附件') }}</p>
          <div class="download-row"><button class="download-button" :disabled="downloading || reading || !!error" @click="download">{{ mt(downloading ? '正在下载…' : preview.image ? '下载图片' : '下载原文件') }}</button></div>
        </template>
      </div>
    </el-dialog>
    <ElImageViewer v-if="zoom && preview.image" :url-list="[preview.image]" teleported @close="zoom=false" />
  </span>
</template>
<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue';
import { ElImageViewer } from 'element-plus';
import { getEventAttachments, getAttachmentContent, downloadEventAttachment } from '@/api/smartReminder';
import { mt } from './mobileLocale';
import store from '@/store';
const props = defineProps({ eventId: String, ids: Array, content: {type:String,default:''}, tone: {type:String,default:'blue'} });
const opened=ref(false), loading=ref(false), reading=ref(false), downloading=ref(false), zoom=ref(false);
const files=ref([]), active=ref(0), error=ref(''), preview=ref({});
const selected=computed(()=>files.value[active.value] || {});
let generation=0, selection=0, pointer=null, suppressClickUntil=0;
function clear(){generation++;selection++;zoom.value=false;files.value=[];preview.value={};error.value='';pointer=null;loading.value=reading.value=downloading.value=false;}
async function openList(){
  if(loading.value || !props.ids?.length)return;
  opened.value=true;loading.value=true;error.value='';const run=++generation;
  try{
    const {data}=await getEventAttachments(props.eventId);
    if(run!==generation)return;
    const seen=new Set();files.value=(data.data||[]).filter(f=>props.ids.map(String).includes(String(f.id))&&!seen.has(String(f.id))&&seen.add(String(f.id)));
    active.value=0;if(files.value.length)await loadCurrent();
  }catch(e){if(run===generation)error.value=e.message||mt('附件读取失败，请稍后重试');}
  finally{if(run===generation)loading.value=false;}
}
async function loadCurrent(){
  if(!selected.value.id)return;
  const run=++selection,page=generation,id=selected.value.id;reading.value=true;error.value='';preview.value={};zoom.value=false;
  try{const {data}=await getAttachmentContent(props.eventId,id);if(run===selection&&page===generation)preview.value=data.data;}
  catch(e){if(run===selection&&page===generation)error.value=e.message||mt('附件读取失败，请稍后重试');}
  finally{if(run===selection&&page===generation)reading.value=false;}
}
function move(delta){const next=active.value+delta;if(next<0||next>=files.value.length)return;active.value=next;loadCurrent();}
function touchStart(e){if(e.pointerType==='mouse'&&e.button!==0)return;suppressClickUntil=0;pointer={id:e.pointerId,x:e.clientX,y:e.clientY};e.currentTarget.setPointerCapture?.(e.pointerId);}
function touchEnd(e){if(!pointer||e.pointerId!==pointer.id)return;const dx=e.clientX-pointer.x,dy=e.clientY-pointer.y;pointer=null;if(Math.abs(dx)>38&&Math.abs(dx)>Math.abs(dy)*1.3){suppressClickUntil=Date.now()+400;move(dx<0?1:-1);}}
function touchCancel(){pointer=null;}
function enlarge(){if(preview.value.image && Date.now()>suppressClickUntil)zoom.value=true;}
async function download(){
  if(downloading.value||!selected.value.id)return;
  downloading.value=true;const run=generation,file=selected.value;
  try{const {data}=await downloadEventAttachment(props.eventId,file.id);if(run!==generation)return;const url=URL.createObjectURL(data),link=document.createElement('a');link.href=url;link.download=file.name;link.click();setTimeout(()=>URL.revokeObjectURL(url),1000);}
  catch(e){if(run===generation)error.value=e.message||mt('附件读取失败，请稍后重试');}
  finally{if(run===generation)downloading.value=false;}
}
watch(()=>[props.eventId,JSON.stringify(props.ids),store.getters.userInfo?.user_id,store.getters.userInfo?.userId],()=>{opened.value=false;clear();});
onBeforeUnmount(clear);
</script>
<style scoped>
.attachment-inline{display:inline;--attachment-color:#4963df;--attachment-bg:#edf1ff}
.tone-amber{--attachment-color:#bc7b15;--attachment-bg:#fff0cf}.tone-green{--attachment-color:#209775;--attachment-bg:#e1f5ed}.tone-red{--attachment-color:#d35960;--attachment-bg:#ffeded}
.attachment-link{display:inline;vertical-align:baseline;margin:0 0 0 5px;padding:2px 6px;border:0;border-radius:6px;color:var(--attachment-color);background:var(--attachment-bg);font:inherit;font-size:.88em;font-weight:600;line-height:1.5;white-space:nowrap;cursor:pointer}
.gallery-body{color:#27354f;padding:6px 2px 2px}.photo-stack{position:relative;margin:0 4px;isolation:isolate}.photo-stack.multiple{padding-top:16px;margin-top:4px}
.stack-back{position:absolute;left:0;right:0;height:85%;border:1px solid #dfe6f6;border-radius:20px;pointer-events:none}.back-far{top:0;transform:scale(.88);transform-origin:top center;background:#e9eef9;z-index:-2}.back-near{top:8px;transform:scale(.95);transform-origin:top center;background:#f4f6fc;z-index:-1}
.photo-face{position:relative;width:100%;height:min(330px,43vh);overflow:hidden;border-radius:18px;background:#f3f6fb;border:1px solid #e6eaf3;box-shadow:0 8px 22px #263d6b0b;touch-action:pan-y;user-select:none;display:flex;align-items:center;justify-content:center}
.photo-face img{width:100%;height:100%;object-fit:contain;cursor:zoom-in}.image-count{position:absolute;right:12px;top:12px;border:1px solid #ffffff90;background:#ffffffed;border-radius:16px;padding:4px 10px;font-size:12px;font-weight:600;color:#53617c;pointer-events:none}
.gallery-empty{min-height:150px;display:flex;align-items:center;justify-content:center;flex-direction:column;gap:12px;text-align:center;font-size:13px;color:#8290a8}.gallery-empty button{background:none;border:0;color:#4963df;cursor:pointer}
.gallery-caption{text-align:center;margin-top:15px;display:flex;flex-direction:column;gap:6px}.gallery-caption b{font-size:14px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.gallery-caption span{font-size:11px;color:#8b97ab}
.gallery-navigation{display:flex;align-items:center;justify-content:center;gap:22px;margin-top:12px}.gallery-navigation button{width:28px;height:28px;border:0;border-radius:50%;background:#edf1f8;color:#657592;font-size:23px;line-height:24px;cursor:pointer}.gallery-navigation button:disabled{opacity:.3;cursor:default}.page-dots{display:flex;gap:6px}.page-dots span{width:5px;height:5px;border-radius:8px;background:#d8deeb;transition:width .2s}.page-dots span.active{width:17px;background:#6480e8}.swipe-hint{text-align:center;color:#98a3b6;font-size:11px;margin:7px 0 0}
.download-row{display:flex;justify-content:center;margin-top:18px}.download-button{border:1px solid #dae4ff;background:#eef3ff;border-radius:13px;padding:10px 30px;font-family:inherit;font-size:13px;font-weight:600;color:#4d67da;cursor:pointer}.download-button:disabled{opacity:.55}
.document-preview{align-self:stretch;width:100%;padding:18px;margin:0;overflow:auto;white-space:pre-wrap;overflow-wrap:anywhere;font:14px/1.8 sans-serif;user-select:text;box-sizing:border-box}
</style>
<style>
.el-dialog.attachment-gallery{border-radius:26px;padding:20px;background:linear-gradient(155deg,#ffffff 55%,#f8faff);box-shadow:0 22px 70px #20355a2e}.attachment-gallery .el-dialog__header{padding:0 30px 13px 2px;margin:0}.attachment-gallery .el-dialog__title{font-size:18px;font-weight:650;color:#1d2c47}.attachment-gallery .el-dialog__headerbtn{top:12px;right:12px;width:32px;height:32px;border-radius:50%;background:#f0f3f9}.attachment-gallery .el-dialog__body{padding:0}
@media(prefers-reduced-motion:reduce){.attachment-gallery *{transition:none!important}}
</style>
