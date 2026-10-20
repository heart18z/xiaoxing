<template>
  <div class="groups-area">
    <div class="group-heading">
      <div class="grow">
        <span class="app-font group-title">{{ t('我的组织') }}</span>
        <span class="app-font group-help">{{ t('按成员标签，灵活发起提醒') }}</span>
      </div>
      <button class="app-font primary create-group" @click="emit('create')">
        ＋ {{ t('创建组织') }}
      </button>
    </div>
    <div class="group-scroll">
      <div v-if="error.length > 0" class="group-empty">
        <span class="app-font error">{{ t(error) }}</span>
        <button class="app-font secondary" @click="emit('retry')">{{ t('重新加载') }}</button>
      </div>
      <div v-else-if="rows.length == 0" class="group-empty">
        <UiIcon name="people" tone="blue" :size="36" />
        <span class="app-font empty-title">{{ t(loading ? '正在读取…' : '还没有群组') }}</span>
        <span class="app-font group-help">{{ t('创建群组、邀请好友，为成员设置多个标签') }}</span>
      </div>
      <div v-for="group in rows" :key="s(group, 'id')" class="group-row" @click="open(group)">
        <img class="group-avatar" :src="asset(s(group, 'avatar', groupAvatars[0]))" />
        <div class="group-main">
          <span class="app-font row-name">{{ s(group, 'name') }}</span>
          <span class="app-font row-description">
            {{ s(group, 'introduction').length > 0 ? s(group, 'introduction') : t('自定义群组') }}
          </span>
          <div class="group-meta">
            <span class="app-font" :class="['role-chip', 'role-' + s(group, 'myRole')]">
              {{ t(groupRoleLabel(s(group, 'myRole'))) }}
            </span>
            <div class="member-previews">
              <img
                v-for="(person, index) in previews(group)"
                :key="s(person, 'id')"
                :class="['preview-avatar', index == 0 ? 'preview-first' : '']"
                :src="asset(s(person, 'avatar', '/avatars/user/B1.png'))"
              />
              <div class="preview-more">
                <span class="app-font preview-number">+{{ remaining(group) }}</span>
              </div>
            </div>
            <span class="app-font member-separator">|</span>
            <span class="app-font member-count">{{ n(group, 'memberCount') }} {{ t('人') }}</span>
          </div>
        </div>
        <div class="group-open" @click.stop="open(group)">
          <UiIcon name="chevron" tone="ink" :size="20" />
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { useRouter } from 'vue-router';
import { mt as t } from './mobileLocale';
import UiIcon from './UiIcon.vue';
import { groupAvatars } from './groupDesign';
defineProps({
  rows: { type: Array, default: () => [] },
  loading: Boolean,
  error: { type: String, default: '' },
});
const emit = defineEmits(['create', 'retry']),
  router = useRouter();
const s = (o, k, d = '') => String(o?.[k] ?? d),
  n = (o, k) => Number(o?.[k] || 0),
  arr = (o, k) => o?.[k] || [],
  asset = (url) => url,
  groupRoleLabel = (r) => ({ OWNER: '我创建的', ADMIN: '我管理的', MEMBER: '我加入的' })[r];
