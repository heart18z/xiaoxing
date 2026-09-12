<template>
  <div class="reply-progress" role="status" aria-live="polite">
    <span class="progress-orbit" aria-hidden="true"></span>
    <Transition name="stage" mode="out-in"><span :key="stage">{{ mt(stages[stage]) }}</span></Transition>
    <span class="progress-shimmer" aria-hidden="true"></span>
  </div>
</template>
<script setup>
import {mt} from './mobileLocale';
import { onMounted,onUnmounted,ref } from 'vue';
const stages=['分析中','思考中','规划中','整理回复中'],stage=ref(0);
let timer;
onMounted(()=>{timer=setInterval(()=>{stage.value=Math.min(stage.value+1,stages.length-1);},2800);});
onUnmounted(()=>clearInterval(timer));
</script>
<style scoped>
.reply-progress{position:relative;display:flex;align-items:center;gap:10px;min-width:150px;overflow:hidden;padding:5px 0;color:#667b9d;font-size:13px}.progress-orbit{width:16px;height:16px;border:2px solid #dce6ff;border-top-color:#537cff;border-right-color:#42c1d8;border-radius:50%;animation:orbit 1s linear infinite}.progress-shimmer{position:absolute;inset:0;transform:translateX(-100%);background:linear-gradient(100deg,transparent,#fff9,transparent);animation:shimmer 2.4s ease-in-out infinite;pointer-events:none}.stage-enter-active,.stage-leave-active{transition:opacity .2s,transform .2s}.stage-enter-from{opacity:0;transform:translateY(4px)}.stage-leave-to{opacity:0;transform:translateY(-4px)}@keyframes orbit{to{transform:rotate(360deg)}}@keyframes shimmer{to{transform:translateX(100%)}}@media(prefers-reduced-motion:reduce){.progress-orbit,.progress-shimmer{animation:none}}
</style>
