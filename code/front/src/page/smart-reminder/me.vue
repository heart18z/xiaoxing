<template>
  <AppShell :title="mt('我的')" variant="profile">
    <div class="me-page">
      <section class="profile sr-card">
        <button type="button" class="avatar-editor" :class="{uploading}" :aria-label="mt('更换我的头像')" :title="mt('更换头像')" @click="avatarTarget='user'">
          <img v-if="profile.avatar" :src="profile.avatar" :alt="mt('我的头像')" />
          <span v-else>{{ initial }}</span>
          <i><svg viewBox="0 0 24 24"><path d="M4 8.5h3l1.4-2h7.2l1.4 2h3v10H4z"/><circle cx="12" cy="13.5" r="3.5"/></svg></i>
        </button>
        <div class="profile-main">
          <div class="nickname-row"><h1>{{ displayName }}</h1></div>
          <p>@{{ data.user?.account }}</p><small>{{ mt("点击头像即可更换") }}</small>
        </div>
        <div class="profile-actions"><button class="edit-profile" @click="openProfileEditor">{{ mt("编辑资料") }}</button><button type="button" class="settings-entry" @click="router.push('/app/settings')">{{ mt("设置") }}<UiIcon name="chevron" /></button></div>
      </section>
      <el-dialog v-model="editingProfile" :title="mt('编辑资料')" width="420px" align-center append-to-body>
        <form class="profile-edit-form" @submit.prevent="saveNickname">
          <label for="profile-nickname">{{ mt("昵称") }}</label>
          <input id="profile-nickname" v-model.trim="nicknameDraft" maxlength="50" class="sr-input" :placeholder="mt('设置昵称')" />
          <button class="sr-button" :disabled="saving">{{ saving?mt('保存中…'):mt('保存资料') }}</button>
        </form>
      </el-dialog>

      <button type="button" class="ai-profile sr-card" @click="avatarTarget='ai'"><img :src="profile.aiAvatar||'/avatars/assistant/A3.png'" :alt="mt('AI头像')"/><span><b>{{ mt("我的 AI 助手") }}</b><small>{{ mt("设置专属于你的 AI 头像") }}</small></span><i>{{ mt("更换 ›") }}</i></button>
      <AvatarPicker :kind="avatarTarget" :initial="initial" :open="!!avatarTarget" :title="avatarTarget==='ai'?mt('设置 AI 头像'):mt('设置我的头像')" :value="avatarTarget==='ai'?profile.aiAvatar:profile.avatar" :busy="uploading||saving" @close="avatarTarget=''" @choose="chooseAvatar" @upload="changeAvatar" />

      <div class="friend-tabs">
        <button :class="{active:friendTab==='friends'}" @click="friendTab='friends'">{{ mt("我的好友") }}<span>{{ friends.length }}</span></button>
        <button :class="{active:friendTab==='pending'}" @click="friendTab='pending'">{{ mt("申请待通过") }}<span v-if="pendingCount">{{ pendingCount }}</span></button>
      </div>

      <div v-if="friendTab==='friends'" class="friend-area">
        <div class="search"><div class="friend-search-input"><UiIcon name="search" /><input v-model.trim="keyword" class="sr-input" :aria-label="mt('查找好友')" :placeholder="mt('账号、姓名或手机号')" @keyup.enter="search" /></div><button class="sr-button" :disabled="searching" @click="search"><span v-if="searching" class="search-spinner"></span>{{ searching?mt('查找中'):mt('查找好友') }}</button></div>
        <el-dialog v-model="searchVisible" :title="mt('查找好友')" width="min(92vw,460px)" align-center append-to-body class="friend-search-dialog">
          <div class="search-results" aria-live="polite">
            <div v-if="searching" class="search-loading" role="status"><span></span><span></span><span></span>{{ mt("正在查找好友…") }}</div>
            <p v-else-if="searchError" class="search-empty">{{ mt("查找失败，请关闭弹窗后重试") }}</p>
            <p v-else-if="searched&&!results.length" class="search-empty">{{ mt("没有找到匹配的用户") }}</p>
            <div v-for="user in results" :key="user.id" class="person">
              <div class="sr-avatar"><img v-if="user.avatar" :src="user.avatar" alt=""/><template v-else>{{ userInitial(user) }}</template></div>
              <div><b>{{ userDisplay(user) }}</b><p>{{ user.account }} · {{ user.phone||mt('未留手机') }}</p></div>
              <button v-if="user.relationStatus==='NONE'" class="sr-button secondary" @click="openPermission(user,'FRIEND')">{{ mt("添加") }}</button><span v-else class="relation">{{ mt(relation(user.relationStatus)) }}</span>
            </div>
          </div>
        </el-dialog>
        <div class="friend-scroll">
          <section v-if="friends.length" class="sr-card people-card friend-list">
            <div v-for="user in friends" :key="user.id" class="person">
              <div class="sr-avatar"><img v-if="user.avatar" :src="user.avatar" alt=""/><template v-else>{{ userInitial(user) }}</template></div>
              <div><b>{{ userDisplay(user) }}</b><p>@{{ user.account }}<template v-if="user.phone"> · {{ user.phone }}</template></p><small>{{ mt(permissionLabel(user.permissionMode)) }}</small></div><button class="permission-change" @click="openPermission(user,'PERMISSION')">{{ mt("管理") }}</button>
            </div>
          </section>
          <div v-else class="sr-card sr-empty">{{ mt("暂无好友，可以通过账号、姓名或手机号查找") }}</div>
        </div>
      </div>

      <div v-else class="friend-area">
        <div class="friend-scroll pending-scroll">
          <section v-if="requests.incoming?.length" class="sr-card people-card">
            <h3>{{ mt("等待我处理") }}</h3>
            <div v-for="item in requests.incoming" :key="item.id" class="person">
              <div class="sr-avatar"><img v-if="item.avatar" :src="item.avatar" alt=""/><template v-else>{{ userInitial(item) }}</template></div>
              <div><b>{{ userDisplay(item) }}</b><p>{{ mt(requestTitle(item,true)) }}</p><small>{{ mt(permissionLabel(item.permissionMode,true)) }}</small></div><div class="reply"><button @click="reply(item.id,false)">{{ mt("拒绝") }}</button><button @click="reply(item.id,true)">{{ mt("同意") }}</button></div>
            </div>
          </section>
          <section v-if="requests.outgoing?.length" class="sr-card people-card">
            <h3>{{ mt("等待对方通过") }}</h3>
            <div v-for="item in requests.outgoing" :key="item.id" class="person">
              <div class="sr-avatar"><img v-if="item.avatar" :src="item.avatar" alt=""/><template v-else>{{ userInitial(item) }}</template></div>
              <div><b>{{ userDisplay(item) }}</b><p>{{ mt(requestTitle(item,false)) }}</p><small>{{ mt(permissionLabel(item.permissionMode)) }}</small></div><span class="relation">{{ mt("待确认") }}</span>
            </div>
          </section>
          <div v-if="!pendingCount" class="sr-card sr-empty">{{ mt("暂无待通过的好友申请") }}</div>
        </div>
      </div>
      <button class="logout" @click="logout"><UiIcon name="logout" />{{ mt("退出登录") }}</button>
      <Transition name="mobile-sheet"><div v-if="permissionDialog.open" class="sheet-mask" @click.self="permissionDialog.open=false">
        <section class="permission-sheet sr-card">
          <div class="sheet-head"><div><h3>{{ permissionDialog.type==='FRIEND'?mt('选择好友权限'):mt('申请变更权限') }}</h3><p>{{ mt("与") }}{{ userDisplay(permissionDialog.user||{}) }}{{ mt("的提醒方向") }}</p></div><button @click="permissionDialog.open=false">×</button></div>
          <label v-for="option in permissionOptions" :key="option.value" :class="['permission-option',{active:permissionDialog.mode===option.value}]">
            <input v-model="permissionDialog.mode" type="radio" :value="option.value" />
            <span><b>{{ mt(option.label) }}</b><small>{{ mt(option.description) }}</small></span><i>✓</i>
          </label>
          <button class="sr-button permission-submit" :disabled="permissionDialog.submitting" @click="submitPermission">{{ permissionDialog.submitting?mt('提交中…'):permissionDialog.type==='FRIEND'?mt('发送好友申请'):mt('发送变更申请') }}</button>
          <p class="sheet-tip">{{ mt("对方同意后权限才会生效") }}</p>
          <button v-if="permissionDialog.type==='PERMISSION'" class="remove-friend" :disabled="removing" @click="unfriend">{{ removing?mt('正在解除…'):mt('解除好友关系') }}</button>
        </section>
      </div></Transition>
    </div>
  </AppShell>
