<template>
  <el-dialog v-model="visible" title="选择提醒日期" width="min(92vw,380px)" align-center append-to-body class="reminder-date-dialog" @open="begin">
    <p class="range-intro">按卡片上的提醒日期筛选</p>
    <div class="range-endpoints">
      <button type="button" :class="{active:active==='start'}" @click="chooseEndpoint('start')"><small>开始日期</small><strong>{{display(draftStart)}}</strong></button>
      <span>—</span>
      <button type="button" :class="{active:active==='end'}" @click="chooseEndpoint('end')"><small>结束日期</small><strong>{{display(draftEnd)}}</strong></button>
    </div>
    <div class="range-shortcuts"><button v-for="item in shortcuts" :key="item.label" type="button" :class="{selected:draftStart===item.start&&draftEnd===item.end}" @click="shortcut(item)">{{item.label}}</button></div>
    <div class="range-month"><button type="button" aria-label="上个月" @click="month=shiftMonth(month,-1)"><UiIcon name="chevron"/></button><strong aria-live="polite">{{monthLabel}}</strong><button type="button" aria-label="下个月" @click="month=shiftMonth(month,1)"><UiIcon name="chevron"/></button></div>
    <div class="range-calendar">
      <span v-for="day in ['一','二','三','四','五','六','日']" :key="day" class="range-weekday">{{day}}</span>
      <button v-for="day in days" :key="day" type="button" :aria-label="day" :aria-pressed="day===draftStart||day===draftEnd" :aria-current="day===today?'date':undefined" :class="{'other-month':!day.startsWith(month),selected:day===draftStart||day===draftEnd,between:draftStart&&draftEnd&&day>draftStart&&day<draftEnd,today:day===today}" @click="select(day)"><span>{{Number(day.slice(-2))}}</span></button>
    </div>
    <p class="range-help" aria-live="polite">{{active==='start'?'请选择开始日期':'请选择结束日期，可与开始日期相同'}}</p>
    <template #footer><div class="range-footer"><button type="button" @click="visible=false">取消</button><button type="button" class="range-apply" @click="apply">确认筛选</button></div></template>
  </el-dialog>
