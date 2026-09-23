<template>
  <div v-if="isImage" class="sent-image">
    <el-image v-if="source" :src="source" :preview-src-list="[source]" fit="contain" preview-teleported />
    <button v-else type="button" @click="load" :disabled="loading">{{ failed ? '图片加载失败，点击重试' : '图片加载中…' }}</button>
  </div>
  <small v-else>{{ name }}</small>
</template>
<script setup>
import {computed,ref,watch,onBeforeUnmount} from 'vue';
import {getFilePreview} from '@/api/smartReminder';
import store from '@/store';
const props=defineProps({fileId:String,name:String});
const source=ref(''),failed=ref(false),loading=ref(false);
const isImage=computed(()=>/\.(png|jpe?g|gif|webp)$/i.test(props.name||''));
const owner=computed(()=>String(store.getters.userInfo?.user_id||store.getters.userInfo?.userId||''));
let generation=0;
async function load(){const run=++generation;if(!isImage.value)return;loading.value=true;failed.value=false;try{const res=await getFilePreview(props.fileId);if(run===generation){source.value=res.data.data.image;failed.value=!source.value;}}catch(_){if(run===generation)failed.value=true;}finally{if(run===generation)loading.value=false;}}
watch(()=>[props.fileId,owner.value],()=>{source.value='';load();},{immediate:true});
onBeforeUnmount(()=>{generation++;});
</script>
<style scoped>
.sent-image{width:210px;max-width:100%;height:156px;margin-top:8px;border-radius:12px;overflow:hidden;background:#eef1ff;display:flex;align-items:center;justify-content:center;}
.sent-image :deep(.el-image){width:100%;height:100%;}
button{border:0;background:none;color:#526085;font-size:12px;cursor:pointer;}
</style>
