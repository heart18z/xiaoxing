import Store from '@/store/';

export default [
  {
    path: '/login',
    name: '登录页',
    component: () =>
      Store.getters.isMacOs ? import('@/mac/login.vue') : import('@/page/login/index.vue'),
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/app/login',
    name: '智能提醒登录',
    component: () => import('@/page/smart-reminder/login.vue'),
    meta: { keepAlive: false, isTab: false, isAuth: false },
  },
  {
    path: '/app',
    redirect: '/app/chat',
  },
  {
    path: '/app/chat',
    name: '智能对话',
    component: () => import('@/page/smart-reminder/chat.vue'),
    meta: { keepAlive: true, isTab: false },
  },
  {
    path: '/app/events',
    name: '我的事件',
    component: () => import('@/page/smart-reminder/events.vue'),
    meta: { keepAlive: false, isTab: false },
  },
  {
    path: '/app/event/:id',
    name: '事件详情',
    component: () => import('@/page/smart-reminder/event-detail.vue'),
    meta: { keepAlive: false, isTab: false },
  },
  {
    path: '/app/friends',
    redirect: '/app/me',
  },
  {
    path: '/app/me',
    name: '我的',
    component: () => import('@/page/smart-reminder/me.vue'),
    meta: { keepAlive: false, isTab: false },
  },
  {
    path: '/app/settings',
    name: '智能提醒设置',
    component: () => import('@/page/smart-reminder/settings.vue'),
    meta: { keepAlive: false, isTab: false },
  },
  {
    path: '/oauth/redirect/:source',
    name: '第三方登录',
    component: () =>
      Store.getters.isMacOs ? import('@/mac/login.vue') : import('@/page/login/index.vue'),
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/oauth/keycloak/callback',
    name: 'Keycloak回调',
    component: () => import('@/page/login/keycloak-callback.vue'),
    meta: {
      keepAlive: false,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/lock',
    name: '锁屏页',
    component: () =>
      Store.getters.isMacOs ? import('@/mac/lock.vue') : import('@/page/lock/index.vue'),
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/404',
    component: () => import(/* webpackChunkName: "page" */ '@/components/error-page/404.vue'),
    name: '404',
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/403',
    component: () => import(/* webpackChunkName: "page" */ '@/components/error-page/403.vue'),
    name: '403',
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/500',
    component: () => import(/* webpackChunkName: "page" */ '@/components/error-page/500.vue'),
    name: '500',
    meta: {
      keepAlive: true,
      isTab: false,
      isAuth: false,
    },
  },
  {
    path: '/',
    name: '主页',
    redirect: '/wel',
  },
];
