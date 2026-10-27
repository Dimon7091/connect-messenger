<template>
  <div class="bg-white dark:bg-gray-900 min-h-screen flex flex-col" @click="closeMenu">
    <header
      class="flex items-center py-3 border-b border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-900 pl-4 flex-shrink-0"
    >
      <button
        @click="goBack"
        class="p-2 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-full transition border border-gray-200 dark:border-gray-500"
      >
        <svg
          class="w-8 h-8 text-gray-700 dark:text-gray-300"
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            stroke-linecap="round"
            stroke-linejoin="round"
            stroke-width="2"
            d="M10 19l-7-7m0 0l7-7m-7 7h18"
          />
        </svg>
      </button>
      <h1 class="ml-3 text-lg font-semibold text-gray-900 dark:text-gray-200">Черный список</h1>
    </header>

    <!-- Затемняющий оверлей -->
    <div v-if="activeMenu" class="fixed inset-0 bg-black/50 z-40" @click="closeMenu"></div>

    <!-- Контент -->
    <div v-if="loading" class="flex-1 flex items-start justify-center pt-30">
      <svg
        class="animate-spin h-10 w-10 text-blue-500 dark:text-gray-200"
        xmlns="http://www.w3.org/2000/svg"
        fill="none"
        viewBox="0 0 24 24"
      >
        <circle
          class="opacity-25"
          cx="12"
          cy="12"
          r="10"
          stroke="currentColor"
          stroke-width="4"
        ></circle>
        <path
          class="opacity-75"
          fill="currentColor"
          d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
        ></path>
      </svg>
    </div>

    <ul v-else-if="blockedUsers.length" class="flex-1 dark:bg-gray-700 pt-3 overflow-y-auto">
      <li
        v-for="user in blockedUsers"
        :key="user.id"
        class="relative m-2"
        :class="{ 'z-50': activeMenu === user.id }"
        @click.stop="closeMenu"
      >
        <div
          class="flex items-center gap-3 p-2 bg-white dark:bg-gray-900 rounded-lg shadow cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 transition"
        >
          <!-- Аватар -->
          <div class="shrink-0 relative">
            <div
              :class="[
                'w-10 h-10 rounded-full backdrop-blur-sm flex items-center justify-center shadow-xl text-white font-bold text-sl uppercase',
                getAvatarClass(user),
              ]"
            >
              <img
                v-if="user.profile?.avatarThumbUrl"
                :src="user.profile.avatarThumbUrl"
                class="w-full h-full rounded-full object-cover"
                alt="avatar"
              />
              <span v-else-if="user.isDeleted">
                <svg width="34" height="34" viewBox="0 0 24 24" fill="none">
                  <g
                    stroke="#FFFFFF"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    opacity="0.6"
                  >
                    <circle cx="10" cy="7" r="4" />
                    <path d="M3 19V16.5C3 15.12 4.12 14 5.5 14H13.5" />
                  </g>
                  <g stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round">
                    <line x1="16" y1="14" x2="22" y2="20" />
                    <line x1="22" y1="14" x2="16" y2="20" />
                  </g>
                </svg>
              </span>
              <span v-else>{{ getInitials(user) }}</span>
            </div>
          </div>

          <!-- Информация -->
          <div class="flex-1 min-w-0">
            <h3 class="font-medium text-black dark:text-gray-200 text-lg truncate">
              {{ getDisplayName(user) }}
            </h3>
          </div>

          <!-- Контейнер для иконки и кнопки (динамическая ширина) -->
          <div
            class="relative flex items-center overflow-hidden transition-all duration-300"
            style="width: 44px; height: 44px"
            :style="activeMenu === user.id ? 'width: 130px;' : ''"
          >
            <!-- Иконка трёх точек (исчезает) -->
            <span
              @click.stop="toggleMenu(user.id)"
              class="absolute inset-y-0 right-0 cursor-pointer rounded-full dark:hover:bg-gray-700 p-1 transition-all duration-300 flex items-center justify-center"
              :class="{
                'opacity-0 scale-75 pointer-events-none': activeMenu === user.id,
                'opacity-100 scale-100 pointer-events-auto': activeMenu !== user.id,
              }"
              style="width: 44px; height: 44px"
            >
              <svg
                xmlns="http://www.w3.org/2000/svg"
                fill="none"
                viewBox="0 0 24 24"
                stroke-width="1.5"
                stroke="currentColor"
                class="w-8 h-8 text-gray-400 dark:text-gray-200"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M12 6.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5ZM12 12.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5ZM12 18.75a.75.75 0 1 1 0-1.5.75.75 0 0 1 0 1.5Z"
                />
              </svg>
            </span>

            <!-- Кнопка "Разблокировать" (выезжает) -->
            <button
              v-if="activeMenu === user.id"
              @click.stop="removeFromBlacklist(user.id)"
              class="absolute inset-0 flex items-center justify-center p-1 bg-green-500 text-white font-semibold rounded-lg transition-all duration-300 animate-slide-in"
              :disabled="unblocking[user.id]"
              :class="{ 'opacity-50': unblocking[user.id] }"
              style="left: 0; right: 0"
            >
              <span v-if="!unblocking[user.id]">Разблокировать</span>
              <span v-else class="flex items-center gap-1">
                <svg
                  class="animate-spin h-5 w-5 text-white"
                  xmlns="http://www.w3.org/2000/svg"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    class="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    stroke-width="4"
                  ></circle>
                  <path
                    class="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  ></path>
                </svg>
                <span>Удаление...</span>
              </span>
            </button>
          </div>
        </div>
      </li>
    </ul>
    <div v-else class="flex-1 flex items-start pt-30 justify-center text-gray-400 text-sl">
      Список пуст
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/userStore'
import type { User } from '@messenger/shared-schema'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'

