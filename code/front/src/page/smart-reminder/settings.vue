<template>
  <AppShell :title="mt('设置')" :subtitle="mt('模型与语言')" variant="event-detail" back back-to="/app/me">
    <form class="settings-form" @submit.prevent="save">
      <NotificationSettings />
      <p v-if="loading" class="sr-card" role="status">{{ mt("正在读取设置…") }}</p>
      <template v-else>
        <p v-if="error" class="settings-error sr-card" role="alert">{{ mt(error) }}</p>
        <section class="sr-card language-settings"><label for="app-language">{{ mt("界面语言") }}</label><el-select id="app-language" v-model="form.language" class="settings-select" popper-class="model-choice-menu" :show-arrow="false" :aria-label="mt('界面语言')"><el-option value="zh-cn" :label="mt('中文')" /><el-option value="en-us" label="English" /></el-select><small>{{ mt("保存后生效，历史消息保持原文。") }}</small></section>
        <section v-for="kind in kinds" :key="kind.key" class="sr-card model-settings">
          <h2>{{ mt(kind.title) }}</h2>
          <div class="model-source"><label><input v-model="form[kind.key+'Mode']" type="radio" :name="kind.key+'-source'" value="SYSTEM" />{{ mt("使用系统配置") }}</label><label><input v-model="form[kind.key+'Mode']" type="radio" :name="kind.key+'-source'" value="PERSONAL" />{{ mt("使用个人配置") }}</label></div>
          <div v-if="form[kind.key+'Mode']==='SYSTEM'" class="personal-fields"><label :for="kind.key+'-choice'">{{ mt("我的模型") }}</label><el-select :id="kind.key+'-choice'" v-model="form[kind.key+'ConfigId']" :empty-values="[null,undefined]" class="settings-select" popper-class="model-choice-menu" :show-arrow="false" :aria-label="mt(kind.title)+' · '+mt('我的模型')"><el-option value="" :label="mt('跟随系统默认')" /><el-option v-for="model in models.filter(m=>m.configType===kind.key.toUpperCase())" :key="model.id" :value="String(model.id)" :label="model.modelAlias||model.configName"><span class="model-choice-label">{{ model.modelAlias||model.configName }}</span><span v-if="model.systemDefault" class="model-default-badge">{{ mt('系统默认') }}</span></el-option></el-select><p class="model-tip">{{ mt("使用管理员维护的模型，无需填写地址和密钥。") }}</p></div>
          <div v-else class="personal-fields">
            <label :for="kind.key+'-url'">{{ mt("接口地址") }}</label><input :id="kind.key+'-url'" v-model.trim="form[kind.key].baseUrl" type="url" class="sr-input" placeholder="https://example.com/v1" required />
            <label :for="kind.key+'-model'">{{ mt("模型名称") }}</label><input :id="kind.key+'-model'" v-model.trim="form[kind.key].modelName" class="sr-input" :placeholder="mt('请输入模型名称')" required />
            <label :for="kind.key+'-key'">API Key</label><input :id="kind.key+'-key'" v-model.trim="form[kind.key].apiKey" type="password" autocomplete="new-password" class="sr-input" :placeholder="mt('填写 Key，留空保留已保存的密钥')" />
            <template v-if="kind.key==='llm'"><label for="personal-extra">{{ mt("额外 body（JSON）") }}</label><textarea id="personal-extra" v-model="form.llm.extraBody" class="sr-input json-input" rows="5" spellcheck="false" placeholder='{"reasoning_effort":"high"}'></textarea><small>{{ mt("可添加不同模型的参数。设为 null 可移除默认参数；不可覆盖 model、messages、stream。") }}</small><label class="thinking-switch"><input v-model="form.llm.showThinking" type="checkbox" />{{ mt("显示模型思考过程") }}</label></template>
            <small>{{ mt("仅支持公网 HTTPS 地址。密钥加密保存，只用于你自己的模型请求。") }}</small>
          </div>
        </section>
        <button v-if="failed" type="button" class="sr-button save-settings" @click="load">{{ mt("重新加载") }}</button><button v-else class="sr-button save-settings" :disabled="saving">{{ saving?mt('保存中…'):mt('保存设置') }}</button>
      </template>
    </form>
  </AppShell>
