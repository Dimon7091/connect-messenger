// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import LoginView from '../views/AuthView.vue'
import HomeView from '../views/HomeView.vue'
import { useUserStore } from '@/stores/userStore'
import ChatsWindow from '@/views/home/ChatsWindow.vue'
import ProfileWindow from '@/views/home/ProfileWindow.vue'
import SettingsWindow from '@/views/home/SettingsWindow.vue'
import FullscreenChatView from '@/views/FullscreenChatView.vue'
import SearchView from '@/views/SearchView.vue'
import BlackList from '@/views/BlackListView.vue'
import { useWebSocketStore } from '@/stores/webSocketStore'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
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
    redirect: '/chats',
    children: [
      {
        path: 'chats',
        name: 'Chats',
        component: ChatsWindow,
      },
      {
        path: 'profile',
        name: 'Profile',
        component: ProfileWindow,
      },
      {
        path: 'settings',
        name: 'Settings',
        component: SettingsWindow,
      },
    ],
  },
  // Полноэкранная страница чата (без хедера/футера)
  {
    path: '/chats/:chatId',
    name: 'FullscreenChat',
    component: FullscreenChatView,
    meta: { requiresAuth: true, fullscreen: true },
  },
  {
    path: '/search',
    name: 'Search',
    component: SearchView,
    meta: { requiresAuth: true, fullscreen: true },
  },
  {
    path: '/blacklist',
    name: 'BlackList',
    component: BlackList,
    meta: { requiresAuth: true, fullscreen: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * Навигационный гард
 */
router.beforeEach(async (to) => {
  const userStore = useUserStore()

  // 1. Сначала инициализируем авторизацию (восстанавливаем сессию),
  // если это еще не было сделано при загрузке страницы
  if (!userStore.isInitialized) {
    await userStore.initAuth()
  }

  const isAuthenticated = !!userStore.user

  // 2. Если страница требует авторизации, а юзер не залогинен
  if (to.meta.requiresAuth && !isAuthenticated) {
    return { name: 'login' }
  }

  // 3. Если юзер залогинен и пытается зайти на страницу логина
  if (to.name === 'login' && isAuthenticated) {
    return { name: 'home' }
  }

  if (userStore.isAuthenticated && userStore.token) {
    const wsStore = useWebSocketStore()
    wsStore.init()
    console.log(`WebSocket connection status`)
  }

  // В остальных случаях разрешаем переход
  return true
})

export default router
