<template>
  <basic-container>
    <el-card shadow="never">
      <template #header><div class="title"><div><b>模型配置</b><p>每类可启用多个模型，移动端按别名选择；系统默认模型仅一个，全局提示词独立维护。</p></div><div class="model-actions"><el-button :loading="loading" @click="refreshModels">刷新列表</el-button><el-button type="primary" @click="newConfig">新增模型</el-button></div></div></template>
      <el-radio-group v-model="type" @change="changeType"><el-radio-button value="LLM">LLM 对话模型</el-radio-button><el-radio-button value="SPEECH">语音识别模型</el-radio-button></el-radio-group>
      <el-table :data="configs.filter(x=>x.configType===type)" row-key="id" v-loading="loading" style="margin-top:20px" empty-text="该类型暂无模型，点击右上角新增">
        <el-table-column prop="configName" label="配置名称" min-width="180" />
        <el-table-column prop="modelAlias" label="移动端别名" min-width="160" />
        <el-table-column label="系统默认" width="110"><template #default="{row}"><el-tag v-if="row.systemDefault">系统默认</el-tag><span v-else>—</span></template></el-table-column>
        <el-table-column prop="modelName" label="模型名称" min-width="200" />
        <el-table-column label="状态" width="110"><template #default="{row}"><el-tag :type="row.enabled?'success':'info'">{{row.enabled?'已启用':'未启用'}}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="280"><template #default="{row}"><el-button link type="primary" @click="edit(row)">编辑参数</el-button><el-button link type="primary" :disabled="row.enabled||!!activating" :loading="activating===String(row.id)" @click="activate(row)">{{row.enabled?'已启用':'启用'}}</el-button><el-button link type="primary" :disabled="!row.enabled||row.systemDefault||!!activating" @click="makeDefault(row)">设为默认</el-button></template></el-table-column>
      </el-table>
    </el-card>
    <el-card shadow="never" class="prompts-card" v-loading="promptLoading">
      <template #header><div class="title"><div><b>全局业务提示词</b><p>所有 LLM 共用（含用户自配模型）。新增、编辑或切换模型均不会覆盖提示词。</p></div></div></template>
      <el-form label-position="top">
        <el-form-item label="意图识别提示词"><el-input v-model="prompts.intentPrompt" type="textarea" :rows="12" /></el-form-item>
        <el-form-item label="滚动决策提示词"><el-input v-model="prompts.decisionPrompt" type="textarea" :rows="10" /></el-form-item>
        <el-button type="primary" :loading="promptSaving" :disabled="promptLoading" @click="savePrompts">保存全局提示词</el-button>
      </el-form>
    </el-card>
    <el-dialog v-model="editorVisible" :title="form.id?'编辑模型参数':'新增模型'" width="min(95vw,1120px)" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="130px" style="max-width:920px">
        <el-form-item label="配置名称" prop="configName"><el-input v-model="form.configName" /></el-form-item>
        <el-form-item label="模型别名"><el-input v-model.trim="form.modelAlias" maxlength="100" /><div class="tip">移动端显示此名称；留空时使用配置名称。</div></el-form-item>
        <el-form-item label="接口地址" prop="baseUrl"><el-input v-model="form.baseUrl" placeholder="https://example.com/v1/chat/completions" /></el-form-item>
        <el-form-item label="API Key" prop="apiKey"><el-input v-model="form.apiKey" show-password autocomplete="new-password" /><div class="tip">页面只显示脱敏值，保存时留空或保留脱敏值不会覆盖原密钥。</div></el-form-item>
        <el-form-item label="模型" prop="modelName"><el-input v-model="form.modelName" /></el-form-item>
        <div v-if="type==='LLM'">
        <el-row :gutter="20"><el-col :span="8"><el-form-item label="上下文窗口"><el-input-number v-model="form.contextWindow" :min="1024" :max="1048576" /></el-form-item></el-col><el-col :span="8"><el-form-item label="最大输入"><el-input-number v-model="form.maxInputTokens" :min="1024" :max="1048576" /></el-form-item></el-col><el-col :span="8"><el-form-item label="最大输出"><el-input-number v-model="form.maxTokens" :min="128" :max="1048576" /></el-form-item></el-col></el-row>
		<el-row :gutter="20">
		  <el-col :span="8"><el-form-item label="Temperature"><el-input-number v-model="form.temperature" :min="0" :max="2" :step="0.1" /></el-form-item></el-col>
		  <el-col :span="8"><el-form-item label="超时(ms)"><el-input-number v-model="form.requestTimeout" :min="5000" :max="600000" :step="5000" /></el-form-item></el-col>
		  
		</el-row>
		<el-form-item label="显示思考过程"><el-switch v-model="form.showThinking" /><div class="tip inline-tip">开启后，移动端对话会实时显示模型思考，回答完成后自动折叠。</div></el-form-item>
		<el-form-item label="额外 body（JSON）"><el-input v-model="form.extraBody" type="textarea" :rows="6" placeholder='{"reasoning_effort":"high","enable_thinking":true}' /><div class="tip">按模型协议填写 JSON 对象，与请求 body 合并；同名参数覆盖默认值，null 移除默认值。不能覆盖 model、messages、stream。留空或 {} 不附加参数。</div></el-form-item>
        </div>
        <el-form-item v-if="type==='SPEECH'" label="超时(ms)"><el-input-number v-model="form.requestTimeout" :min="5000" :max="120000" :step="5000" /></el-form-item>
        <el-form-item v-if="type==='SPEECH'" label="额外表单参数"><el-input v-model="form.extraBody" type="textarea" :rows="4" placeholder='{"language":"zh","response_format":"json"}' /><div class="tip">JSON 对象，每个字段作为 multipart 表单参数发送，不能覆盖 model、file。</div></el-form-item>
        <el-form-item v-if="type==='SPEECH'" label="接口协议"><div class="tip">POST multipart/form-data，字段 model、file；返回 JSON 的 text 字段。密钥不会下发给 APP。</div></el-form-item>
        <el-form-item label="保存后启用"><el-switch v-model="form.enabled" :disabled="form.systemDefault" /><div v-if="form.systemDefault" class="tip inline-tip">停用前请先将其他模型设为系统默认。</div></el-form-item><el-form-item label="系统默认模型"><el-switch v-model="form.systemDefault" :disabled="!form.enabled" /><div class="tip inline-tip">同类型只保留一个默认；不会停用其他模型。</div></el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="save">保存配置</el-button><el-button v-if="type==='LLM'" :loading="testing" @click="test">测试连接</el-button></el-form-item>
      </el-form>
    </el-dialog>
  </basic-container>