</template>

<script setup>
import {mt} from './mobileLocale';
import UiIcon from './UiIcon.vue';
const editingProfile=ref(false),nicknameDraft=ref('');
const searchVisible=ref(false),searchError=ref(false);
const openProfileEditor=()=>{nicknameDraft.value=profile.nickname;editingProfile.value=true;};
const saveNickname=async()=>{
  if(saving.value)return;
  if(!nicknameDraft.value){ElMessage.warning(mt('请输入昵称'));return;}
  const previous=profile.nickname;profile.nickname=nicknameDraft.value;
  try{await save();editingProfile.value=false;}catch{profile.nickname=previous;}
};
import { computed, onMounted, reactive, ref } from 'vue';import { useRouter } from 'vue-router';import { useStore } from 'vuex';import { ElMessage,ElMessageBox } from 'element-plus';import AppShell from './AppShell.vue';import { bootstrap,getFriends,getFriendRequests,replyFriend,requestFriend,searchUsers,updateProfile,uploadAvatar,removeFriend } from '@/api/smartReminder';import { withMobileLoading } from './mobileLoading';import AvatarPicker from './AvatarPicker.vue';
const router=useRouter(),store=useStore(),data=ref({}),profile=reactive({nickname:'',avatar:'',aiAvatar:''}),saving=ref(false),uploading=ref(false),friendTab=ref('friends'),keyword=ref(''),results=ref([]),friends=ref([]),requests=reactive({incoming:[],outgoing:[]}),permissionDialog=reactive({open:false,user:null,type:'FRIEND',mode:'MUTUAL',submitting:false});const avatarTarget=ref(''),searching=ref(false),searched=ref(false),removing=ref(false);
const permissionOptions=[{value:'I_CAN_REMIND',label:'我可提醒对方',description:'你可以提醒对方，对方不能提醒你'},{value:'THEY_CAN_REMIND',label:'对方可提醒我',description:'对方可以提醒你，你不能提醒对方'},{value:'MUTUAL',label:'互相提醒',description:'双方都可以向对方创建提醒'}];
const displayName=computed(()=>profile.nickname||data.value.user?.realName||data.value.user?.account||'用户'),initial=computed(()=>displayName.value.slice(0,1)),pendingCount=computed(()=>(requests.incoming?.length||0)+(requests.outgoing?.length||0));
const userDisplay=u=>u.friendRemark||u.name||u.realName||u.account,userInitial=u=>(userDisplay(u)||'?').slice(0,1),relation=s=>({FRIEND:'已是好友',PENDING_OUT:'已申请',PENDING_IN:'待你同意'}[s]||s);
const mirrorMode=mode=>mode==='I_CAN_REMIND'?'THEY_CAN_REMIND':mode==='THEY_CAN_REMIND'?'I_CAN_REMIND':'MUTUAL';
const permissionLabel=(mode,incoming=false)=>permissionOptions.find(x=>x.value===(incoming?mirrorMode(mode):mode))?.label||'互相提醒';
const requestTitle=(item,incoming)=>item.requestType==='PERMISSION'?(incoming?'对方申请变更提醒权限':'提醒权限变更申请'):(item.requestMessage||'好友申请');
const applyUser=user=>{data.value.user=user;profile.nickname=user?.name||user?.realName||user?.account||'';profile.avatar=user?.avatar||'';profile.aiAvatar=user?.aiAvatar??data.value.aiAvatar??'/avatars/assistant/A3.png';};const loadProfile=async()=>{const res=await bootstrap();data.value=res.data.data||{};applyUser(data.value.user||{});};const loadFriends=async()=>{friends.value=(await getFriends()).data.data||[];Object.assign(requests,(await getFriendRequests()).data.data||{});};
const save=async()=>{if(!profile.nickname){ElMessage.warning(mt('请输入昵称'));return;}saving.value=true;try{const res=await updateProfile({nickname:profile.nickname,avatar:profile.avatar,aiAvatar:profile.aiAvatar});applyUser(res.data.data||{});window.dispatchEvent(new Event('smart-reminder:profile-updated'));ElMessage.success(mt('个人资料已保存'));}finally{saving.value=false;}};
const chooseAvatar=async url=>{if(saving.value||uploading.value)return;const field=avatarTarget.value==='ai'?'aiAvatar':'avatar',previous=profile[field];profile[field]=url;try{await save();avatarTarget.value='';}catch(error){profile[field]=previous;ElMessage.error(mt(error?.message||'保存头像失败'));}};
const changeAvatar=async file=>{if(!file||uploading.value||saving.value)return;if(!['image/jpeg','image/png','image/gif','image/webp'].includes(file.type)||file.size>2*1024*1024){ElMessage.warning(mt('请选择 2MB 以内的 JPG、PNG、GIF 或 WEBP 图片'));return;}uploading.value=true;try{const res=await uploadAvatar(file),value=res.data.data,url=value?.link||value?.url||value;if(typeof url!=='string'||!url)throw new Error('未获得头像地址');uploading.value=false;await chooseAvatar(url);}catch(error){ElMessage.error(mt(error?.message||'头像上传失败'));}finally{uploading.value=false;}};
const search=async()=>{if(!keyword.value||searching.value)return;searchVisible.value=true;searchError.value=false;searching.value=true;searched.value=false;results.value=[];const query=keyword.value;try{const response=await searchUsers(query);if(keyword.value===query){results.value=response.data.data||[];searched.value=true;}}catch(error){searchError.value=true;ElMessage.error(mt(error?.message||'查找失败，请重试'));}finally{searching.value=false;}};
const unfriend=async()=>{const user=permissionDialog.user;if(!user||removing.value)return;try{await ElMessageBox.confirm(mt('解除与 {name} 的好友关系后，双方不能再新建提醒或恢复提醒分支。历史事件与消息仍保留。',{name:userDisplay(user)}),mt('解除好友关系'),{confirmButtonText:mt('解除关系'),cancelButtonText:mt('保留好友'),type:'warning'});}catch{return;}removing.value=true;try{await removeFriend(user.id);permissionDialog.open=false;await loadFriends();if(keyword.value)await search();window.dispatchEvent(new Event('smart-reminder:badge-refresh'));ElMessage.success(mt('好友关系已解除'));}finally{removing.value=false;}};
const openPermission=(user,type)=>{searchVisible.value=false;permissionDialog.user=user;permissionDialog.type=type;permissionDialog.mode=type==='PERMISSION'?(user.permissionMode||'MUTUAL'):'MUTUAL';permissionDialog.open=true;};
const submitPermission=async()=>{const user=permissionDialog.user;if(!user||permissionDialog.submitting)return;permissionDialog.submitting=true;try{await requestFriend({targetUserId:user.id,permissionMode:permissionDialog.mode,message:permissionDialog.type==='FRIEND'?'我想添加你为智能提醒好友':'申请变更好友提醒权限'});ElMessage.success(mt(permissionDialog.type==='FRIEND'?'好友申请已发送':'权限变更申请已发送'));permissionDialog.open=false;await Promise.all([keyword.value?search():Promise.resolve(),loadFriends()]);}finally{permissionDialog.submitting=false;}};
const reply=async(id,accept)=>{await replyFriend({requestId:id,accept});ElMessage.success(mt(accept?'申请已同意':'已拒绝'));await loadFriends();window.dispatchEvent(new Event('smart-reminder:badge-refresh'));};const logout=async()=>{try{await store.dispatch('LogOut');}catch{await store.dispatch('FedLogOut');}router.replace('/app/login');};onMounted(()=>withMobileLoading(()=>Promise.all([loadProfile(),loadFriends()])));
</script>

