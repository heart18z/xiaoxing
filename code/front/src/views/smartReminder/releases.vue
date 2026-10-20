<template>
  <basic-container>
    <h2>APP版本管理</h2>
    <section class="download-settings" v-loading="configLoading">
      <div class="section-heading">
        <div>
          <h3>统一下载页</h3>
          <p>一个网址和二维码，统一管理安卓、iOS 与鸿蒙下载入口。</p>
        </div>
        <el-button :disabled="!configReady" @click="previewPage">预览下载页</el-button>
      </div>
      <div class="settings-layout">
        <el-form label-width="145px" class="download-form">
          <el-form-item label="统一下载页网址">
            <el-input
              v-model="downloadForm.pageUrl"
              maxlength="1000"
              placeholder="https://你的域名/download/"
            />
            <span class="field-hint">
              填写实际可访问的 HTTPS 下载页网址，二维码将使用这个地址。
            </span>
          </el-form-item>
          <el-form-item label="下载渠道">
            <div class="channel-switches">
              <label class="channel-switch">
                <span>Android</span>
                <el-switch v-model="downloadForm.androidEnabled" aria-label="Android 下载渠道" />
              </label>
              <label class="channel-switch">
                <span>iOS</span>
                <el-switch v-model="downloadForm.iosEnabled" aria-label="iOS 下载渠道" />
              </label>
              <label class="channel-switch">
                <span>鸿蒙</span>
                <el-switch v-model="downloadForm.harmonyEnabled" aria-label="鸿蒙下载渠道" />
              </label>
            </div>
            <span class="field-hint">
              开启的渠道可点击下载；关闭后入口仍显示，但置灰且不可点击。Android 自动下载最新已发布
              APK。
            </span>
          </el-form-item>
          <el-form-item label="iOS 下载方式">
            <el-radio-group v-model="downloadForm.iosMode">
              <el-radio value="testflight">TestFlight 邀请</el-radio>
              <el-radio value="appstore">App Store</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="downloadForm.iosMode === 'testflight'" label="TestFlight 邀请链接">
            <el-input
              v-model="downloadForm.testflightUrl"
              maxlength="1000"
              placeholder="https://testflight.apple.com/join/邀请码"
            />
          </el-form-item>
          <el-form-item v-else label="App Store 链接">
            <el-input
              v-model="downloadForm.appStoreUrl"
              maxlength="1000"
              placeholder="https://apps.apple.com/…"
            />
          </el-form-item>
          <el-form-item label="鸿蒙 APK 兼容版">
            <el-switch
              v-model="downloadForm.harmonyApkEnabled"
              :disabled="!downloadForm.harmonyEnabled"
              aria-label="鸿蒙 APK 兼容版"
            />
            <span class="inline-hint">HarmonyOS 4.x 及更早版本，下载安卓 APK</span>
          </el-form-item>
          <el-form-item label="原生鸿蒙下载">
            <el-switch
              v-model="downloadForm.harmonyNativeEnabled"
              :disabled="!downloadForm.harmonyEnabled"
              aria-label="原生鸿蒙下载"
            />
            <span class="inline-hint">HarmonyOS 5 及以上 / NEXT</span>
          </el-form-item>
          <el-form-item label="鸿蒙应用市场链接">
            <el-input
              v-model="downloadForm.harmonyStoreUrl"
              maxlength="1000"
              placeholder="华为应用市场官方分享链接"
            />
            <span class="field-hint">
              开启原生鸿蒙下载前需填写已上架应用的链接。关闭鸿蒙总开关会停用两个入口，并保留原配置。
            </span>
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="configSaving"
              :disabled="!configReady"
              @click="saveDownload"
            >
              保存下载配置
            </el-button>
            <span class="inline-hint">保存后立即生效</span>
          </el-form-item>
        </el-form>
        <aside class="qr-panel">
          <h4>统一下载二维码</h4>
          <template v-if="configReady">
            <img :src="qrUrl" alt="统一下载页二维码" width="160" height="160" />
            <p class="qr-address">{{ savedPageUrl }}</p>
            <el-button @click="copyPage">复制网址</el-button>
            <el-button @click="saveQr">下载二维码</el-button>
          </template>
          <p v-else>加载配置后显示二维码</p>
          <p class="field-hint">二维码指向下载页；更新版本或切换商店链接，无需重新印制。</p>
        </aside>
      </div>
    </section>
    <el-divider content-position="left">安卓安装包与版本发布</el-divider>
    <p>
      上传安卓完整APK并保存草稿，确认后发布。用户启动新版 APP
      时会弹窗提示更新，设置页同步显示更新状态。
    </p>
    <el-form label-width="110px" style="max-width: 720px">
      <el-form-item label="版本代码">
        <el-input-number v-model="form.versionCode" :min="1" :precision="0" />
        <span class="hint">对应 manifest 的 versionCode，必须递增</span>
      </el-form-item>
      <el-form-item label="版本名称">
        <el-input v-model="form.versionName" maxlength="40" placeholder="例如 1.0.117" />
      </el-form-item>
      <el-form-item label="更新说明">
        <el-input v-model="form.notes" type="textarea" :rows="4" maxlength="4000" show-word-limit />
      </el-form-item>
      <el-form-item label="强制更新">
        <el-switch v-model="form.forceUpdate" />
        <span class="hint">开启后，支持此功能的 APP 必须完成更新才能继续使用。</span>
      </el-form-item>
      <el-form-item label="APK文件">
        <input ref="picker" type="file" accept=".apk" :disabled="busy" @change="choose" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="busy" @click="upload">
          {{ busy ? '正在上传 ' + progress + '%' : '上传并保存草稿' }}
        </el-button>
      </el-form-item>
    </el-form>
    <el-alert
      title="请使用与已安装应用相同的包名和签名证书；安装前手机会校验版本、文件完整性及签名。"
      type="info"
      :closable="false"
    />
    <el-table v-loading="loading" :data="rows" style="margin-top: 20px">
      <el-table-column prop="versionCode" label="版本代码" width="100" />
      <el-table-column prop="versionName" label="版本名称" width="140" />
      <el-table-column prop="fileName" label="APK文件名" min-width="220" show-overflow-tooltip />
      <el-table-column prop="notes" label="更新说明" show-overflow-tooltip />
      <el-table-column label="大小" width="110">
        <template #default="{ row }">{{ (row.fileSize / 1048576).toFixed(1) }} MB</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.published ? 'success' : 'info'">
            {{ row.published ? '已发布' : '草稿/已下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新策略" width="130">
        <template #default="{ row }">
          <el-switch
            :model-value="row.forceUpdate === true"
            :disabled="busy"
            active-text="强制"
            @change="(value) => setPolicy(row, value)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button
            :disabled="busy"
            :type="row.published ? 'warning' : 'primary'"
            @click="publish(row)"
          >
            {{ row.published ? '下架' : '发布' }}
          </el-button>
          <el-button
            v-if="!row.published"
            :disabled="busy"
            type="danger"
            plain
            @click="remove(row)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </basic-container>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import request from '@/axios';
const rows = ref([]),
  loading = ref(false),
  busy = ref(false),
  progress = ref(0),
  picker = ref(null);
const form = reactive({ versionCode: 117, versionName: '', notes: '', forceUpdate: false });
let file = null;
const choose = (e) => {
  file = e.target.files[0] || null;
};
const endpoint = '/api/blade-smart/admin/releases';
const configLoading = ref(false),
  configSaving = ref(false),
  configReady = ref(false),
  savedPageUrl = ref(''),
  qrUrl = ref('/api/app/download/qr');
const downloadForm = reactive({
  pageUrl: '',
  androidEnabled: true,
  iosEnabled: false,
  harmonyEnabled: true,
  iosMode: 'testflight',
  appStoreUrl: '',
  testflightUrl: '',
  harmonyApkEnabled: true,
  harmonyNativeEnabled: false,
  harmonyStoreUrl: '',
});
async function loadDownload() {
  configLoading.value = true;
  try {
    const data = (await request({ url: endpoint + '/download-page', method: 'post' })).data.data;
    Object.assign(downloadForm, data);
    savedPageUrl.value = data.pageUrl;
    qrUrl.value = '/api/app/download/qr?v=' + Date.now();
    configReady.value = true;
  } finally {
    configLoading.value = false;
  }
}
function validHttps(value, hosts) {
  try {
    const u = new URL(value);
    return (
      u.protocol === 'https:' &&
      !u.username &&
      !u.password &&
      (!u.port || u.port === '443') &&
      (!hosts || hosts.includes(u.hostname))
    );
  } catch {
    return false;
  }
}
async function saveDownload() {
  if (configSaving.value || !configReady.value) return;
  if (!validHttps(downloadForm.pageUrl)) {
    ElMessage.warning('请填写有效的 HTTPS 下载页网址');
    return;
  }
  const iosUrl =
    downloadForm.iosMode === 'appstore' ? downloadForm.appStoreUrl : downloadForm.testflightUrl;
  if (
    downloadForm.iosEnabled &&
    !validHttps(iosUrl, [
      downloadForm.iosMode === 'appstore' ? 'apps.apple.com' : 'testflight.apple.com',
    ])
  ) {
    ElMessage.warning('开启 iOS 下载前，请填写对应的官方链接');
    return;
  }
  if (
    downloadForm.harmonyEnabled &&
    downloadForm.harmonyNativeEnabled &&
    !validHttps(downloadForm.harmonyStoreUrl, [
      'appgallery.huawei.com',
      'appgallery.cloud.huawei.com',
    ])
  ) {
    ElMessage.warning('开启原生鸿蒙下载前，请填写华为应用市场链接');
    return;
  }
  configSaving.value = true;
  try {
    await request({
      url: endpoint + '/download-page/save',
      method: 'post',
      data: { ...downloadForm },
    });
    await loadDownload();
    ElMessage.success('下载配置已保存');
  } finally {
    configSaving.value = false;
  }
}
function previewPage() {
  if (validHttps(savedPageUrl.value))
    window.open(savedPageUrl.value, '_blank', 'noopener,noreferrer');
}
async function copyPage() {
  try {
    await navigator.clipboard.writeText(savedPageUrl.value);
    ElMessage.success('下载页网址已复制');
  } catch {
    ElMessage.info('请复制二维码下方的网址');
  }
}
async function saveQr() {
  try {
    const response = await fetch('/api/app/download/qr', { cache: 'no-store' });
    if (!response.ok) throw new Error();
    const url = URL.createObjectURL(await response.blob());
    const link = document.createElement('a');
    link.href = url;
    link.download = 'AI小醒统一下载二维码.png';
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  } catch {
    ElMessage.error('二维码下载失败，请稍后再试');
  }
}
async function load() {
  loading.value = true;
  try {
    rows.value = (await request({ url: endpoint, method: 'post' })).data.data || [];
  } finally {
    loading.value = false;
  }
}
async function upload() {
  if (busy.value) return;
  if (
    !file ||
    !file.name.toLowerCase().endsWith('.apk') ||
    file.size > 300 * 1048576 ||
    !form.versionName.trim() ||
    !form.notes.trim()
  ) {
    ElMessage.warning('请填写版本信息并选择不超过300MB的APK');
    return;
  }
  if (rows.value.some((row) => Number(row.versionCode) === Number(form.versionCode))) {
    ElMessage.warning('版本代码已存在，请先删除草稿/已下架版本，或使用新的版本代码');
    return;
  }
  busy.value = true;
  progress.value = 0;
  try {
    const data = new FormData();
    data.append('file', file);
    Object.entries(form).forEach(([k, v]) => data.append(k, String(v)));
    await request({
      url: endpoint + '/upload',
      method: 'post',
      data,
      timeout: 600000,
      onUploadProgress: (e) => {
        progress.value = e.total ? Math.floor((e.loaded / e.total) * 100) : 0;
      },
    });
    ElMessage.success('草稿已保存，请确认后发布');
    file = null;
    if (picker.value) picker.value.value = '';
    await load();
  } finally {
    busy.value = false;
  }
}
async function setPolicy(row, value) {
  if (busy.value) return;
  try {
    await ElMessageBox.confirm(
      value
        ? '将此版本设为强制更新？此版本作为最新已发布版本时，用户将无法跳过更新。'
        : '取消此版本的强制更新？',
      '确认更新策略',
    );
  } catch {
    return;
  }
  busy.value = true;
  try {
    await request({
      url: endpoint + '/policy',
      method: 'post',
      params: { id: row.id, forceUpdate: value },
    });
    await load();
    ElMessage.success('更新策略已保存');
  } finally {
    busy.value = false;
  }
}
async function publish(row) {
  try {
    await ElMessageBox.confirm(
      row.published
        ? '下架后将不再向用户提供此版本。'
        : '发布版本 ' + row.versionName + '？请确认已完成安装和功能测试。',
      '确认版本操作',
    );
  } catch {
    return;
  }
  busy.value = true;
  try {
    await request({
      url: endpoint + '/publish',
      method: 'post',
      params: { id: row.id, published: !row.published },
    });
    await load();
    ElMessage.success('已更新发布状态');
  } finally {
    busy.value = false;
  }
}
async function remove(row) {
  if (busy.value || row.published) return;
  try {
    await ElMessageBox.confirm(
      '删除版本 ' +
        row.versionCode +
        '（' +
        (row.fileName || 'APK') +
        '）及其安装包？删除后可重新上传此版本代码。',
      '删除下架版本',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    );
  } catch {
    return;
  }
  busy.value = true;
  try {
    await request({ url: endpoint + '/delete', method: 'post', params: { id: row.id } });
    await load();
    ElMessage.success('版本已删除');
  } finally {
    busy.value = false;
  }
}
onMounted(() => {
  load();
  loadDownload();
});
</script>
<style scoped>
.channel-switches {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  width: 100%;
}
.channel-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  min-width: 145px;
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e1e8f6;
  border-radius: 12px;
  color: #374c75;
}
.hint {
  color: #8791a2;
  margin-left: 12px;
  font-size: 12px;
}
p {
  color: #68758a;
  margin-bottom: 24px;
}
.download-settings {
  border: 1px solid #e7ebf4;
  border-radius: 16px;
  padding: 24px;
  background: linear-gradient(125deg, #f8fbff, #fff);
  margin: 24px 0 32px;
}
.section-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}
.section-heading h3 {
  margin: 0 0 10px;
  font-size: 20px;
}
.section-heading p {
  font-size: 14px;
  margin: 0 0 24px;
}
.settings-layout {
  display: flex;
  gap: 40px;
  align-items: flex-start;
}
.download-form {
  flex: 1;
  max-width: 820px;
  min-width: 0;
}
.field-hint {
  display: block;
  width: 100%;
  font-size: 12px;
  color: #8791a2;
  line-height: 1.8;
  margin-top: 6px;
}
.inline-hint {
  font-size: 12px;
  color: #8791a2;
  margin-left: 12px;
}
.qr-panel {
  width: 235px;
  flex-shrink: 0;
  text-align: center;
  border: 1px solid #e5ebf6;
  border-radius: 14px;
  background: #fff;
  padding: 16px;
}
.qr-panel h4 {
  margin: 0 0 12px;
  color: #374c75;
}
.qr-panel img {
  display: block;
  margin: 0 auto;
  width: 160px;
  height: 160px;
}
.qr-address {
  font-size: 12px;
  overflow-wrap: anywhere;
  user-select: all;
  margin: 12px 0;
}
.qr-panel .el-button {
  margin: 4px;
  font-size: 12px;
}
.qr-panel .field-hint {
  margin: 12px 0 0;
  text-align: left;
}
@media (max-width: 1100px) {
  .settings-layout {
    flex-direction: column;
    gap: 16px;
  }
  .download-form {
    width: 100%;
  }
  .qr-panel {
    width: 100%;
    max-width: 350px;
  }
}
@media (max-width: 600px) {
  .download-settings {
    padding: 16px;
  }
  .section-heading {
    flex-direction: column;
  }
  .download-form :deep(.el-form-item) {
    display: block;
  }
  .download-form :deep(.el-form-item__content) {
    margin-left: 0 !important;
  }
  .download-form :deep(.el-form-item__label) {
    float: none;
    justify-content: flex-start;
  }
  .inline-hint {
    margin-left: 8px;
  }
}
</style>
