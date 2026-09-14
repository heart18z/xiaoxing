import { createApp } from 'vue';
import website from './config/website';
import axios from './axios';
import router from './router/';
import store from './store';
import i18n from './lang/';
import { language, messages } from './lang/';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import Avue from '@smallwei/avue';
import '@smallwei/avue/lib/index.css';
import avueUeditor from 'avue-plugin-ueditor';
import crudCommon from '@/mixins/crud.js';
import { getScreen, findColumn } from './utils/util';
import './permission';
import error from './error';
import App from './App.vue';
import 'animate.css';
import dayjs from 'dayjs';
import 'styles/common.scss';
import './page/smart-reminder/mobile-feedback.css';
// 系统组件
import debug from './debug';
import VueClipboard from 'vue3-clipboard';
import highlight from './components/highlight/main.vue';
import codeEditor from './components/code-editor/main.vue';
import cronEditor from './components/cron-editor/main.vue';
import basicBlock from './components/basic-block/main.vue';
import basicContainer from './components/basic-container/main.vue';
import thirdRegister from './components/third-register/main.vue';
import flowDesign from './components/flow-design/main.vue';
import flowDesignStep from './components/flow-design-step/main.vue';
// 业务组件
import codeSetting from './views/tool/codesetting.vue';
import formSetting from './views/tool/formsetting.vue';
// old-front 迁移业务组件
import pageAttach from './components/attach-dialog/page-attach/main.vue';
import rowAttach from './components/attach-dialog/row-attach/main.vue';
import pickAttach from './components/attach-dialog/pick-attach/main.vue';
import signAttach from './components/attach-dialog/sign-attach/main.vue';
import messageAttach from './components/attach-dialog/message-attach/main.vue';
import attachDialog from './components/attach-dialog/main.vue';
import filePreviewer from './components/file-previewer/main.vue';
import noteSetting from './components/note-setting/main.vue';
import selectLazy from './components/select-lazy/index.vue';
import userInfo from './components/user-info/main.vue';
import jobShowForm from './components/job-show-form/index.vue';
import messageBuild from './components/message-build/main.vue';
import aiBtnChatDialog from './components/ai-btn-chat-dialog/main.vue';
import bizParam from './components/bizParam/index.vue';
import colorSolidRound from './components/button/color-solid-round.vue';
import { setMessageConfig } from './utils/elementfunc';

window.$crudCommon = crudCommon;
debug();
window.axios = axios;
const app = createApp(App);
// avue 的 form-temp 组件未声明 prop 属性且为多根节点，crud 单元格编辑时会触发
// "Extraneous non-props attributes (prop)" 警告（库内部缺陷，3.9.2 仍未修复），此处仅屏蔽该条
if (import.meta.env.DEV) {
  app.config.warnHandler = (msg, instance, trace) => {
    if (msg.includes('Extraneous non-props attributes (prop)') && trace.includes('FormTemp')) {
      return;
    }
    console.warn(`[Vue warn]: ${msg}\n${trace}`);
  };
}
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component);
}
app.component('basicContainer', basicContainer);
app.component('basicBlock', basicBlock);
app.component('highlight', highlight);
app.component('codeEditor', codeEditor);
app.component('cronEditor', cronEditor);
app.component('thirdRegister', thirdRegister);
app.component('flowDesign', flowDesign);
app.component('flowDesignStep', flowDesignStep);
app.component('codeSetting', codeSetting);
app.component('formSetting', formSetting);
app.component('pageAttach', pageAttach);
app.component('rowAttach', rowAttach);
app.component('signAttach', signAttach);
app.component('messageAttach', messageAttach);
app.component('attachDialog', attachDialog);
app.component('filePreviewer', filePreviewer);
app.component('noteSetting', noteSetting);
app.component('selectLazy', selectLazy);
app.component('userInfo', userInfo);
app.component('jobShowForm', jobShowForm);
app.component('messageBuild', messageBuild);
app.component('aiBtnChatDialog', aiBtnChatDialog);
app.component('bizParam', bizParam);
app.component('colorSolidRound', colorSolidRound);
app.config.globalProperties.$app = app;
app.config.globalProperties.$dayjs = dayjs;
app.config.globalProperties.website = website;
app.config.globalProperties.getScreen = getScreen;
app.config.globalProperties.findColumn = findColumn;
app.use(error);
app.use(i18n);
app.use(store);
app.use(router);
app.use(ElementPlus, {
  locale: messages[language],
});
app.use(Avue, {
  axios,
  calcHeight: 120,
  crudOption: {
    height: 'auto',
    calcHeight: 30,
  },
  locale: messages[language],
  indexWidth: 70,
  // 工具栏默认只显示刷新按钮（各页面可在 option 中单独覆盖）
  columnBtn: false,
  gridBtn: false,
  searchShowBtn: false,
  filterBtn: false,
  refreshBtn: true,
});
// 兼容旧版 Avue 2 拼写错误的 vaildData 方法名
app.config.globalProperties.vaildData = app.config.globalProperties.validData;
app.use(avueUeditor, { axios });
app.use(VueClipboard, {
  autoSetContainer: true,
  appendToBody: true, // 这可以帮助解决一些更复杂的使用场景下的问题
});
app.config.globalProperties.$message = setMessageConfig(6);
app.mount('#app');