const router = useRouter()
const userStore = useUserStore()

const blockedUsers = ref<User[]>([])
const loading = ref(false)
const activeMenu = ref<string | null>(null)
const unblocking = ref<Record<string, boolean>>({})

// ========== Функции отображения ==========
function getDisplayName(user: User): string {
  const firstName = user.profile?.firstName || ''
  const lastName = user.profile?.lastName || ''
  if (firstName || lastName) {
    return `${firstName} ${lastName}`.trim()
  }
  return user.userName
}

function getInitials(user: User): string {
  const name = getDisplayName(user)
  return getInitialsFromName(name)
}

function getAvatarClass(user: User): string {
  if (user.profile?.avatarThumbUrl) return ''
  if (user.isDeleted) return 'bg-gradient-to-br from-gray-500 to-gray-700'
  return getAvatarGradient(user.id)
}

// ========== Управление меню ==========
const toggleMenu = (userId: string) => {
  activeMenu.value = activeMenu.value === userId ? null : userId
}

const closeMenu = () => {
  activeMenu.value = null
}

// ========== Загрузка пользователей ==========
const loadBlockedUsers = async () => {
  // Проверяем, есть ли ID в чёрном списке
  const blackListIds = userStore.user?.blackListIds
  if (!blackListIds || blackListIds.length === 0) {
    blockedUsers.value = []
    loading.value = false
    return
  }

  loading.value = true
  try {
    blockedUsers.value = await userStore.fetchBlacklistedUsers()
  } catch (error) {
    console.error('Ошибка загрузки чёрного списка:', error)
    blockedUsers.value = []
  } finally {
    loading.value = false
  }
}

// ========== Удаление из чёрного списка ==========
const removeFromBlacklist = async (userId: string) => {
  unblocking.value[userId] = true
  try {
    await userStore.removeFromBlackList(userId)
    await new Promise((resolve) => setTimeout(resolve, 500))
    await loadBlockedUsers()
    closeMenu()
  } catch (error) {
    console.error('Ошибка удаления из черного списка:', error)
  } finally {
    unblocking.value[userId] = false
  }
}

onMounted(() => {
  loadBlockedUsers()
})

const goBack = () => {
  router.back()
}
</script>

<style scoped>
.truncate {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.animate-slide-in {
  animation: slideIn 0.3s ease-out forwards;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

.animate-spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
