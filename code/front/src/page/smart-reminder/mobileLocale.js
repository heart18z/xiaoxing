import {ref} from 'vue';
import en from './mobileEnglish.json';
export const mobileLanguage=ref(localStorage.getItem('smart-ui-language')==='en-us'?'en-us':'zh-cn');
export const setMobileLanguage=value=>{mobileLanguage.value=value==='en-us'?'en-us':'zh-cn';localStorage.setItem('smart-ui-language',mobileLanguage.value);document.documentElement.lang=mobileLanguage.value;};
// Lookup only UI literals; historical content is never passed through a translation service.
export const mt=(value,params={})=>{const text=mobileLanguage.value==='en-us'&&typeof value==='string'?(en[value]??value):value;return typeof text==='string'?text.replace(/\{(\w+)\}/g,(token,key)=>params[key]??token):text;};
