import { createRouter, createWebHistory } from 'vue-router';
import ExtRouter from './ext/';
import PageRouter from './page/';
import ViewsRouter from './views/';
import AvueRouter from './avue-router';
import i18n from '@/lang';
import Store from '@/store/';
import { startMobileLoading } from '@/page/smart-reminder/mobileLoading';
import { isNative } from '@/native/runtime';

const constantRoutes = isNative ? [...PageRouter.filter(route => route.path.startsWith('/app')), { path: '/:pathMatch(.*)*', redirect: '/app/chat' }] : [...ExtRouter, ...PageRouter, ...ViewsRouter];

function collectRouteNames(routes, names = new Set()) {
  routes.forEach(route => {
    if (route.name) {
      names.add(route.name);
    }
    if (route.children?.length) {
      collectRouteNames(route.children, names);
    }
  });
  return names;
}

const constantRouteNames = collectRouteNames(constantRoutes);

//创建路由
const Router = createRouter({
  base: import.meta.env.VITE_APP_BASE,
  history: createWebHistory(import.meta.env.VITE_APP_BASE),
  routes: constantRoutes,
});
AvueRouter.install({
  store: Store,
  router: Router,
  i18n: i18n,
});

if (!isNative) Router.$avueRouter.formatRoutes(Store.getters.menuAll, true);

let releaseRouteLoading;
Router.beforeEach((to, from, next) => {
  releaseRouteLoading?.();
  releaseRouteLoading = to.path.startsWith('/app') && to.path !== from.path ? startMobileLoading() : null;
  next();
});
const finishRouteLoading = () => { releaseRouteLoading?.(); releaseRouteLoading = null; };
Router.afterEach(finishRouteLoading);
Router.onError(finishRouteLoading);

export function resetRouter() {
  // Vue Router 4：移除登录后动态注入的菜单路由，保留静态路由
  Router.getRoutes().forEach(route => {
    if (route.name && !constantRouteNames.has(route.name)) {
      Router.removeRoute(route.name);
    }
  });
}

export default Router;
