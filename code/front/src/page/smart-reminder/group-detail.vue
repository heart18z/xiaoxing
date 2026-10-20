<template>
  <div class="group-page">
    <AppShell class="group-shell" variant="profile" :back="true">
      <div class="group-screen">
        <div class="group-nav">
          <div class="nav-circle" @click="goBack()">
            <UiIcon name="chevron" tone="blue" :size="23" class="back-icon" />
          </div>
          <span class="app-font nav-title">{{ t('群组详情') }}</span>
          <div class="nav-spacer" aria-hidden="true" />
        </div>
        <div class="detail-scroll">
          <div class="detail-body">
            <div v-if="error.length > 0" class="surface">
              <span class="app-font error">{{ t(error) }}</span>
              <button class="app-font secondary" @click="load()">{{ t('重新加载') }}</button>
            </div>
            <div v-else-if="s(group, 'id').length > 0">
              <div class="surface hero-card">
                <div class="hero-row">
                  <div class="hero-picture" @click="manager && (editing = true)">
                    <img class="hero-avatar" :src="asset(s(group, 'avatar', groupAvatars[0]))" />
                    <span class="app-font avatar-caption">
                      {{ t(manager ? '点击头像可更换' : '群组头像') }}
                    </span>
                  </div>
                  <div class="hero-copy">
                    <div class="hero-title-row" @click="manager && (editing = true)">
                      <span class="app-font hero-name">{{ s(group, 'name') }}</span>
                      <div v-if="manager" class="pencil-dot">
                        <UiIcon name="edit" tone="muted" :size="15" />
                      </div>
                    </div>
                    <div class="hero-title-row intro-row" @click="manager && (editing = true)">
                      <span class="app-font introduction">
                        {{
                          s(group, 'introduction').length > 0
                            ? s(group, 'introduction')
                            : t('暂无介绍')
                        }}
                      </span>
                      <div v-if="manager" class="pencil-dot">
                        <UiIcon name="edit" tone="muted" :size="14" />
                      </div>
                    </div>
                  </div>
                </div>
                <div class="hero-actions">
                  <div v-if="role == 'OWNER'" class="hero-action" @click="openInvite()">
                    <UiIcon name="userPlus" tone="blue" :size="22" />
                    <span class="app-font hero-action-text">{{ t('邀请好友') }}</span>
                  </div>
                  <div v-if="role == 'OWNER'" class="hero-action" @click="openAdmins()">
                    <UiIcon name="settings" tone="blue" :size="21" />
                    <span class="app-font hero-action-text">{{ t('设置管理员') }}</span>
                  </div>
                  <div v-if="role == 'OWNER'" class="hero-action action-danger" @click="dissolve()">
                    <UiIcon name="trash" tone="red" :size="21" />
                    <span class="app-font hero-action-text danger-text">{{ t('解散群') }}</span>
                  </div>
                  <div v-else class="hero-action" @click="editSelfTags()">
                    <UiIcon name="tag" tone="blue" :size="21" />
                    <span class="app-font hero-action-text">{{ t('我的标签') }}</span>
                  </div>
                </div>
              </div>
              <div class="surface tags-card">
                <div class="section-heading">
                  <UiIcon name="layers" tone="blue" :size="24" />
                  <span class="app-font section-title grow">{{ t('群组标签') }}</span>
                  <div v-if="manager" class="unified-action" @click="openCatalog()">
                    <UiIcon name="settings" tone="blue" :size="15" />
                    <span class="app-font unified-text">{{ t('统一设置') }}</span>
                    <UiIcon name="chevron" :size="14" />
                  </div>
                </div>
                <div class="tag-wrap group-tags">
                  <div
                    v-for="tag in tags"
                    :key="s(tag, 'id')"
                    :class="['tag-pill', groupTagTone(s(tag, 'name'))]"
                    @click="manager && editCatalogTag(tag)"
                  >
                    <span class="app-font tag-label">{{ s(tag, 'name') }}</span>
                    <span class="app-font tag-count">{{ n(tag, 'memberCount') }}</span>
                  </div>
                  <span v-if="tags.length == 0" class="app-font small-muted">
                    {{ t('暂无标签') }}
                  </span>
                </div>
              </div>
              <div class="surface members-card">
                <div class="section-heading member-heading">
                  <UiIcon name="people" tone="blue" :size="24" />
                  <span class="app-font section-title">{{ t('群员列表') }}</span>
                  <span class="app-font count-text">{{ members.length }} {{ t('人') }}</span>
                </div>
                <div v-for="person in members" :key="s(person, 'id')" class="member-row">
                  <img
                    class="member-avatar"
                    :src="asset(s(person, 'avatar', '/avatars/user/B1.png'))"
                  />
                  <div class="member-info">
                    <div class="member-top">
                      <span class="app-font member-name grow">{{ groupPersonName(person) }}</span>
                      <div v-if="canEdit(person)" class="row-action" @click="openPerson(person)">
                        <UiIcon name="edit" tone="blue" :size="12" />
                        <span class="app-font row-action-text">{{ t('编辑') }}</span>
                      </div>
                      <div
                        v-if="role == 'OWNER' && s(person, 'memberRole') != 'OWNER'"
                        class="row-action row-danger"
                        @click="removePerson(person)"
                      >
                        <UiIcon name="userMinus" tone="red" :size="12" />
                        <span class="app-font row-action-text danger-text">
                          {{ t('移出群组') }}
                        </span>
                      </div>
                    </div>
                    <span class="app-font small-muted">@{{ s(person, 'account') }}</span>
                    <div class="tag-wrap">
                      <div
                        v-if="s(person, 'memberRole') == 'OWNER'"
                        class="tag-pill compact-pill tone-4"
                      >
                        <span class="app-font tag-label">♔ {{ t('群主') }}</span>
                      </div>
                      <div
                        v-if="s(person, 'memberRole') == 'ADMIN'"
                        class="tag-pill compact-pill tone-0"
                      >
                        <span class="app-font tag-label">{{ t('管理员') }}</span>
                      </div>
                      <div
                        v-for="tag in arr(person, 'tags')"
                        :key="s(tag, 'id')"
                        :class="['tag-pill', 'compact-pill', groupTagTone(s(tag, 'name'))]"
                      >
                        <span class="app-font tag-label">{{ s(tag, 'name') }}</span>
                      </div>
                      <span
                        v-if="
                          arr(person, 'tags').length == 0 && s(person, 'memberRole') == 'MEMBER'
                        "
                        class="app-font no-tag"
                      >
                        {{ t('未设置标签') }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
              <button v-if="role != 'OWNER'" class="app-font danger bottom-action" @click="leave()">
                {{ t('退出群组') }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </AppShell>
    <GroupEditor :open="editing" :group="group" @close="editing = false" @saved="metadataSaved" />
    <GroupDialog
      :open="inviting"
      :title="t('邀请好友')"
      variant="group"
      :busy="saving"
      :fixed-footer="true"
      :panel-height="620"
      @close="inviting = false"
    >
      <div class="search-box">
        <UiIcon name="search" :size="21" />
        <input
          class="app-font search-input"
          v-model="inviteQuery"
          :placeholder="t('搜索好友名称或账号')"
        />
      </div>
      <div class="selection-count">
        <span class="app-font small-muted">{{ t('已选择') }}</span>
        <span class="app-font selection-number">{{ inviteIds.length }}</span>
        <span class="app-font small-muted">{{ t('人') }}</span>
      </div>
      <span v-if="friendsLoading" class="app-font small-muted">{{ t('正在读取…') }}</span>
      <span v-else-if="availableFriends.length == 0" class="app-font small-muted">
        {{ t('暂无可邀请好友') }}
      </span>
      <div
        v-for="friend in availableFriends"
        :key="s(friend, 'id')"
        :class="['picker-row', canInviteFriend(friend) ? '' : 'invite-disabled']"
        @click="toggleInvite(s(friend, 'id'))"
      >
        <img class="picker-avatar" :src="asset(s(friend, 'avatar', '/avatars/user/B1.png'))" />
        <div class="grow">
          <span class="app-font member-name">{{ groupPersonName(friend) }}</span>
          <span class="app-font small-muted">@{{ s(friend, 'account') }}</span>
          <span v-if="!canInviteFriend(friend)" class="app-font invite-reason">
            {{ t('暂无向对方发起提醒的权限') }}
          </span>
        </div>
        <div :class="['check-circle', inviteIds.includes(s(friend, 'id')) ? 'check-selected' : '']">
          <UiIcon v-if="inviteIds.includes(s(friend, 'id'))" name="check" tone="white" :size="18" />
        </div>
      </div>
      <span v-if="formError.length > 0" class="app-font error">{{ t(formError) }}</span>
      <template #footer>
        <div class="form-actions">
          <button
            class="app-font secondary form-button"
            :disabled="saving"
            @click="inviting = false"
          >
            {{ t('取消') }}
          </button>
          <button
            class="app-font primary form-button submit-button"
            :disabled="saving || friendsLoading || inviteIds.length == 0"
            @click="invite()"
          >
            {{ t('发送邀请') }}
          </button>
        </div>
      </template>
    </GroupDialog>
    <GroupDialog
      :open="personOpen"
      :title="t('成员详情')"
      variant="group"
      :busy="saving"
      :fixed-footer="true"
      :panel-height="590"
      @close="personOpen = false"
    >
      <div class="person-profile">
        <img
          class="person-profile-avatar"
          :src="asset(s(selected, 'avatar', '/avatars/user/B1.png'))"
        />
        <div class="grow">
          <span class="app-font person-profile-name">{{ groupPersonName(selected) }}</span>
          <span class="app-font profile-account">@{{ s(selected, 'account') }}</span>
        </div>
      </div>
      <div class="tag-section">
        <div class="section-heading">
          <UiIcon name="tag" tone="blue" :size="19" />
          <span class="app-font modal-section-title">{{ t('当前标签') }}</span>
        </div>
        <div class="tag-wrap">
          <div
            v-for="label in personTags"
            :key="label"
            :class="['tag-pill', groupTagTone(label)]"
            @click="togglePersonTag(label)"
          >
            <span class="app-font tag-label">{{ label }}</span>
            <UiIcon v-if="canEditPerson" name="close" :size="13" />
          </div>
          <span v-if="personTags.length == 0" class="app-font small-muted">
            {{ t('未设置标签') }}
          </span>
        </div>
      </div>
      <div v-if="canEditPerson" class="tag-section">
        <div class="section-heading">
          <UiIcon name="plus" tone="blue" :size="19" />
          <span class="app-font modal-section-title">{{ t('添加标签') }}</span>
        </div>
        <div class="tag-wrap">
          <div
            v-for="tag in availablePersonTags"
            :key="s(tag, 'id')"
            :class="['tag-pill', groupTagTone(s(tag, 'name'))]"
            @click="togglePersonTag(s(tag, 'name'))"
          >
            <UiIcon name="plus" tone="blue" :size="14" />
            <span class="app-font tag-label">{{ s(tag, 'name') }}</span>
          </div>
        </div>
        <div v-if="addingPersonTag" class="new-tag-input">
          <input
            v-focus
            class="app-font new-tag-field grow"
            v-model="newPersonTag"
            maxlength="40"
            :placeholder="t('输入标签内容')"
            @keyup.enter="addPersonTag()"
          />
          <div class="new-tag-done" @click="addPersonTag()">
            <UiIcon name="check" tone="blue" :size="19" />
          </div>
        </div>
        <div v-else class="new-tag-button" @click="addingPersonTag = true">
          <UiIcon name="plus" tone="blue" :size="18" />
          <span class="app-font new-tag-label">{{ t('新建标签') }}</span>
        </div>
      </div>
      <span v-if="formError.length > 0" class="app-font error">{{ t(formError) }}</span>
      <template #footer>
        <div class="form-actions">
          <button
            class="app-font secondary form-button"
            :disabled="saving"
            @click="personOpen = false"
          >
            {{ t('取消') }}
          </button>
          <button
            v-if="canEditPerson"
            class="app-font primary form-button submit-button"
            :disabled="saving"
            @click="savePersonTags()"
          >
            {{ t('保存') }}
          </button>
        </div>
      </template>
    </GroupDialog>
    <GroupDialog
      :open="adminsOpen"
      :title="t('设置管理员')"
      variant="group"
      :busy="saving"
      :fixed-footer="true"
      :panel-height="610"
      @close="adminsOpen = false"
    >
      <div class="admin-note">
        <UiIcon name="tipInfo" tone="blue" :size="18" />
        <span class="app-font admin-note-text">{{ t('仅可从当前群成员中新增管理员') }}</span>
      </div>
      <div class="section-heading admin-heading">
        <span class="app-font crown">♔</span>
        <span class="app-font modal-section-title">{{ t('当前管理员') }}</span>
        <span class="app-font small-muted">{{ currentAdmins.length }} {{ t('人') }}</span>
      </div>
      <div v-for="person in currentAdmins" :key="s(person, 'id')" class="picker-row admin-person">
        <img class="picker-avatar" :src="asset(s(person, 'avatar', '/avatars/user/B1.png'))" />
        <div class="grow">
          <span class="app-font member-name">{{ groupPersonName(person) }}</span>
          <span class="app-font small-muted">@{{ s(person, 'account') }}</span>
        </div>
        <span v-if="s(person, 'memberRole') == 'OWNER'" class="app-font owner-fixed">
          {{ t('群主') }}
        </span>
        <div v-else class="row-action row-danger" @click="toggleAdmin(s(person, 'id'))">
          <UiIcon name="trash" tone="red" :size="14" />
          <span class="app-font row-action-text danger-text">{{ t('移除') }}</span>
        </div>
      </div>
      <div class="section-heading admin-heading admin-divider">
        <UiIcon name="people" tone="blue" :size="21" />
        <span class="app-font modal-section-title">{{ t('选择群成员') }}</span>
        <span class="app-font small-muted">{{ availableAdmins.length }} {{ t('人') }}</span>
      </div>
      <div v-for="person in availableAdmins" :key="s(person, 'id')" class="picker-row admin-person">
        <img class="picker-avatar" :src="asset(s(person, 'avatar', '/avatars/user/B1.png'))" />
        <div class="grow">
          <span class="app-font member-name">{{ groupPersonName(person) }}</span>
          <span class="app-font small-muted">@{{ s(person, 'account') }}</span>
        </div>
        <div class="outline-action" @click="toggleAdmin(s(person, 'id'))">
          <span class="app-font row-action-text">{{ t('设为管理员') }}</span>
        </div>
      </div>
      <span v-if="formError.length > 0" class="app-font error">{{ t(formError) }}</span>
      <template #footer>
        <div class="form-actions">
          <button
            class="app-font secondary form-button"
            :disabled="saving"
            @click="adminsOpen = false"
          >
            {{ t('取消') }}
          </button>
          <button
            class="app-font primary form-button submit-button"
            :disabled="saving"
            @click="saveAdmins()"
          >
            {{ t('保存') }}
          </button>
        </div>
      </template>
    </GroupDialog>
    <GroupDialog
      :open="catalogOpen"
      variant="group"
      :title="t(tagEditing ? '标签与人员' : '标签管理')"
      :busy="saving"
      :fixed-footer="true"
      :panel-height="620"
      @close="catalogOpen = false"
    >
      <div v-if="!tagEditing">
        <span class="app-font intro-note">
          {{ t('先设置标签，再选择对应的多位成员。标签不会授予管理或提醒权限。') }}
        </span>
        <div v-for="tag in tags" :key="s(tag, 'id')" class="catalog-row" @click="editTag(tag)">
          <div class="grow">
            <div :class="['tag-pill', groupTagTone(s(tag, 'name'))]">
              <span class="app-font tag-label">{{ s(tag, 'name') }}</span>
            </div>
          </div>
          <span class="app-font small-muted">{{ n(tag, 'memberCount') }} {{ t('人') }}</span>
          <UiIcon name="chevron" :size="18" />
        </div>
        <span v-if="tags.length == 0" class="app-font small-muted">
          {{ t('暂无标签，点击下方创建') }}
        </span>
      </div>
      <div v-else>
        <input
          class="app-font input"
          v-model="tagName"
          maxlength="40"
          :placeholder="t('例如：消防负责人')"
        />
        <span class="app-font intro-note">
          {{ t('选择拥有该标签的成员') }} · {{ tagPeople.length }}
        </span>
        <div
          v-for="person in members"
          :key="s(person, 'id')"
          class="picker-row"
          @click="toggleTagPerson(s(person, 'id'))"
        >
          <img class="picker-avatar" :src="asset(s(person, 'avatar', '/avatars/user/B1.png'))" />
          <div class="grow">
            <span class="app-font member-name">{{ groupPersonName(person) }}</span>
            <span class="app-font small-muted">@{{ s(person, 'account') }}</span>
          </div>
          <div
            :class="['check-circle', tagPeople.includes(s(person, 'id')) ? 'check-selected' : '']"
          >
            <span v-if="tagPeople.includes(s(person, 'id'))" class="app-font check-mark">✓</span>
          </div>
        </div>
        <button
          v-if="tagId.length > 0"
          class="app-font danger"
          :disabled="saving"
          @click="deleteTag()"
        >
          {{ t('删除此标签') }}
        </button>
      </div>
      <span v-if="formError.length > 0" class="app-font error">{{ t(formError) }}</span>
      <template #footer>
        <button
          class="app-font primary"
          :disabled="saving"
          @click="tagEditing ? saveTag() : editTag({})"
        >
          {{ t(tagEditing ? '保存标签与人员' : '新增标签') }}
        </button>
      </template>
    </GroupDialog>
  </div>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { getFriends, groupRequest } from '@/api/smartReminder';
import AppShell from './AppShell.vue';
import GroupDialog from './GroupDialog.vue';
import UiIcon from './UiIcon.vue';
import GroupEditor from './GroupEditor.vue';
import { mt as t } from './mobileLocale';
import { groupAvatars, groupTagTone } from './groupDesign';
const vFocus = { mounted: (el) => el.focus() };
const route = useRoute(),
  router = useRouter(),
  group = ref({}),
  loading = ref(false),
  saving = ref(false),
  error = ref(''),
  formError = ref(''),
  editing = ref(false),
  inviting = ref(false),
  personOpen = ref(false),
  catalogOpen = ref(false),
  tagEditing = ref(false),
  adminsOpen = ref(false),
  addingPersonTag = ref(false),
  friendsLoading = ref(false),
  adminIds = ref([]),
  friends = ref([]),
  inviteIds = ref([]),
  inviteQuery = ref(''),
  selected = ref({}),
  personTags = ref([]),
  newPersonTag = ref(''),
  tagId = ref(''),
  tagName = ref(''),
  tagPeople = ref([]);
const s = (o, k, d = '') => String(o?.[k] ?? d),
  n = (o, k) => Number(o?.[k] || 0),
  arr = (o, k) => o?.[k] || [],
  asset = (url) => url,
  groupPersonName = (p) => p?.friendRemark || p?.name || p?.realName || p?.account || '',
  groupMemberRole = (r) => ({ OWNER: '群主', ADMIN: '管理员', MEMBER: '群员' })[r] || '群员',
  toggleSelection = (values, value) =>
    values.includes(value) ? values.filter((v) => v !== value) : [...values, value];
const members = computed(() => arr(group.value, 'members')),
  tags = computed(() => arr(group.value, 'tags')),
  role = computed(() => s(group.value, 'myRole')),
  manager = computed(() => ['OWNER', 'ADMIN'].includes(role.value)),
  canEditPerson = computed(() => canEdit(selected.value)),
  availablePersonTags = computed(() =>
    tags.value.filter((tag) => !personTags.value.includes(tag.name)),
  ),
  currentAdmins = computed(() =>
    members.value.filter((p) => p.memberRole === 'OWNER' || adminIds.value.includes(String(p.id))),
  ),
  availableAdmins = computed(() =>
    members.value.filter((p) => p.memberRole !== 'OWNER' && !adminIds.value.includes(String(p.id))),
  ),
  availableFriends = computed(() =>
    friends.value.filter(
      (p) =>
        !members.value.some((m) => String(m.id) === String(p.id)) &&
        (groupPersonName(p) + ' ' + p.account)
          .toLowerCase()
          .includes(inviteQuery.value.trim().toLowerCase()),
    ),
  );
function goBack() {
  router.replace('/app/me');
}
function canEdit(person) {
  return manager.value || !!person.self;
}
function personSummary(person) {
  return (
    t(groupMemberRole(person.memberRole)) +
    (person.tags?.length
      ? ' · ' +
        person.tags
          .slice(0, 2)
          .map((t) => t.name)
          .join('、')
      : '')
  );
}
async function load() {
  loading.value = !group.value.id;
  try {
    group.value = (await groupRequest('detail', null, { groupId: route.params.id })).data.data;
    error.value = '';
  } catch (e) {
    error.value = e.message || t('读取失败，请重试');
  } finally {
    loading.value = false;
  }
}
async function mutate(action, data) {
  if (saving.value) return false;
  saving.value = true;
  formError.value = '';
  try {
    const result = (await groupRequest(action, { ...data, groupId: route.params.id })).data.data;
    if (!['member/remove', 'dissolve'].includes(action)) group.value = result;
    return true;
  } catch (e) {
    formError.value = e.message || t('操作失败，请重试');
    ElMessage.error(formError.value);
    return false;
  } finally {
    saving.value = false;
  }
}
async function ask(title, text) {
  try {
    await ElMessageBox.confirm(t(text), t(title), {
      confirmButtonText: t('确定'),
      cancelButtonText: t('取消'),
      type: 'warning',
    });
    return true;
  } catch {
    return false;
  }
}
function metadataSaved(value) {
  group.value = value;
  editing.value = false;
  ElMessage.success(t('群组资料已保存'));
}
async function openInvite() {
  if (role.value !== 'OWNER') return;
  inviteIds.value = [];
  inviteQuery.value = '';
  formError.value = '';
  friends.value = [];
  friendsLoading.value = true;
  inviting.value = true;
  try {
    friends.value = (await getFriends()).data.data || [];
  } catch (e) {
    formError.value = e.message;
  } finally {
    friendsLoading.value = false;
  }
}
function canInviteFriend(person) {
  return ['MUTUAL', 'I_CAN_REMIND'].includes(s(person, 'permissionMode'));
}
function toggleInvite(id) {
  const person = friends.value.find((p) => s(p, 'id') == id);
  if (saving.value || person == null || !canInviteFriend(person)) return;
  if (!saving.value) inviteIds.value = toggleSelection(inviteIds.value, id);
}
async function invite() {
  if (!inviteIds.value.length) return;
  if (await mutate('invite', { userIds: inviteIds.value })) {
    inviting.value = false;
    ElMessage.success(t('好友已加入群组'));
  }
}
function openPerson(person) {
  selected.value = person;
  personTags.value = (person.tags || []).map((t) => t.name);
  newPersonTag.value = '';
  addingPersonTag.value = false;
  formError.value = '';
  personOpen.value = true;
}
function editSelfTags() {
  const person = members.value.find((p) => p.self);
  if (person) openPerson(person);
}
function togglePersonTag(name) {
  if (canEditPerson.value && !saving.value)
    personTags.value = toggleSelection(personTags.value, name);
}
function addPersonTag() {
  const value = newPersonTag.value.trim();
  if (!value) return;
  if (!personTags.value.includes(value)) personTags.value.push(value);
  newPersonTag.value = '';
  addingPersonTag.value = false;
}
async function savePersonTags() {
  addPersonTag();
  if (await mutate('member/tags', { userId: selected.value.id, tags: personTags.value })) {
    personOpen.value = false;
    ElMessage.success(t('标签已保存'));
  }
}
function openAdmins() {
  adminIds.value = members.value.filter((p) => p.memberRole === 'ADMIN').map((p) => String(p.id));
  formError.value = '';
  adminsOpen.value = true;
}
function toggleAdmin(id) {
  if (!saving.value) adminIds.value = toggleSelection(adminIds.value, id);
}
async function saveAdmins() {
  if (await mutate('admins', { userIds: adminIds.value })) {
    adminsOpen.value = false;
    ElMessage.success(t('管理员已更新'));
  }
}
async function removePerson(person) {
  if (
    (await ask('移出群组', '移出后，该成员不再参与新的群组选人，已创建事件保留。')) &&
    (await mutate('member/remove', { userId: person.id }))
  ) {
    await load();
  }
}
function openCatalog() {
  catalogOpen.value = true;
  tagEditing.value = false;
  formError.value = '';
}
function editCatalogTag(tag) {
  catalogOpen.value = true;
  editTag(tag);
}
function editTag(tag) {
  tagId.value = s(tag, 'id');
  tagName.value = s(tag, 'name');
  tagPeople.value = members.value
    .filter((p) => p.tags.some((t) => String(t.id) === tagId.value))
    .map((p) => String(p.id));
  tagEditing.value = true;
  formError.value = '';
}
function toggleTagPerson(id) {
  if (!saving.value) tagPeople.value = toggleSelection(tagPeople.value, id);
}
async function saveTag() {
  if (!tagName.value.trim()) {
    formError.value = t('请输入标签名称');
    return;
  }
  if (
    await mutate('tags/save', {
      tagId: tagId.value || null,
      name: tagName.value.trim(),
      userIds: tagPeople.value,
    })
  ) {
    tagEditing.value = false;
    ElMessage.success(t('标签已保存'));
  }
}
async function deleteTag() {
  if (
    (await ask('删除标签', '此标签将从群组所有成员中移除，人员与历史事件保留。')) &&
    (await mutate('tags/delete', { tagId: tagId.value }))
  )
    tagEditing.value = false;
}
async function dissolve() {
  if (saving.value) return;
  if (
    (await ask(
      '解散群组',
      '确定解散这个群组吗？解散后不可恢复，成员不能再通过它发起提醒，已创建事件继续保留和处理。',
    )) &&
    (await mutate('dissolve', {}))
  ) {
    ElMessage.success(t('群组已解散'));
    goBack();
  }
}
async function leave() {
  const person = members.value.find((p) => p.self);
  if (
    person &&
    (await ask('退出群组', '退出后不能再查看该群组或以它发起新提醒，已创建事件保留。')) &&
    (await mutate('member/remove', { userId: person.id }))
  )
    goBack();
}
onMounted(load);
</script>
<style scoped>
.group-page {
  height: 100dvh;
}
.group-shell :deep(.sr-main) {
  padding: max(10px, env(safe-area-inset-top)) 0 max(0px, env(safe-area-inset-bottom));
  height: 100dvh;
  min-height: 0;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.group-screen {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}
:where(.group-screen div) {
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}
.group-screen span {
  display: block;
}
.group-screen img {
  object-fit: cover;
  flex-shrink: 0;
}
.detail-scroll {
  overflow-y: auto;
  overscroll-behavior: contain;
}
.nav-circle,
.hero-action,
.unified-action,
.row-action,
.outline-action,
.new-tag-button,
.new-tag-done,
.tag-pill,
.pencil-dot {
  cursor: pointer;
}
.hero-avatar,
.person-profile-avatar,
.picker-avatar {
  object-fit: cover;
}
.error {
  font-size: 12px;
  color: #ea6576;
}
.form-actions button {
  border: 0;
}
.new-tag-field,
.search-input {
  border: 0;
  outline: none;
  background: none;
}
.tag-pill span {
  display: block;
}
.bottom-action {
  border: 1px solid #ffdfe5;
  background: #fff1f4;
  color: #f65b70;
  width: 100%;
  border-radius: 15px;
  min-height: 42px;
  font: inherit;
}
.tag-label,
.tag-count {
  color: inherit;
}
.group-dialog-body .grow {
  flex: 1;
  min-width: 0;
}
.group-dialog-body .ui-icon {
  flex-shrink: 0;
}
.new-tag-input input {
  width: 0;
}
.check-circle {
  flex-shrink: 0;
}
.hero-copy {
  min-width: 0;
}
.hero-name {
  overflow-wrap: anywhere;
}

.group-nav {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  padding: 10px 19px 12px;
  flex-shrink: 0;
}
.nav-circle {
  width: 36px;
  height: 36px;
  border-radius: 20px;
  background-color: #ffffffb5;
  align-items: center;
  justify-content: center;
}
.back-icon {
  transform: rotate(180deg);
}
.nav-title {
  font-size: 18px;
  font-weight: 600;
  color: #10204a;
}
.more-glyph {
  font-size: 18px;
  letter-spacing: 1px;
  color: #164bc9;
}
.detail-scroll {
  flex: 1;
  height: 0;
}
.detail-body {
  padding: 0 14px 24px;
}
.surface {
  border: 1px solid #fff;
  border-radius: 25px;
  padding: 14px 12px;
  background-color: #fffffff2;
  margin-bottom: 12px;
}
.hero-row {
  flex-direction: row;
  align-items: flex-start;
}
.hero-picture {
  width: 109px;
  align-items: center;
  margin-right: 14px;
}
.hero-avatar {
  width: 109px;
  height: 109px;
  border-radius: 22px;
  background-color: #edf3ff;
}
.avatar-caption {
  font-size: 11px;
  color: #8492b1;
  margin-top: 8px;
}
.hero-copy {
  flex: 1;
  min-width: 0;
  padding-top: 10px;
}
.hero-title-row {
  flex-direction: row;
  align-items: center;
}
.hero-name {
  font-size: 21px;
  font-weight: 600;
  line-height: 29px;
  color: #112046;
  flex-shrink: 1;
}
.pencil-dot {
  width: 22px;
  height: 22px;
  border-radius: 12px;
  background-color: #f0f3fa;
  align-items: center;
  justify-content: center;
  margin-left: 7px;
  flex-shrink: 0;
}
.intro-row {
  margin-top: 11px;
  align-items: flex-start;
}
.introduction {
  font-size: 12px;
  line-height: 20px;
  color: #8290ad;
  flex-shrink: 1;
}
.hero-actions {
  flex-direction: row;
  margin-top: 16px;
}
.hero-action {
  flex: 1;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  border: 1px solid #e0eaff;
  background-color: #f5f9ff;
  border-radius: 11px;
  margin-right: 6px;
  padding: 6px 3px;
}
.hero-action-text {
  font-size: 11px;
  font-weight: 600;
  color: #24365f;
  margin-left: 5px;
}
.action-danger {
  background-color: #fff5f6;
  border-color: #ffe0e4;
  margin-right: 0;
}
.danger-text {
  color: #fa536a;
}
.section-heading {
  flex-direction: row;
  align-items: center;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #152448;
  margin-left: 8px;
}
.unified-action {
  flex-direction: row;
  align-items: center;
  border: 1px solid #e0eaff;
  background-color: #f1f6ff;
  border-radius: 10px;
  padding: 5px 7px;
}
.unified-text {
  font-size: 11px;
  color: #1766ff;
  margin: 0 5px;
}
.tag-wrap {
  flex-direction: row;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 5px;
}
.group-tags {
  padding-left: 30px;
  margin-top: 9px;
}
.tag-pill {
  flex-direction: row;
  align-items: center;
  border-radius: 18px;
  padding: 6px 10px;
  margin-right: 6px;
  margin-bottom: 6px;
  background-color: #eaf2ff;
  color: #1c6bfa;
}
.tag-label {
  font-size: 12px;
  color: inherit;
}
.tag-count {
  font-size: 10px;
  color: inherit;
  margin-left: 7px;
  background-color: #ffffff90;
  border-radius: 8px;
  padding: 1px 4px;
}
.tone-0 {
  background-color: #e7f1ff;
  color: #1766ff;
}
.tone-1 {
  background-color: #f0e6ff;
  color: #9446e8;
}
.tone-2 {
  background-color: #e5f7ea;
  color: #149951;
}
.tone-3 {
  background-color: #fff0f2;
  color: #ef536b;
}
.tone-4 {
  background-color: #fff4e2;
  color: #dc9217;
}
.member-heading {
  margin-bottom: 11px;
}
.count-text {
  font-size: 13px;
  color: #8492b1;
  margin-left: 8px;
}
.member-row {
  flex-direction: row;
  align-items: flex-start;
  border: 1px solid #edf3fc;
  border-radius: 15px;
  padding: 10px 8px;
  margin-bottom: 7px;
  background-color: #fcfdff;
}
.member-avatar {
  width: 48px;
  height: 48px;
  border-radius: 13px;
  margin-right: 10px;
  background-color: #edf0ff;
}
.member-info {
  flex: 1;
  min-width: 0;
}
.member-top {
  flex-direction: row;
  align-items: center;
  min-height: 24px;
}
.member-name {
  font-size: 14px;
  font-weight: 600;
  line-height: 21px;
  color: #162646;
}
.row-action {
  flex-direction: row;
  align-items: center;
  justify-content: center;
  background-color: #edf5ff;
  border-radius: 10px;
  padding: 6px;
  margin-left: 4px;
  flex-shrink: 0;
}
.row-action-text {
  font-size: 10px;
  color: #1766ff;
  margin-left: 3px;
}
.row-danger {
  background-color: #fff0f2;
}
.row-danger .row-action-text {
  color: #fa536a;
}
.small-muted {
  font-size: 11px;
  line-height: 19px;
  color: #8592ae;
}
.compact-pill {
  padding: 3px 7px;
  border-radius: 12px;
  margin-right: 4px;
  margin-bottom: 3px;
}
.compact-pill .tag-label {
  font-size: 10px;
}
.no-tag {
  font-size: 10px;
  color: #a1acc0;
}
.bottom-action {
  margin-top: 16px;
}
.grow {
  flex: 1;
  min-width: 0;
}
.picker-row {
  flex-direction: row;
  align-items: center;
  border: 1px solid #edf2fc;
  border-radius: 13px;
  padding: 8px;
  margin-bottom: 5px;
  background-color: #fcfdff;
}
.picker-avatar {
  width: 43px;
  height: 43px;
  border-radius: 11px;
  margin-right: 11px;
  background-color: #edf3ff;
}
.check-circle {
  width: 23px;
  height: 23px;
  border-radius: 13px;
  border: 1px solid #a5b1ca;
  align-items: center;
  justify-content: center;
  margin: 0 4px 0 8px;
}
.check-selected {
  background-color: #1766ff;
  border-color: #1766ff;
}
.search-box {
  flex-direction: row;
  align-items: center;
  height: 43px;
  border: 1px solid #e1e8ff;
  border-radius: 12px;
  padding: 0 12px;
  margin-bottom: 12px;
  background-color: #f8faff;
}
.search-input {
  flex: 1;
  font-size: 13px;
  margin-left: 9px;
}
.selection-count {
  flex-direction: row;
  align-items: center;
  margin-bottom: 10px;
}
.selection-number {
  font-size: 15px;
  font-weight: 600;
  color: #1766ff;
  margin: 0 7px;
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
  border-radius: 13px;
  font-size: 16px;
}
.submit-button {
  margin-left: 10px;
}
.person-profile {
  flex-direction: row;
  align-items: center;
  padding: 1px 0 16px;
}
.person-profile-avatar {
  width: 77px;
  height: 77px;
  border-radius: 19px;
  margin-right: 15px;
  background-color: #edf3ff;
}
.person-profile-name {
  font-size: 21px;
  font-weight: 600;
  line-height: 30px;
  color: #14234a;
}
.profile-account {
  font-size: 15px;
  line-height: 27px;
  color: #8190b1;
}
.tag-section {
  border-top: 1px solid #e8edf8;
  padding-top: 14px;
  margin-top: 2px;
  padding-bottom: 12px;
}
.modal-section-title {
  font-size: 15px;
  font-weight: 600;
  color: #17264b;
  margin-left: 8px;
  margin-right: 8px;
}
.tag-section .tag-wrap {
  margin-top: 12px;
}
.tag-pill .ui-icon {
  margin-left: 5px;
}
.new-tag-button,
.new-tag-input {
  flex-direction: row;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  border: 1px solid #cedaff;
  border-radius: 9px;
  margin-top: 3px;
}
.new-tag-label {
  font-size: 12px;
  color: #8396d7;
  margin-left: 8px;
}
.new-tag-field {
  font-size: 13px;
  padding: 0 10px;
  height: 38px;
}
.new-tag-done {
  padding: 8px;
}
.admin-note {
  flex-direction: row;
  align-items: center;
  background-color: #edf4ff;
  padding: 11px 10px;
  border-radius: 11px;
  margin-bottom: 15px;
}
.admin-note-text {
  font-size: 12px;
  color: #5e82dc;
  flex: 1;
  margin-left: 9px;
  line-height: 19px;
}
.admin-heading {
  margin-top: 4px;
  margin-bottom: 9px;
}
.crown {
  font-size: 23px;
  color: #f3b442;
}
.admin-person {
  padding: 8px;
  background-color: #f7faff;
}
.admin-divider {
  padding-top: 15px;
  border-top: 1px solid #e7edf8;
  margin-top: 14px;
}
.owner-fixed {
  font-size: 11px;
  color: #cf991c;
  padding: 6px;
}
.outline-action {
  border: 1px solid #7598ff;
  border-radius: 10px;
  padding: 8px 7px;
}
.intro-note {
  font-size: 12px;
  line-height: 20px;
  color: #8994ab;
  margin-bottom: 10px;
}
.catalog-row {
  flex-direction: row;
  align-items: center;
  padding: 15px 0;
  border-bottom: 1px solid #edf0f8;
}

.group-shell :deep(.sr-main) {
  background: transparent;
}
.group-screen .detail-scroll {
  display: block;
}
.group-screen .detail-body {
  display: block;
}
.group-screen .detail-body > div {
  display: block;
}
.group-screen .hero-title-row {
  flex-direction: row;
}
.form-button.submit-button {
  margin-left: 0;
}
.group-dialog-body .input {
  display: block;
}
.new-tag-field {
  font-size: 13px;
}
.picker-row .ui-icon {
  width: 18px;
  height: 18px;
}
@media (max-width: 360px) {
  .detail-body {
    padding-left: 10px;
    padding-right: 10px;
  }
  .hero-picture {
    width: 90px;
    margin-right: 10px;
  }
  .hero-avatar {
    width: 90px;
    height: 90px;
  }
  .hero-name {
    font-size: 19px;
  }
  .row-action {
    padding: 5px 4px;
  }
  .member-avatar {
    width: 40px;
    height: 40px;
    margin-right: 7px;
  }
  .row-action .ui-icon {
    width: 11px;
    height: 11px;
  }
  .row-action-text {
    font-size: 9px;
  }
  .group-dialog .person-profile-avatar {
    width: 65px;
    height: 65px;
  }
}
.nav-spacer {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}
.hero-action {
  background-color: #f5f8ff;
  border-color: #dde7fa;
}
.hero-action-text {
  color: #45659c;
  font-weight: 600;
}
.action-danger {
  background-color: #fff4f5;
  border-color: #ffdee3;
}
.action-danger .hero-action-text {
  color: #ee6574;
}
.row-action {
  background-color: #edf4ff;
}
.row-action-text {
  color: #3768ce;
}
.row-danger {
  background-color: #fff1f3;
}
.row-danger .row-action-text {
  color: #ec6b7a;
}
.unified-action {
  background-color: #f0f5ff;
  border-color: #e0e8f9;
}
.unified-text {
  color: #4773cc;
}
.outline-action {
  border-color: #a6bdf4;
}
.outline-action .row-action-text {
  color: #4a73cd;
}
.catalog-row {
  min-height: 54px;
  padding: 9px 0;
}
.catalog-row .tag-pill {
  align-self: flex-start;
  margin-bottom: 0;
}
.nav-title,
.hero-name,
.section-title,
.member-name,
.person-profile-name,
.modal-section-title {
  font-weight: 700;
}
.hero-action-text,
.unified-text,
.row-action-text,
.tag-label,
.form-button {
  font-weight: 600;
}
.invite-disabled {
  opacity: 0.45;
}
.invite-reason {
  font-size: 11px;
  line-height: 18px;
  color: #7d8799;
  margin-top: 3px;
}
</style>
