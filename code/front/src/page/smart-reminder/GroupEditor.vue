<template>
  <GroupDialog
    :open="open"
    :title="t(creating ? '创建群组' : '群组资料')"
    :busy="saving"
    variant="group-compact"
    :fixed-footer="true"
    :panel-height="456"
    @close="emit('close')"
  >
    <div class="editor-summary">
      <div class="avatar-column">
        <span class="app-font field-title">{{ t('群组头像') }}</span>
        <div class="upload-cover" @click="pickAvatar()">
          <img class="group-picture" :src="asset(avatar)" />
        </div>
        <span class="app-font avatar-hint" @click="pickAvatar()">{{
          t(saving ? '上传中…' : '点击头像更换')
        }}</span>
      </div>
      <div class="fields-column">
        <span class="app-font field-title"
          >{{ t('群组名称') }}<span class="required"> *</span></span
        >
        <input
          class="app-font name-input"
          v-model="name"
          maxlength="60"
          :placeholder="t('请输入群组名称')"
        />
        <span class="app-font field-title intro-title">{{ t('群组介绍') }}</span>
        <div class="intro-field">
          <textarea
            class="app-font intro-input"
            v-model="introduction"
            maxlength="200"
            :placeholder="t('请输入群组介绍')"
          /><span class="app-font counter">{{ introduction.length }}/200</span>
        </div>
      </div>
    </div>
    <span class="app-font field-title avatar-title">{{ t('选择头像') }}</span>
    <div class="avatar-grid">
      <div
        v-for="url in groupAvatars"
        :key="url"
        class="avatar-cell"
        @click="!saving && (avatar = url)"
      >
        <div :class="['avatar-border', avatar == url ? 'avatar-selected' : '']">
          <img class="preset-image" :src="asset(url)" />
          <div v-if="avatar == url" class="selected-dot">
            <UiIcon name="check" tone="white" :size="18" />
          </div>
        </div>
      </div>
    </div>
    <span v-if="error.length > 0" class="app-font error">{{ t(error) }}</span>
    <template #footer
      ><div class="form-actions">
        <button class="app-font secondary form-button" :disabled="saving" @click="emit('close')">
          {{ t('取消') }}</button
        ><button class="app-font primary form-button" :disabled="saving" @click="save()">
          {{ t(creating ? '创建' : '保存') }}
        </button>
      </div></template
    >
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/webp,image/gif"
      hidden
      @change="uploadSelected"
    />
  </GroupDialog>
</template>
<script setup>
import { ref, watch } from 'vue';
import GroupDialog from './GroupDialog.vue';
import UiIcon from './UiIcon.vue';
import { mt as t } from './mobileLocale';
import { groupRequest, uploadAvatar } from '@/api/smartReminder';
import { groupAvatars } from './groupDesign';
const props = defineProps({ open: Boolean, group: Object, creating: Boolean }),
  emit = defineEmits(['close', 'saved']);
const name = ref(''),
  avatar = ref(groupAvatars[0]),
  introduction = ref(''),
  error = ref(''),
  saving = ref(false),
  fileInput = ref(null),
  asset = (url) => url;
