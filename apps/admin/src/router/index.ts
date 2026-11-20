import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import { useAuthStore } from '@/stores/auth.store.ts';
import LoginView from '../views/LoginView.vue';
import HomeView from '../views/HomeView.vue';
import ForbiddenView from '../views/ForbiddenView.vue';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean;
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'home',
    component: HomeView,
    meta: { requiresAuth: true },
  },
  {
    path: '/forbidden',
    name: 'forbidden',
    component: ForbiddenView,
    meta: { requiresAuth: false },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

router.beforeEach(async to => {
  const authStore = useAuthStore();
  const token = localStorage.getItem('token');

  if (!token) {
    authStore.destroy();
    if (to.meta.requiresAuth) {
      return '/login';
    }
    return true;
  }

  if (!authStore.user) {
    const isSuccess = await authStore.fetchAdmin();

    if (!isSuccess) {
      if (to.meta.requiresAuth) {
        return '/login';
      }
      return true;
    }

    authStore.startAutoUpdate();
  }

  if (to.path === '/login') {
    return '/';
  }

  return true;
});

export default router;
