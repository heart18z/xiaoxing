import router from './router/';
import store from './store';
import { getToken } from '@/utils/auth';
import NProgress from 'nprogress'; // progress bar
import 'nprogress/nprogress.css'; // progress bar style
NProgress.configure({ showSpinner: false });
const lockPage = '/lock'; //锁屏页
router.beforeEach(async (to, from, next) => {
  // 获取匹配的路由数组，预加载组件并设置name以确保keep-alive首次即可匹配
  const matchedRoutes = to.matched;
  const component = matchedRoutes.length > 0 ? matchedRoutes[matchedRoutes.length - 1].components.default : null;
  if (!to.path.startsWith('/app') && component && typeof (component) == 'function') {
    const mod = await component();
    mod.default.name = to.fullPath;
  }
  const meta = to.meta || {};
  const isMenu = meta.menu === undefined ? to.query.menu : meta.menu;
  store.commit('SET_IS_MENU', isMenu === undefined);
  if (getToken()) {
    if (store.getters.isLock && to.path !== lockPage) {
      //如果系统激活锁屏，全部跳转到锁屏页
      next({ path: lockPage });
    } else if (to.path === '/login' || to.path === '/app/login') {
      //如果登录成功访问登录页跳转到主页
      next({ path: to.path === '/app/login' ? '/app/chat' : '/' });
    } else {
      if (store.getters.token.length === 0) {
        store.dispatch('FedLogOut').then(() => {
          next({ path: '/login' });
        });
      } else {
        const meta = to.meta || {};
        const query = to.query || {};
        if (meta.target) {
          window.open(query.url.replace(/#/g, '&'));
          return;
        } else if (meta.isTab !== false) {
          store.commit('ADD_TAG', {
            name: query.name || to.name,
            path: to.path,
            fullPath: to.path,
            params: to.params,
            query: to.query,
            meta: meta,
          });
        }
        next();
      }
    }
  } else {
    //判断是否需要认证，没有登录访问去登录页
    if (meta.isAuth === false) {
      next();
    } else {
      next(to.path.startsWith('/app') ? '/app/login' : '/login');
    }
  }
});

router.afterEach(to => {
  NProgress.done();
  let title = router.$avueRouter.generateTitle(to, { label: 'name' });
  router.$avueRouter.setTitle(title);
  store.commit('SET_IS_SEARCH', false);
});
