<template>
  <basic-container>
    <div class="metrics">
      <el-card v-for="item in metrics" :key="item.label" shadow="hover"><span>{{ item.label }}</span><b>{{ item.value }}</b><small>{{ item.tip }}</small></el-card>
    </div>
    <el-card shadow="never">
      <template #header><div class="header"><b>最近事件</b><el-button text type="primary" @click="load">刷新</el-button></div></template>
      <el-table :data="data.recentEvents" v-loading="loading" stripe>
        <el-table-column prop="eventNo" label="事件编号" width="190" />
        <el-table-column prop="eventSummary" label="事件摘要" min-width="280" show-overflow-tooltip />
        <el-table-column prop="creatorName" label="发起人" width="120" />
        <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.eventStatus==='ACTIVE'?'success':'info'">{{ statusText(row.eventStatus) }}</el-tag></template></el-table-column>
        <el-table-column prop="recipientCount" label="接收人数" width="100" />
        <el-table-column prop="activeBranchCount" label="活跃分支" width="100" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
      </el-table>
    </el-card>
  </basic-container>
</template>
<script setup>
import { computed,onMounted,reactive,ref } from 'vue';import {getAdminDashboard} from '@/api/smartReminder';
const loading=ref(false),data=reactive({totalEvents:0,activeEvents:0,activeBranches:0,dueBranches:0,totalNotifications:0,unreadNotifications:0,evaluationsToday:0,recentEvents:[]});
const metrics=computed(()=>[
  {label:'进行中事件',value:data.activeEvents,tip:`全部 ${data.totalEvents}`},
  {label:'活跃接收分支',value:data.activeBranches,tip:`当前待评估 ${data.dueBranches}`},
  {label:'已发送通知',value:data.totalNotifications,tip:`未读 ${data.unreadNotifications}`},
  {label:'今日 AI 评估',value:data.evaluationsToday,tip:'滚动决策次数'},
]);
const statusText=v=>({ACTIVE:'进行中',STOPPED:'已停止',COMPLETED:'已完成'}[v]||v);
const load=async()=>{loading.value=true;try{Object.assign(data,(await getAdminDashboard()).data.data||{});}finally{loading.value=false;}};onMounted(load);
</script>
<style scoped>.metrics{display:grid;grid-template-columns:repeat(4,minmax(180px,1fr));gap:16px;margin-bottom:16px}.metrics span,.metrics small{display:block;color:#8a93a3}.metrics b{display:block;font-size:30px;margin:10px 0;color:#25304a}.header{display:flex;justify-content:space-between;align-items:center}@media(max-width:1000px){.metrics{grid-template-columns:repeat(2,1fr)}}</style>
