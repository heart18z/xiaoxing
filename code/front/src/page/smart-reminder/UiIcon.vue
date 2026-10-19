<template>
  <svg class="ui-icon" :style="iconStyle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><component v-for="(node,index) in lucideIcons[name] || lucideIcons.note" :is="node.tag" :key="index" v-bind="node.attrs" /></svg>
</template>
<script setup>
import {computed} from 'vue';
import {lucideIcons} from './lucideIcons';
const props=defineProps({name:String,size:Number,tone:String});
const tones={muted:'#8e9ab0',blue:'#3267ed',purple:'#8a63d8',ink:'#243550',green:'#19ad88',red:'#ef6675',white:'#ffffff',violet:'#885bdd',amber:'#d99b37'};
const iconStyle=computed(()=>({color:tones[props.tone],width:props.size?props.size+'px':undefined,height:props.size?props.size+'px':undefined}));
</script>
<style scoped>.ui-icon{width:22px;height:22px;flex:0 0 auto;vertical-align:middle}</style>