</template>
<script setup>
import {mt} from './mobileLocale';
import {onMounted,reactive,ref} from 'vue';
import {ElMessage,ElSelect,ElOption} from 'element-plus';
import AppShell from './AppShell.vue';
import NotificationSettings from '@/native/NotificationSettings.vue';
import {getModelSettings,saveModelSettings} from '@/api/smartReminder';
import {setMobileLanguage} from './mobileLocale';
const blank=()=>({baseUrl:'',modelName:'',apiKey:'',extraBody:'{}',showThinking:true});
const kinds=[{key:'llm',title:'LLM 对话模型'},{key:'speech',title:'语音识别模型'}];
const models=ref([]);
const loading=ref(true),saving=ref(false),error=ref(''),failed=ref(false),form=reactive({language:'zh-cn',llmConfigId:'',speechConfigId:'',llmMode:'SYSTEM',speechMode:'SYSTEM',llm:blank(),speech:blank()});
const apply=data=>{models.value=data.models||[];for(const key of ['llm','speech']){const id=String(data[key+'ConfigId']||'');form[key+'ConfigId']=models.value.some(m=>String(m.id)===id&&m.configType===key.toUpperCase())?id:'';}form.language=data.language||'zh-cn';for(const key of ['llm','speech']){form[key+'Mode']=data[key+'Mode']||'SYSTEM';form[key]={...blank(),...data[key]};}};
const load=async()=>{loading.value=true;error.value='';failed.value=false;try{apply((await getModelSettings()).data.data||{});}catch{failed.value=true;error.value='读取设置失败，请重试';}finally{loading.value=false;}};
const save=async()=>{if(saving.value||failed.value)return;error.value='';if(form.llmMode==='PERSONAL'){try{const value=JSON.parse(form.llm.extraBody||'{}');if(!value||Array.isArray(value)||typeof value!=='object'||['model','messages','stream'].some(k=>k in value))throw Error();}catch{error.value='额外 body 必须是 JSON 对象，且不能覆盖 model、messages、stream';return;}}saving.value=true;try{const data={llmConfigId:form.llmConfigId||null,speechConfigId:form.speechConfigId||null,language:form.language,llmMode:form.llmMode,speechMode:form.speechMode,llm:form.llmMode==='PERSONAL'?form.llm:null,speech:form.speechMode==='PERSONAL'?form.speech:null};apply((await saveModelSettings(data)).data.data||{});setMobileLanguage(form.language);ElMessage.success(mt('设置已保存'));}catch(e){error.value=e?.response?.data?.msg||'保存失败，请重试';}finally{saving.value=false;}};
onMounted(load);
</script>
<style scoped>
.settings-select{width:100%;min-width:0;--el-color-primary:#5269ed;--el-border-radius-base:14px}.settings-select :deep(.el-select__wrapper){min-height:46px;padding:10px 14px;border-radius:14px;background:#f8faff;box-shadow:0 0 0 1px #e0e7f4 inset}.settings-select :deep(.el-select__wrapper.is-focused){box-shadow:0 0 0 1px #8c9cef inset,0 0 0 3px #edf1ff}.settings-select :deep(.el-select__selected-item){font-size:14px;color:#3d4d6c}.settings-select :deep(.el-select__caret){color:#8b98b6;font-size:16px}
.settings-form{display:grid;gap:14px;padding-bottom:24px}.model-settings,.language-settings,.personal-fields{display:flex;flex-direction:column;gap:11px}.model-settings h2{font-size:17px;margin:0 0 4px;color:#293854}.model-settings label,.language-settings label{font-size:14px;font-weight:600;color:#40516b}.settings-form small,.model-tip{font-size:12px;line-height:1.65;color:#8793a8}.model-tip{margin:4px 0;background:#f5f7fd;padding:12px;border-radius:12px}.settings-error{color:#bf5353;font-size:13px;line-height:1.6}.model-source{display:flex;flex-wrap:wrap;gap:13px;padding:10px 0;border-bottom:1px solid #edf0f7}.model-source label,.thinking-switch{display:flex;align-items:center;gap:5px}.personal-fields{padding-top:5px}.settings-form input[type=radio],.settings-form input[type=checkbox]{accent-color:#5067ee}.json-input{font:12px/1.6 Consolas,monospace;resize:vertical;min-height:105px}.settings-form .sr-input{width:100%;min-width:0}.save-settings{width:100%;min-height:44px}
</style>
<style>
.model-choice-menu.el-popper{border:1px solid #e6ebf7!important;border-radius:16px!important;background:#fff!important;box-shadow:0 10px 35px #4e679729!important;overflow:hidden;max-width:calc(100vw - 32px)}.model-choice-menu .el-select-dropdown__wrap{max-height:244px}.model-choice-menu .el-select-dropdown__list{padding:6px}.model-choice-menu .el-select-dropdown__item{height:auto;min-height:42px;line-height:1.5;display:flex;align-items:center;gap:8px;margin:2px 0;padding:10px 12px;border-radius:10px;font-size:14px;color:#56637e;white-space:normal;overflow-wrap:anywhere}.model-choice-menu .el-select-dropdown__item.is-hovering{background:#f3f6fd}.model-choice-menu .el-select-dropdown__item.is-selected{color:#4b62e7;background:#edf2ff;font-weight:600}.model-choice-label{flex:1;min-width:0}.model-default-badge{flex:0 0 auto;border-radius:6px;padding:2px 5px;background:#e8eeff;color:#697de0;font-size:10px;font-weight:500}
</style>