</template>
<script setup>
import {onMounted,onActivated,onDeactivated,onBeforeUnmount,reactive,ref} from 'vue';
import {ElMessage,ElMessageBox} from 'element-plus';
import {getAiConfig,getAiConfigs,saveAiConfig,testAiConfig,activateAiConfig,setDefaultAiConfig,getAiPrompts,saveAiPrompts} from '@/api/smartReminder';
const type=ref('LLM'),configs=ref([]),formRef=ref(),saving=ref(false),testing=ref(false),loading=ref(false),editorVisible=ref(false),activating=ref('');
const promptLoading=ref(true),promptSaving=ref(false),prompts=reactive({intentPrompt:'',decisionPrompt:''});
const defaults=()=>({id:null,configType:type.value,configName:'',modelAlias:'',systemDefault:false,baseUrl:'',apiKey:'',modelName:'',contextWindow:1048576,maxInputTokens:991000,maxTokens:8192,temperature:.3,extraBody:'{}',showThinking:true,requestTimeout:120000,enabled:false});
const form=reactive(defaults());
const rules={configName:[{required:true,message:'请输入配置名称'}],baseUrl:[{required:true,message:'请输入接口地址'}],modelName:[{required:true,message:'请输入模型名称'}]};
let loadId=0,listLoadId=0,active=true,mounted=false;
// A slow pre-save response must not overwrite the fresh post-save list.
const reload=async()=>{const ticket=++listLoadId;loading.value=true;try{const rows=(await getAiConfigs()).data.data||[];if(ticket===listLoadId)configs.value=rows;}finally{if(ticket===listLoadId)loading.value=false;}};
const refreshModels=()=>reload().catch(()=>ElMessage.warning('模型列表刷新失败，请点击刷新列表重试'));
const refreshAfterSave=async()=>{try{await reload();}catch{ElMessage.warning('修改已保存，但列表刷新失败，请点击刷新列表重试');}};
const onFocus=()=>{if(active&&document.visibilityState==='visible'&&!saving.value&&!activating.value)refreshModels();};
const edit=async row=>{const ticket=++loadId,kind=type.value;const data=(await getAiConfig(kind,row.id)).data.data||{};if(ticket!==loadId||kind!==type.value)return;Object.assign(form,defaults(),data);editorVisible.value=true;};
const newConfig=()=>{loadId++;Object.assign(form,defaults());editorVisible.value=true;};
const changeType=()=>{loadId++;editorVisible.value=false;refreshModels();};
const activate=async row=>{
  await ElMessageBox.confirm('启用后用户可以在移动端选择此模型。其他模型和系统默认保持不变。','启用模型',{type:'info'});
  activating.value=String(row.id);
  try{await activateAiConfig(type.value,row.id);ElMessage.success('模型已启用');await refreshAfterSave();}finally{activating.value='';}
};
const makeDefault=async row=>{await ElMessageBox.confirm('跟随系统默认的用户将使用此模型；已自主选择的用户不受影响。','设置系统默认');activating.value=String(row.id);try{await setDefaultAiConfig(type.value,row.id);ElMessage.success('系统默认模型已更新');await refreshAfterSave();}finally{activating.value='';}};
const save=async()=>{
  await formRef.value.validate();
  try{const extra=JSON.parse(form.extraBody||'{}');if(!extra||Array.isArray(extra)||typeof extra!=='object'||['model','messages','stream','file'].some(k=>k in extra))throw Error();}
  catch{ElMessage.error('请填写 JSON 对象，不能覆盖 model、messages、stream、file');return;}
  saving.value=true;
  try{const {intentPrompt,decisionPrompt,...model}=form;await saveAiConfig(model);editorVisible.value=false;ElMessage.success('模型配置已保存');await refreshAfterSave();}finally{saving.value=false;}
};
const test=async()=>{if(!form.id){ElMessage.warning('请先保存模型再测试');return;}testing.value=true;try{const d=(await testAiConfig(form.id)).data.data;ElMessage.success(d.model+': '+d.content);}finally{testing.value=false;}};
const savePrompts=async()=>{if(!prompts.intentPrompt.trim()||!prompts.decisionPrompt.trim()){ElMessage.warning('提示词不能为空');return;}promptSaving.value=true;try{await saveAiPrompts({...prompts});ElMessage.success('全局提示词已保存');}finally{promptSaving.value=false;}};
onMounted(()=>{refreshModels();window.addEventListener('focus',onFocus);document.addEventListener('visibilitychange',onFocus);getAiPrompts().then(r=>Object.assign(prompts,r.data.data)).finally(()=>promptLoading.value=false);});
onActivated(()=>{active=true;if(mounted)refreshModels();mounted=true;});
onDeactivated(()=>{active=false;listLoadId++;loading.value=false;});
onBeforeUnmount(()=>{active=false;listLoadId++;loadId++;window.removeEventListener('focus',onFocus);document.removeEventListener('visibilitychange',onFocus);});
</script>
<style scoped>
.title{display:flex;align-items:center;justify-content:space-between;gap:20px}.title b{font-size:18px}.title p{margin:6px 0 0;color:#8b93a3;font-size:13px}.tip{font-size:12px;color:#9299a8}.inline-tip{margin-left:12px}.prompts-card{margin-top:20px}
.model-actions{display:flex;flex-shrink:0}
</style>
