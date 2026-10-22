import {ref} from 'vue';
import en from './mobileEnglish.json';
const validMode=value=>value==='zh-cn'||value==='en-us'||value==='system';
const savedMode=localStorage.getItem('smart-ui-language-mode')||localStorage.getItem('smart-ui-language');
export const mobileLanguageMode=ref(validMode(savedMode)?savedMode:'system');
export const effectiveMobileLanguage=mode=>mode==='zh-cn'||mode==='en-us'?mode:((navigator.language||'en-US').toLowerCase().startsWith('zh')?'zh-cn':'en-us');
export const mobileLanguage=ref(effectiveMobileLanguage(mobileLanguageMode.value));
const refreshLanguage=()=>{mobileLanguage.value=effectiveMobileLanguage(mobileLanguageMode.value);document.documentElement.lang=mobileLanguage.value;};
export const setMobileLanguage=value=>{mobileLanguageMode.value=validMode(value)?value:'system';localStorage.setItem('smart-ui-language-mode',mobileLanguageMode.value);localStorage.setItem('smart-ui-language',effectiveMobileLanguage(mobileLanguageMode.value));refreshLanguage();};
refreshLanguage();
window.addEventListener('languagechange',refreshLanguage);
// Lookup only UI literals; historical content is never passed through a translation service.
export const mt=(value,params={})=>{const text=mobileLanguage.value==='en-us'&&typeof value==='string'?(en[value]??value):value;return typeof text==='string'?text.replace(/\{(\w+)\}/g,(token,key)=>params[key]??token):text;};