const previews = (group) => arr(group, 'memberPreviews').slice(0, 4);
const remaining = (group) => Math.max(0, n(group, 'memberCount') - previews(group).length);
const open = (group) => router.push('/app/group/' + group.id);
</script>
<style scoped>
:where(.groups-area, .groups-area div) {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.groups-area span {
  display: block;
}
.groups-area .grow {
  flex: 1;
  min-width: 0;
}
.groups-area button {
  border: 0;
  cursor: pointer;
}
.groups-area .primary {
  color: #fff;
  background: linear-gradient(115deg, #365dff, #6a7cff);
  font-family: inherit;
}
.group-scroll {
  overflow-y: auto;
  overscroll-behavior: contain;
}
.group-row,
.group-open {
  cursor: pointer;
}
.group-avatar,
.preview-avatar {
  object-fit: cover;
}
.row-description {
  display: block;
  width: 100%;
}

.groups-area {
  width: 100%;
  min-width: 0;
  flex: 1;
  min-height: 0;
}
.group-heading {
  flex-direction: row;
  align-items: center;
  padding: 1px 2px 14px;
}
.group-title {
  font-size: 17px;
  font-weight: 600;
  color: #142146;
}
.group-help {
  font-size: 11px;
  line-height: 18px;
  color: #8996b0;
  margin-top: 3px;
}
.create-group {
  height: 40px;
  min-height: 40px;
  line-height: 40px;
  padding: 0 13px;
  margin: 0 0 0 6px;
  font-size: 13px;
  border-radius: 14px;
}
.group-scroll {
  flex: 1;
  height: 0;
  min-height: 0;
}
.group-row {
  flex-direction: row;
  align-items: center;
  padding: 13px 11px;
  margin-bottom: 10px;
  border-radius: 24px;
  background-color: #fffffff2;
  border: 1px solid #fff;
}
.group-avatar {
  width: 64px;
  height: 64px;
  border-radius: 17px;
  margin-right: 11px;
  flex-shrink: 0;
  background-color: #edf0ff;
}
.group-main {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.row-name {
  font-size: 15px;
  font-weight: 600;
  color: #152448;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.row-description {
  font-size: 11px;
  line-height: 17px;
  height: 17px;
  color: #8794ad;
  margin-top: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.group-meta {
  flex-direction: row;
  align-items: center;
  margin-top: 7px;
}
.role-chip {
  font-size: 10px;
  line-height: 17px;
  padding: 2px 7px;
  border-radius: 10px;
  flex-shrink: 0;
  margin-right: 6px;
}
.role-OWNER {
  color: #326bf3;
  background-color: #e8f0ff;
}
.role-ADMIN {
  color: #8a56df;
  background-color: #f0e9ff;
}
.role-MEMBER {
  color: #169c72;
  background-color: #e2f6ec;
}
.member-previews {
  flex-direction: row;
  align-items: center;
  flex-shrink: 0;
  overflow: visible;
  padding: 1px;
}
.preview-avatar {
  width: 24px;
  height: 24px;
  border: 2px solid #fff;
  border-radius: 13px;
  margin-left: -6px;
  flex-shrink: 0;
}
.preview-first {
  margin-left: 0;
}
.preview-more {
  width: 25px;
  height: 25px;
  margin-left: 1px;
  border-radius: 13px;
  background-color: #eef2ff;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.preview-number {
  color: #7e8eb9;
  font-size: 10px;
  font-weight: 600;
}
.member-separator {
  font-size: 12px;
  color: #c2cadd;
  margin: 0 6px;
  flex-shrink: 0;
}
.member-count {
  font-size: 11px;
  color: #8996b0;
  flex-shrink: 0;
}
.group-open {
  width: 20px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin-left: 6px;
  padding: 8px 0;
}
.group-empty {
  align-items: center;
  padding: 34px 14px;
  border-radius: 22px;
  background-color: #ffffffd9;
}
.empty-title {
  font-size: 15px;
  font-weight: 600;
  color: #52617c;
  margin-top: 13px;
}
@media (max-width: 360px) {
  .group-avatar {
    width: 50px;
    height: 50px;
    margin-right: 8px;
  }
  .role-chip {
    font-size: 9px;
    padding: 2px 5px;
    margin-right: 4px;
  }
  .preview-avatar {
    width: 20px;
    height: 20px;
  }
  .preview-more {
    width: 22px;
    height: 22px;
  }
  .member-count {
    font-size: 10px;
  }
  .member-separator {
    margin: 0 4px;
  }
}
.group-title,
.row-name {
  font-weight: 700;
}
.create-group,
.role-chip {
  font-weight: 600;
}
</style>