</template>
<script setup>
import {computed,ref} from 'vue';
import UiIcon from './UiIcon.vue';
import {shanghaiToday,shiftDay,shiftMonth,calendarDays,selectRangeDay} from './reminderCalendar.mjs';
const props=defineProps({modelValue:Boolean,start:String,end:String}),emit=defineEmits(['update:modelValue','apply']);
const visible=computed({get:()=>props.modelValue,set:value=>emit('update:modelValue',value)});
const today=ref(shanghaiToday()),month=ref(today.value.slice(0,7)),draftStart=ref(''),draftEnd=ref(''),active=ref('start');
const days=computed(()=>calendarDays(month.value));
const monthLabel=computed(()=>`${month.value.slice(0,4)}年 ${Number(month.value.slice(5))}月`);
const display=value=>value?value.replaceAll('-',' / '):'不限';
const shortcuts=computed(()=>[{label:'不限',start:'',end:''},{label:'今天',start:today.value,end:today.value},{label:'明天',start:shiftDay(today.value,1),end:shiftDay(today.value,1)},{label:'未来7天',start:today.value,end:shiftDay(today.value,6)}]);
const begin=()=>{today.value=shanghaiToday();draftStart.value=props.start||'';draftEnd.value=props.end||'';active.value='start';month.value=(draftStart.value||draftEnd.value||today.value).slice(0,7);};
const chooseEndpoint=value=>{active.value=value;const date=value==='start'?draftStart.value:draftEnd.value;if(date)month.value=date.slice(0,7);};
const shortcut=item=>{draftStart.value=item.start;draftEnd.value=item.end;month.value=(item.start||today.value).slice(0,7);active.value='start';};
const select=day=>{const next=selectRangeDay(draftStart.value,draftEnd.value,active.value,day);draftStart.value=next.start;draftEnd.value=next.end;active.value=next.active;};
const apply=()=>{emit('apply',{start:draftStart.value,end:draftEnd.value});visible.value=false;};
</script>
<style>
.reminder-date-dialog.el-dialog{padding:22px 18px 18px;border:1px solid #fff;border-radius:26px;background:linear-gradient(145deg,#fff,#f5f8ff);box-shadow:0 24px 70px #23385a26;max-height:calc(100dvh - env(safe-area-inset-top) - env(safe-area-inset-bottom) - 32px);overflow-y:auto;overscroll-behavior:contain;color:#293956}
.reminder-date-dialog *{box-sizing:border-box;touch-action:manipulation}.reminder-date-dialog .el-dialog__header{padding:0 26px 0 0}.reminder-date-dialog .el-dialog__title{font-size:19px;font-weight:650;color:#253657}.reminder-date-dialog .el-dialog__headerbtn{width:44px;height:44px;top:10px;right:8px}.reminder-date-dialog .el-dialog__body{padding:0}.reminder-date-dialog .el-dialog__footer{padding:12px 0 0}
.range-intro{margin:8px 0 18px;font-size:12px;color:#929db2}.reminder-date-dialog button{font:inherit;cursor:pointer;-webkit-tap-highlight-color:transparent}.reminder-date-dialog button:focus-visible{outline:2px solid #5881ef;outline-offset:2px}
.range-endpoints{display:grid;grid-template-columns:minmax(0,1fr) 16px minmax(0,1fr);align-items:center;gap:6px}.range-endpoints>span{text-align:center;color:#bac4d6}.range-endpoints button{padding:10px 6px;border:1px solid #e6ebf5;border-radius:14px;background:#f2f5fa;text-align:left;min-height:65px}.range-endpoints small,.range-endpoints strong{display:block;padding-left:5px}.range-endpoints small{font-size:11px;color:#8b98af;margin-bottom:7px}.range-endpoints strong{font-size:12px;color:#526583;font-weight:600;white-space:nowrap}.range-endpoints button.active{background:#eef3ff;border-color:#9ab3f3;box-shadow:0 0 0 2px #e8efff}.range-endpoints button.active strong{color:#5073d1}
.range-shortcuts{display:flex;gap:7px;margin:15px 0}.range-shortcuts button{flex:1;min-width:0;border:1px solid transparent;border-radius:10px;padding:7px 0;background:#edf2fa;color:#7c8ba7;font-size:11px;min-height:32px}.range-shortcuts button.selected{border-color:#d9e3fc;background:#e8efff;color:#5577d0}
.range-month{display:flex;align-items:center;justify-content:space-between;padding:2px 0 8px}.range-month strong{font-size:15px;font-weight:650}.range-month button{width:36px;height:36px;border:0;background:#edf2fa;border-radius:11px;color:#8090ae;display:grid;place-items:center}.range-month svg{width:15px;height:15px}.range-month button:first-child svg{transform:rotate(180deg)}
.range-calendar{display:grid;grid-template-columns:repeat(7,minmax(0,1fr));row-gap:4px}.range-weekday{height:27px;display:grid;place-items:center;font-size:11px;color:#9da8bc}.range-calendar button{position:relative;border:0;min-width:0;height:38px;background:transparent;color:#3b4b67;font-size:14px;border-radius:11px;padding:0}.range-calendar button span{position:relative;z-index:1}.range-calendar .other-month{color:#bbc5d5}.range-calendar .between{background:#eaf0ff;border-radius:0;color:#6383cf}.range-calendar .selected{background:linear-gradient(140deg,#67aff4,#657bf2);color:white;box-shadow:0 3px 7px #647deb25}.range-calendar .today:not(.selected)::after{content:'';position:absolute;bottom:3px;left:calc(50% - 2px);width:4px;height:4px;border-radius:50%;background:#6f8fee}
.range-help{font-size:11px;color:#929fb5;margin:12px 0 0;min-height:17px}.range-footer{display:flex;gap:10px}.range-footer button{min-height:42px;border:0;border-radius:13px;background:#edf1f8;color:#8391aa;flex:1;font-size:14px}.range-footer .range-apply{flex:2;background:linear-gradient(120deg,#70b2f4,#677ff1);color:#fff;box-shadow:0 4px 12px #6a89ec20}.reminder-date-dialog button:active{filter:brightness(.97)}
.reminder-date-dialog.el-dialog{display:flex;flex-direction:column;overflow:hidden}.reminder-date-dialog .el-dialog__header,.reminder-date-dialog .el-dialog__footer{flex:none}.reminder-date-dialog .el-dialog__body{min-height:0;overflow-y:auto;overscroll-behavior:contain;scrollbar-width:thin;padding:0 2px}.reminder-date-dialog .el-dialog__footer{position:relative;background:#f5f8ff}
@media(max-width:350px){.reminder-date-dialog.el-dialog{padding:18px 12px 14px}.range-calendar button{height:34px}.range-endpoints strong{font-size:11px;padding-left:1px}.range-shortcuts{gap:4px}}
</style>
