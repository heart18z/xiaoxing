import { readonly, ref } from 'vue';

const visible = ref(false);
const pending = new Set();
let showTimer;
export const mobileLoadingVisible = readonly(visible);

// Each operation owns its release: redirects and cancelled routes cannot leak counts.
export const startMobileLoading = () => {
  const token = Symbol('mobile-loading');
  pending.add(token);
  if (!showTimer && !visible.value) {
    showTimer = setTimeout(() => { showTimer = null; visible.value = pending.size > 0; }, 120);
  }
  return () => {
    pending.delete(token);
    if (!pending.size) {
      clearTimeout(showTimer); showTimer = null; visible.value = false;
    }
  };
};

export const withMobileLoading = async task => {
  const release = startMobileLoading();
  try { return await task(); }
  finally { release(); }
};
