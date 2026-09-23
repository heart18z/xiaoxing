<template>
  <basic-container>
    <h2>APP版本管理</h2>
    <p>上传安卓完整APK并保存草稿，确认后发布。用户将在设置页看到更新红点。</p>
    <el-form label-width="110px" style="max-width:720px">
      <el-form-item label="版本代码"><el-input-number v-model="form.versionCode" :min="1" :precision="0" /><span class="hint">对应 manifest 的 versionCode，必须递增</span></el-form-item>
      <el-form-item label="版本名称"><el-input v-model="form.versionName" maxlength="40" placeholder="例如 1.0.117" /></el-form-item>
      <el-form-item label="更新说明"><el-input v-model="form.notes" type="textarea" :rows="4" maxlength="4000" show-word-limit /></el-form-item>
      <el-form-item label="APK文件"><input ref="picker" type="file" accept=".apk" :disabled="busy" @change="choose" /></el-form-item>
      <el-form-item><el-button type="primary" :loading="busy" @click="upload">{{ busy ? '正在上传 '+progress+'%' : '上传并保存草稿' }}</el-button></el-form-item>
    </el-form>
    <el-alert title="请使用与已安装应用相同的包名和签名证书；安装前手机会校验版本、文件完整性及签名。" type="info" :closable="false" />
    <el-table v-loading="loading" :data="rows" style="margin-top:20px">
      <el-table-column prop="versionCode" label="版本代码" width="100" />
      <el-table-column prop="versionName" label="版本名称" width="140" />
      <el-table-column prop="notes" label="更新说明" show-overflow-tooltip />
      <el-table-column label="大小" width="110"><template #default="{row}">{{ (row.fileSize/1048576).toFixed(1) }} MB</template></el-table-column>
      <el-table-column label="状态" width="100"><template #default="{row}"><el-tag :type="row.published ? 'success':'info'">{{ row.published ? '已发布':'草稿/已下架' }}</el-tag></template></el-table-column>
      <el-table-column label="操作" width="130"><template #default="{row}"><el-button :disabled="busy" :type="row.published ? 'warning':'primary'" @click="publish(row)">{{ row.published ? '下架':'发布' }}</el-button></template></el-table-column>
    </el-table>
  </basic-container>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/axios';
const rows=ref([]), loading=ref(false), busy=ref(false), progress=ref(0), picker=ref(null);
const form=reactive({versionCode:117,versionName:'',notes:''});
let file=null;
const choose=e=>{file=e.target.files[0]||null;};
const endpoint='/api/blade-smart/admin/releases';
async function load(){loading.value=true;try{rows.value=(await request({url:endpoint,method:'post'})).data.data||[];}finally{loading.value=false;}}
async function upload(){
 if(busy.value)return;
 if(!file||!file.name.toLowerCase().endsWith('.apk')||file.size>300*1048576||!form.versionName.trim()||!form.notes.trim()){ElMessage.warning('请填写版本信息并选择不超过300MB的APK');return;}
 busy.value=true;progress.value=0;
 try{const data=new FormData();data.append('file',file);Object.entries(form).forEach(([k,v])=>data.append(k,String(v)));
 await request({url:endpoint+'/upload',method:'post',data,timeout:600000,onUploadProgress:e=>{progress.value=e.total?Math.floor(e.loaded/e.total*100):0;}});
 ElMessage.success('草稿已保存，请确认后发布');file=null;if(picker.value)picker.value.value='';await load();
 }finally{busy.value=false;}
}
async function publish(row){
 try{await ElMessageBox.confirm(row.published?'下架后将不再向用户提供此版本。':'发布版本 '+row.versionName+'？请确认已完成安装和功能测试。','确认版本操作');}catch{return;}
 busy.value=true;try{await request({url:endpoint+'/publish',method:'post',params:{id:row.id,published:!row.published}});await load();ElMessage.success('已更新发布状态');}finally{busy.value=false;}
}
onMounted(load);
</script>
<style scoped>.hint{color:#8791a2;margin-left:12px;font-size:12px}p{color:#68758a;margin-bottom:24px}</style>