watch(
  () => props.open,
  (open) => {
    if (!open) return;
    name.value = props.creating ? '' : props.group?.name || '';
    avatar.value = props.creating
      ? groupAvatars[Math.floor(Math.random() * groupAvatars.length)]
      : props.group?.avatar || groupAvatars[0];
    introduction.value = props.creating ? '' : props.group?.introduction || '';
    error.value = '';
  },
);
function pickAvatar() {
  if (!saving.value) fileInput.value?.click();
}
async function uploadSelected(e) {
  const file = e.target.files?.[0];
  e.target.value = '';
  if (!file || saving.value) return;
  if (
    !['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type) ||
    file.size > 2 * 1024 * 1024
  ) {
    error.value = t('请选择 2MB 以内的 JPG、PNG、GIF 或 WEBP 图片');
    return;
  }
  saving.value = true;
  error.value = '';
  try {
    const result = (await uploadAvatar(file)).data.data;
    const url = result?.link || result?.url;
    if (!url) throw Error(t('上传未返回头像地址'));
    avatar.value = url;
  } catch (e) {
    error.value = e.message;
  } finally {
    saving.value = false;
  }
}
async function save() {
  if (saving.value) return;
  if (!name.value.trim()) {
    error.value = t('请输入群组名称');
    return;
  }
  saving.value = true;
  error.value = '';
  try {
    const result = await groupRequest(props.creating ? 'create' : 'save', {
      groupId: props.creating ? null : props.group.id,
      name: name.value.trim(),
      avatar: avatar.value,
      introduction: introduction.value.trim(),
      groupType: 'CUSTOM',
    });
    emit('saved', result.data.data);
  } catch (e) {
    error.value = e.message || t('保存失败，请重试');
  } finally {
    saving.value = false;
  }
}
</script>
<style scoped>
.editor-summary {
  flex-direction: row;
  padding: 13px 10px;
  border: 1px solid #e6ebff;
  border-radius: 15px;
  background-color: #f8faff;
}
.avatar-column {
  width: 92px;
  align-items: center;
  margin-right: 12px;
}
.field-title {
  font-size: 14px;
  font-weight: 600;
  color: #142146;
  line-height: 22px;
}
.required {
  color: #ee7f8b;
  font-size: 12px;
}
.upload-cover {
  width: 88px;
  height: 88px;
  border: 2px solid #fff;
  border-radius: 20px;
  margin-top: 9px;
  overflow: hidden;
}
.group-picture {
  width: 100%;
  height: 100%;
}
.avatar-hint {
  font-size: 10px;
  color: #8b97b6;
  margin-top: 6px;
}
.field-title .required {
  display: inline;
}
.fields-column {
  flex: 1;
  min-width: 0;
}
.name-input {
  width: 100%;
  height: 39px;
  border: 1px solid #dce4ff;
  border-radius: 10px;
  font-size: 12px;
  padding: 0 9px;
  margin-top: 5px;
  background-color: #ffffff80;
}
.intro-title {
  margin-top: 10px;
}
.intro-field {
  border: 1px solid #dce4ff;
  border-radius: 10px;
  padding: 8px;
  margin-top: 5px;
  background-color: #ffffff80;
}
.intro-input {
  width: 100%;
  height: 48px;
  font-size: 12px;
  line-height: 19px;
}
.counter {
  font-size: 10px;
  color: #929db9;
  text-align: right;
}
.avatar-title {
  margin-top: 16px;
  margin-bottom: 8px;
}
.avatar-grid {
  flex-direction: row;
  flex-wrap: wrap;
  margin-left: -4px;
  margin-right: -4px;
}
.avatar-cell {
  width: 33.333%;
  padding: 4px;
}
.avatar-border {
  width: 100%;
  height: 87px;
  border: 2px solid transparent;
  border-radius: 17px;
  padding: 2px;
  position: relative;
  overflow: visible;
}
.avatar-selected {
  border-color: #176aff;
}
.preset-image {
  width: 100%;
  height: 100%;
  border-radius: 13px;
}
.selected-dot {
  position: absolute;
  right: -4px;
  top: -4px;
  width: 23px;
  height: 23px;
  border: 2px solid #fff;
  border-radius: 13px;
  background-color: #176aff;
  align-items: center;
  justify-content: center;
}
.form-actions {
  flex-direction: row;
}
.form-button {
  flex: 1;
  height: 44px;
  min-height: 44px;
  line-height: 44px;
  padding: 0;
  margin: 0;
  border-radius: 14px;
  font-size: 16px;
}
.form-button + .form-button {
  margin-left: 10px;
}

.field-title .required {
  display: inline;
}
.fields-column {
  display: flex;
  flex-direction: column;
}
.editor-summary .avatar-column {
  flex-shrink: 0;
}
.name-input {
  flex-shrink: 0;
}
.intro-input {
  border: 0;
  resize: none;
  background: transparent;
  outline: none;
  padding: 0;
}
.group-picture,
.preset-image {
  object-fit: cover;
}
.avatar-border,
.upload-cover,
.avatar-cell,
.selected-dot {
  cursor: pointer;
}
.avatar-border {
  height: auto;
  aspect-ratio: 1.22;
}
.editor-summary .field-title {
  white-space: nowrap;
}
.group-picture {
  display: block;
}
.group-dialog-body .avatar-cell {
  width: 33.333%;
}
.counter {
  text-align: right;
}
.form-button + .form-button {
  margin-left: 0;
}
.avatar-caption {
  cursor: pointer;
}
.editor-summary {
  padding: 10px 8px;
  border-radius: 13px;
}
.avatar-column {
  width: 76px;
  margin-right: 10px;
}
.field-title {
  font-size: 13px;
  line-height: 20px;
}
.upload-cover {
  width: 72px;
  height: 72px;
  border-radius: 17px;
  margin-top: 7px;
}
.name-input {
  height: 35px;
  font-size: 12px;
}
.intro-title {
  margin-top: 8px;
}
.intro-input {
  height: 32px;
  line-height: 17px;
}
.intro-field {
  padding: 7px;
}
.avatar-title {
  margin-top: 12px;
  margin-bottom: 5px;
}
.avatar-cell {
  padding: 3px;
}
.avatar-border {
  height: 65px;
  border-radius: 13px;
}
.preset-image {
  border-radius: 10px;
}
.selected-dot {
  width: 20px;
  height: 20px;
  border-radius: 11px;
}
.form-button {
  height: 40px;
  min-height: 40px;
  line-height: 40px;
  font-size: 14px;
  border-radius: 12px;
}
.avatar-border {
  width: 70px;
  height: 70px;
  align-self: center;
}
.editor-summary {
  padding: 8px;
}
.intro-title {
  margin-top: 6px;
}
.name-input {
  height: 34px;
}
.avatar-cell {
  padding: 2px;
}
.avatar-title {
  margin-top: 8px;
  margin-bottom: 4px;
}
.avatar-grid {
  margin-left: 0;
  margin-right: 0;
}
.avatar-cell {
  width: 20%;
  padding: 2px;
}
.avatar-border {
  width: 100%;
  height: 54px;
  align-self: center;
  aspect-ratio: auto;
}
.preset-image {
  border-radius: 9px;
  object-fit: contain;
}
.selected-dot {
  width: 17px;
  height: 17px;
  border-radius: 10px;
  right: -2px;
  top: -2px;
}
</style>
