<template>
  <el-dialog :model-value="open" @update:model-value="$emit('close')" :title="title" width="min(92vw,430px)" append-to-body class="avatar-picker">
    <p class="picker-tip">{{ mt("选择默认头像，或上传自己的图片") }}</p>
    <div class="avatar-grid"><button v-if="kind==='user'" type="button" :disabled="busy" :class="{selected:!value}" @click="$emit('choose','')" :aria-label="mt('使用名字首字')"><b>{{ initial||mt('我') }}</b></button><button v-for="item in presets" :key="item" type="button" :class="{selected:value===item}" :disabled="busy" @click="$emit('choose',item)" :aria-label="mt('选择头像 ')+item.split('/').pop()"><img :src="item" :alt="mt('默认头像')"/><span v-if="value===item">✓</span></button></div>
    <label :class="['avatar-upload',{busy}]"><span v-if="busy" class="upload-spinner"></span>{{ busy?mt('正在保存…'):mt('上传图片') }}<input type="file" accept="image/jpeg,image/png,image/webp,image/gif" :disabled="busy" @change="upload" /></label>
    <small>{{ mt("支持 JPG、PNG、WEBP、GIF，最大 2MB") }}</small>
  </el-dialog>
</template>
<script setup>
import {mt} from './mobileLocale';
import {computed} from 'vue';
const props=defineProps({open:Boolean,title:String,value:String,busy:Boolean,kind:String,initial:String});const emit=defineEmits(['close','choose','upload']);
const presets=computed(()=>props.kind==='ai'?[1,2,3,4,5,6,7,8,9,10,11,13].map(i=>`/avatars/assistant/A${i}.png`):Array.from({length:13},(_,i)=>`/avatars/user/B${i+1}.png`));
const upload=event=>{const file=event.target.files?.[0];event.target.value='';if(file)emit('upload',file);};
</script>
<style scoped>
.avatar-grid b{font-size:27px;color:#4168e3}:global(.avatar-picker.el-dialog){width:min(calc(100vw - 40px),430px)!important;border-radius:24px;padding:20px;max-height:calc(100dvh - 64px);overflow:auto;margin-top:max(32px,8vh)!important;box-sizing:border-box}:global(.avatar-picker .el-dialog__header){padding:0 24px 15px 0;margin:0}:global(.avatar-picker .el-dialog__title){font-size:17px;font-weight:700;color:#263653}:global(.avatar-picker .el-dialog__body){padding:0}.avatar-grid{max-height:48dvh;overflow:auto;padding:2px}
.picker-tip{margin:0 0 15px;color:#8190a6;font-size:13px}.avatar-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:11px}.avatar-grid button{position:relative;border:2px solid transparent;padding:3px;background:#f5f8ff;border-radius:18px;cursor:pointer;aspect-ratio:1}.avatar-grid button.selected{border-color:#4771ff;background:#ecf1ff}.avatar-grid img{width:100%;height:100%;display:block;border-radius:13px}.avatar-grid span{position:absolute;bottom:0;right:0;background:#4771ff;color:#fff;border-radius:50%;width:18px;height:18px}.avatar-upload{display:flex;justify-content:center;align-items:center;gap:8px;margin-top:20px;padding:12px;border-radius:14px;background:#edf2ff;color:#4168e3;cursor:pointer;font-weight:600}.avatar-upload input{display:none}.avatar-upload.busy{opacity:.6;pointer-events:none}small{display:block;text-align:center;margin-top:8px;color:#9aa4b4}.upload-spinner{width:16px;height:16px;border:2px solid #c7d5ff;border-top-color:#4168e3;border-radius:50%;animation:spin .8s linear infinite}@keyframes spin{to{transform:rotate(360deg)}}
</style>
