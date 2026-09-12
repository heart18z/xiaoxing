import { reactive } from 'vue';
export const DEFAULT_AI_AVATAR='/avatars/assistant/A3.png';
export const avatarState = reactive({ user: '', initial:'我', ai: DEFAULT_AI_AVATAR });
export const setAvatars = data => {
  avatarState.user = data.user?.avatar || '';
  avatarState.initial=(data.user?.name||data.user?.realName||data.user?.account||'我').slice(0,1);
  avatarState.ai = data.aiAvatar || DEFAULT_AI_AVATAR;
};