<style scoped>
:global(.friend-search-dialog.el-dialog){padding:18px;border-radius:22px;max-height:80dvh;box-sizing:border-box}
:global(.friend-search-dialog .el-dialog__header){padding:0 28px 14px 0;margin:0;border-bottom:1px solid #edf0f5}
:global(.friend-search-dialog .el-dialog__title){font-size:17px;font-weight:650;color:#263653}
:global(.friend-search-dialog .el-dialog__body){padding:12px 0 0}
.search-results{max-height:60dvh;overflow-y:auto;overscroll-behavior:contain;scrollbar-width:thin;padding:2px 5px 4px 0}
.search-results .person{min-height:66px}.search-results .sr-avatar{width:42px;height:42px;flex:0 0 42px;border-radius:14px;background:#edf1ff;color:#526bcb;display:grid;place-items:center}
.search-results .person b{font-size:14px;color:#243149}.search-results .person p{font-size:12px}.search-results .person button{border:0;border-radius:12px;background:#edf1ff;color:#4262d5;cursor:pointer}
.avatar-editor{border:0}.ai-profile{display:flex;align-items:center;gap:12px;width:100%;border:0;text-align:left;padding:12px 15px;margin-bottom:12px;cursor:pointer;flex:0 0 auto}.ai-profile img{width:43px;height:43px;border-radius:14px}.ai-profile span{flex:1}.ai-profile b{font-size:14px;color:#263653}.ai-profile small{display:block;font-size:11px;color:#8f9bb0;margin-top:4px}.ai-profile i{font-size:12px;color:#5272bc;font-style:normal}.search button{display:flex;align-items:center;gap:6px}.search-spinner{width:15px;height:15px;border:2px solid #ffffff70;border-top-color:white;border-radius:50%;animation:searchSpin .8s linear infinite}.search-loading{display:flex;align-items:center;justify-content:center;gap:5px;padding:10px;margin-bottom:8px;color:#7990b5;font-size:12px;background:#ffffffa8;border-radius:12px}.search-loading span{width:5px;height:5px;border-radius:50%;background:#5e89ee;animation:searchPulse 1s infinite alternate}.search-loading span:nth-child(2){animation-delay:.2s}.search-loading span:nth-child(3){animation-delay:.4s}.search-empty{font-size:12px;color:#8591a5;text-align:center;margin:0 0 10px}.remove-friend{display:block;width:100%;padding:12px;border:0;background:#fff1f0;color:#d35858;border-radius:12px;margin-top:14px;cursor:pointer}@keyframes searchSpin{to{transform:rotate(360deg)}}@keyframes searchPulse{to{opacity:.25;transform:translateY(-3px)}}
.me-page{height:calc(100vh - 190px);min-height:526px;display:flex;flex-direction:column;overflow:hidden}.profile{display:flex;align-items:center;gap:14px;padding:16px;margin-bottom:12px;flex:0 0 auto}.avatar-editor{position:relative;width:66px;height:66px;flex:0 0 66px;border-radius:22px;background:linear-gradient(135deg,#e5eaff,#f2f5ff);color:#315bff;display:flex;align-items:center;justify-content:center;font-size:25px;font-weight:700;cursor:pointer}.avatar-editor>img{width:100%;height:100%;border-radius:22px;object-fit:cover}.avatar-editor>i{position:absolute;right:-4px;bottom:-4px;width:26px;height:26px;border:3px solid #fff;border-radius:50%;background:#315bff;color:#fff;display:flex;align-items:center;justify-content:center}.avatar-editor svg{width:13px;height:13px;fill:none;stroke:currentColor;stroke-width:1.8}.avatar-editor input{display:none}.avatar-editor.uploading{opacity:.55;pointer-events:none}.profile-main{flex:1;min-width:0}.nickname-row{display:flex;align-items:center;gap:7px}.nickname-row input{min-width:0;width:100%;height:36px;border:1px solid transparent;border-radius:10px;padding:0 9px;background:#f6f8fc;color:#172039;font:700 19px/1 inherit;outline:none}.nickname-row input:focus{border-color:#8da3ff;background:#fff}.nickname-row button{height:34px;flex:0 0 auto;border:0;border-radius:10px;padding:0 11px;background:#edf1ff;color:#315bff;font-weight:700}.profile p{margin:4px 0 0;color:#68738a}.profile small{display:block;color:#a0a8b8;margin-top:4px}.friend-tabs{display:grid;grid-template-columns:1fr 1fr;background:rgba(226,233,244,.88);border-radius:14px;padding:4px;margin:0 0 12px;flex:0 0 auto}.friend-tabs button{height:40px;border:0;border-radius:11px;background:transparent;color:#737d91;font:inherit}.friend-tabs button.active{background:#fff;color:#315bff;font-weight:700;box-shadow:0 3px 12px rgba(34,48,85,.08)}.friend-tabs span{display:inline-flex;align-items:center;justify-content:center;min-width:19px;height:19px;margin-left:3px;padding:0 5px;border-radius:10px;background:#edf1ff;color:#315bff;font-size:11px}.friend-area{display:flex;flex:1;min-height:0;flex-direction:column}.search{display:grid;grid-template-columns:1fr auto;gap:8px;margin-bottom:10px;flex:0 0 auto}.friend-scroll{flex:1;min-height:0;overflow-y:auto;overscroll-behavior:contain;padding-right:3px;scrollbar-width:thin;scrollbar-color:#ccd4e5 transparent}.pending-scroll{padding-top:1px}.people-card{margin-bottom:8px}.people-card>h3{font-size:15px;margin:0 0 13px}.person{display:flex;align-items:center;gap:11px}.person+.person{border-top:1px solid #edf0f5;padding-top:12px;margin-top:12px}.person>.sr-avatar{padding:0;overflow:hidden}.person>.sr-avatar img{width:100%;height:100%;object-fit:cover}.person>div:nth-child(2){flex:1;min-width:0}.person p{margin:3px 0;color:#828b9e;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.person .sr-button{min-height:36px;padding:0 12px}.relation,.friend-mark{flex:0 0 auto;font-size:12px;color:#768198}.friend-mark{color:#315bff;background:#edf1ff;border-radius:12px;padding:4px 8px}.reply{display:flex!important;gap:5px!important;flex:0 0 auto!important}.reply button{border:0;border-radius:9px;padding:8px;color:#7c8495}.reply button:last-child{background:#315bff;color:#fff}.logout{width:100%;height:46px;flex:0 0 46px;margin-top:10px;border:0;border-radius:14px;background:rgba(255,255,255,.94);color:#d84b4b;font-size:15px;box-shadow:0 5px 18px rgba(24,52,91,.06)}
.person small{display:block;color:#315bff;font-size:11px;margin-top:3px}.permission-change{flex:0 0 auto;border:0;border-radius:10px;padding:7px 10px;background:#edf2ff;color:#315bff;font-weight:600}
.sheet-mask{position:fixed;z-index:40;inset:0;background:rgba(20,31,55,.38);display:flex;align-items:flex-end;justify-content:center}.permission-sheet{width:min(100%,760px);border-radius:24px 24px 0 0;padding:20px 18px calc(18px + env(safe-area-inset-bottom));box-shadow:0 -14px 40px rgba(28,45,85,.18)}.sheet-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:14px}.sheet-head h3{margin:0;font-size:19px}.sheet-head p{margin:4px 0 0;color:#8a94a7}.sheet-head>button{border:0;background:#f1f4fa;border-radius:50%;width:31px;height:31px;color:#7a8498;font-size:20px}.permission-option{display:flex;align-items:center;gap:12px;margin:9px 0;padding:12px;border:1px solid #e5eaf2;border-radius:14px;background:#fafbfe}.permission-option.active{border-color:#7891ff;background:#f1f4ff;box-shadow:0 0 0 2px #e5eaff}.permission-option input{display:none}.permission-option>span{display:flex;flex:1;flex-direction:column}.permission-option b{font-size:15px}.permission-option small{margin-top:3px;color:#8490a5}.permission-option>i{font-style:normal;color:#315bff;opacity:0}.permission-option.active>i{opacity:1}.permission-submit{width:100%;margin-top:13px}.sheet-tip{text-align:center;color:#9aa3b4;font-size:12px;margin:9px 0 0}
</style>

<style scoped>
.me-page{height:auto;min-height:calc(100dvh - 138px);overflow:visible;padding-bottom:84px}
.me-page .profile{padding:16px 17px;gap:16px;border-radius:22px;background:#ffffffd9;box-shadow:none;margin-bottom:9px;position:relative;min-height:97px}
.me-page .avatar-editor{width:62px;height:62px;flex-basis:62px;border-radius:23px;box-shadow:0 0 0 3px #eff2ff}
.me-page .avatar-editor>img{border-radius:23px}
.me-page .avatar-editor>i{width:25px;height:25px;right:-5px;bottom:-3px}
.nickname-row h1{margin:0;font-size:19px;font-weight:750;line-height:1.5;color:#1a243b;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.me-page .profile p{font-size:12px;margin-top:0;color:#8991a5;overflow-wrap:anywhere;line-height:1.5}
.me-page .profile small{font-size:11px;margin-top:5px;color:#8691a4;white-space:nowrap}
.edit-profile{align-self:flex-start;margin-top:3px;flex:0 0 auto;border:1px solid #fff;border-radius:20px;min-height:33px;padding:0 12px;background:linear-gradient(130deg,#f7f9ff,#e9efffd0);box-shadow:inset 0 1px 2px white;color:#4265d5;font-weight:650;font-size:12px;cursor:pointer}
.profile-actions{display:flex;flex-direction:column;align-items:center;gap:5px;flex:0 0 auto}.profile-actions .settings-entry{background:transparent;padding:3px 9px;min-height:28px;color:#7886a7}.profile-actions .edit-profile{align-self:stretch}
.profile-edit-form{display:grid;gap:12px}.profile-edit-form label{font-size:14px;color:#68758e}.profile-edit-form button{margin-top:6px}
.me-page .ai-profile{padding:11px 14px;gap:13px;border:1px solid white;border-radius:22px;box-shadow:inset 0 0 0 2px #ffffff60;background:linear-gradient(120deg,#ffffffef,#ffffffa8);min-height:65px;margin-bottom:12px}
.me-page .ai-profile img{width:43px;height:43px;border-radius:14px}
.me-page .ai-profile b{font-size:16px;color:#202b44}
.me-page .ai-profile small{font-size:12px;color:#8b94a6;margin-top:4px}
.me-page .ai-profile i{font-size:13px;font-weight:600;white-space:nowrap;color:#4566d9}
.me-page .friend-tabs{margin:0 0 13px;padding:3px;border-radius:15px;background:#e4eaf480}
.me-page .friend-tabs button{height:35px;border-radius:12px;font-size:15px;font-weight:600}
.me-page .friend-tabs button.active{box-shadow:0 4px 12px #6e88b40b}
.me-page .friend-tabs span{font-size:12px;border-radius:8px}
.me-page .friend-area{flex:initial;min-height:0}
.me-page .search{gap:9px;margin-bottom:10px}
.friend-search-input{display:flex;align-items:center;gap:9px;padding-left:13px;border:1px solid #e9edf5;border-radius:13px;background:#ffffffec;min-width:0;color:#919cb1}
.friend-search-input .ui-icon{width:19px;height:19px}
.friend-search-input:focus-within{border-color:#8ea9ff;box-shadow:0 0 0 3px #edf1ff}
.friend-search-input .sr-input{border:0;background:transparent;padding-left:0;box-shadow:none;min-width:0;font-size:13px;min-height:39px}
.me-page .search>.sr-button{min-height:39px;font-size:13px;padding:0 14px;border-radius:12px;background:linear-gradient(135deg,#4266ff,#4b64fc);white-space:nowrap}
.me-page .friend-scroll{overflow:visible;padding-right:0;flex:initial}
.me-page .people-card{border-radius:21px;padding:0 13px;background:#ffffffeb;box-shadow:none;margin-bottom:0}
.me-page .people-card>h3{padding-top:15px}
.me-page .people-card+.people-card{margin-top:12px}
.me-page .person{gap:13px;min-height:70px;padding:10px 0;margin:0}
.me-page .person+.person{margin:0;padding-top:10px;border-top:1px solid #e9edf5}
.me-page .person>div:nth-child(2){display:flex;flex-direction:column;align-items:flex-start;gap:1px}
.me-page .person>.sr-avatar{width:43px;height:43px;flex-basis:43px;border-radius:16px;font-size:20px;background:linear-gradient(130deg,#f3eeff,#efeffc);color:#805dcd}
.me-page .person:nth-child(5n+2)>.sr-avatar{background:#eaf2ff;color:#4b8bd4}
.me-page .person:nth-child(5n+3)>.sr-avatar{background:#e5f5ed;color:#4f9e7d}
.me-page .person:nth-child(5n+4)>.sr-avatar{background:#fceaf3;color:#cc628f}
.me-page .person:nth-child(5n+5)>.sr-avatar{background:#fff1e2;color:#e79a52}
.me-page .person b{font-size:15px;font-weight:650;color:#222c43;line-height:20px;max-width:100%;overflow-wrap:anywhere}
.me-page .person p{font-size:12px;margin:0;line-height:16px;color:#9199ab;max-width:100%}
.me-page .person small{display:inline-block;font-size:10px;line-height:14px;background:#eaf0ff;border-radius:6px;color:#4765cb;padding:0 5px;margin-top:0}
.me-page .permission-change{border:1px solid #e5eafd;border-radius:20px;padding:8px 13px;background:linear-gradient(135deg,#f0f4ff,#edf1ff);color:#4268d9;font-size:12px;cursor:pointer}
.me-page .logout{position:fixed;z-index:9;left:50%;transform:translateX(-50%);bottom:96px;width:min(calc(100% - 32px),728px);display:flex;align-items:center;justify-content:center;gap:10px;margin:0;height:46px;border:2px solid white;border-radius:18px;background:#fff5f6;box-shadow:0 5px 22px #b9889812;color:#e95356;font-size:16px;font-weight:600;cursor:pointer}
.me-page .logout .ui-icon{width:22px;height:22px}
@media(max-width:360px){.me-page .profile{gap:11px;padding:14px 12px}.me-page .avatar-editor{width:50px;height:50px;flex-basis:50px}.nickname-row h1{font-size:17px}.edit-profile{padding:0 8px;font-size:11px}.me-page .profile small{font-size:10px}.me-page .search>.sr-button{padding:0 10px;font-size:12px}.friend-search-input{padding-left:9px;gap:6px}.friend-search-input .sr-input{font-size:12px}.me-page .person{gap:9px}.me-page .permission-change{padding:7px 10px}}
</style>
