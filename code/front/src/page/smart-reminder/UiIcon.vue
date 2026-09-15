<template><svg class="ui-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="paths[name] || paths.note" /></svg></template>
<script setup>
defineProps({name:String});
const paths={
  reset:'M3 10a9 9 0 1 1 2 8M3 4v6h6',
  check:'m5 12 4 4L19 6',
  menu:'M5 6h14M5 12h14M5 18h14',
  list:'M9 5h12M9 12h12M9 19h12M3 5h.01M3 12h.01M3 19h.01',
  plus:'M12 4v16M4 12h16',
  microphone:'M9 5a3 3 0 0 1 6 0v7a3 3 0 0 1-6 0ZM6 10v2a6 6 0 0 0 12 0v-2M12 18v4M9 22h6',
  arrowUp:'M12 20V4M6 10l6-6 6 6',
  arrowDown:'M12 4v16M6 14l6 6 6-6',
  eraser:'m15 3 6 6-11 11H5l-3-3a2 2 0 0 1 0-3L13 3a1.4 1.4 0 0 1 2 0ZM8 8l7 7M10 20h11',
  filter:'M3 4h18l-7 8v7l-4 2V12Z',
  people:'M15 21v-2a6 6 0 0 0-12 0v2M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M17 4a4 4 0 0 1 0 7M21 21v-2a6 6 0 0 0-4-5.65',
  clock:'M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0M12 6v6l4 2',
  calendar:'M5 5h14a2 2 0 0 1 2 2v13H3V7a2 2 0 0 1 2-2ZM7 2v6M17 2v6M3 11h18',
  note:'M6 3h12a2 2 0 0 1 2 2v16H4V5a2 2 0 0 1 2-2ZM8 8h8M8 12h8M8 16h5',
  search:'M20 20l-5-5M17 10A7 7 0 1 1 3 10a7 7 0 0 1 14 0',
  logout:'M10 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h5M9 12h13M17 7l5 5-5 5',
  chevron:'m9 5 7 7-7 7',
};
</script>
<style scoped>.ui-icon{width:22px;height:22px;flex:0 0 auto;vertical-align:middle}</style>
