<template>
  <section class="thinking-panel">
    <button type="button" class="thinking-toggle" :aria-expanded="open" @click="$emit('toggle')">
      <span :class="['thinking-mark',{active:streaming}]" aria-hidden="true">
        <svg viewBox="0 0 24 24"><path d="M12 3.5c.7 4.4 2.1 5.8 6.5 6.5-4.4.7-5.8 2.1-6.5 6.5-.7-4.4-2.1-5.8-6.5-6.5 4.4-.7 5.8-2.1 6.5-6.5Z"/><path d="M18.5 15.5c.3 1.8.9 2.4 2.7 2.7-1.8.3-2.4.9-2.7 2.7-.3-1.8-.9-2.4-2.7-2.7 1.8-.3 2.4-.9 2.7-2.7Z"/></svg>
      </span>
      <span class="thinking-label">{{ streaming ? mt('正在思考…') : mt('AI 思考') }}</span>
      <span class="thinking-action">{{ open ? mt('收起') : mt('查看') }}</span>
      <svg :class="['thinking-chevron',{open}]" viewBox="0 0 20 20" aria-hidden="true"><path d="m6.5 8 3.5 3.5L13.5 8"/></svg>
    </button>
    <div v-show="open" ref="output" class="thinking-content markdown-body" v-html="renderMarkdown(content)"></div>
  </section>
</template>

<script setup>
import {mt} from './mobileLocale';
import { renderMarkdown } from './markdown';
import { nextTick, ref, watch } from 'vue';

const props=defineProps({
  content:{type:String,default:''},
  open:{type:Boolean,default:false},
  streaming:{type:Boolean,default:false}
});
defineEmits(['toggle']);
const output=ref();
watch(()=>[props.content,props.open],async()=>{if(!props.open||!props.streaming)return;await nextTick();if(output.value)output.value.scrollTop=output.value.scrollHeight;},{flush:'post'});
</script>

<style scoped>
.thinking-panel{box-sizing:border-box;width:100%;min-width:0;color:#6d7890}
.thinking-toggle{display:flex;width:100%;min-width:0;align-items:center;gap:7px;padding:1px 0 7px;border:0;border-bottom:1px solid #edf0f6;background:transparent;color:inherit;font:inherit;cursor:pointer;text-align:left}
.thinking-mark{display:inline-flex;flex:0 0 22px;width:22px;height:22px;align-items:center;justify-content:center;border-radius:8px;background:linear-gradient(145deg,#eef2ff,#e3eaff);color:#496cff}
.thinking-mark svg{width:14px;height:14px;fill:none;stroke:currentColor;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round}
.thinking-mark.active{animation:thinkingPulse 1.1s ease-in-out infinite}
.thinking-label{min-width:0;overflow:hidden;color:#596780;font-size:13px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}
.thinking-action{margin-left:auto;color:#9aa4b5;font-size:11px;white-space:nowrap}
.thinking-chevron{flex:0 0 16px;width:16px;height:16px;fill:none;stroke:#8e99ac;stroke-width:1.7;stroke-linecap:round;stroke-linejoin:round;transform:rotate(-90deg);transition:transform .2s ease}
.thinking-chevron.open{transform:rotate(0)}
.thinking-content{box-sizing:border-box;width:100%;min-width:0;max-height:230px;overflow:auto;margin-top:8px;padding:2px 1px 4px;color:#747f94;font-size:12px;line-height:1.65;overflow-wrap:anywhere;word-break:break-word}
.thinking-content :deep(p){margin:.2em 0 .6em}.thinking-content :deep(:first-child){margin-top:0}.thinking-content :deep(:last-child){margin-bottom:0}
@keyframes thinkingPulse{50%{transform:scale(1.1);background:#dce5ff;box-shadow:0 0 0 4px rgba(73,108,255,.08)}}
</style>
