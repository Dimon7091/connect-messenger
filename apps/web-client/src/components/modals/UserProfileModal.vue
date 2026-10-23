<template>
  <div
    v-if="visible"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
    @click.self="handleClose"
  >
    <div
      class="bg-linear-to-b from-blue-50 to-white dark:bg-gray-800 rounded-xl shadow-2xl max-w-sm w-full mx-4 p-6"
    >
      <!-- Заголовок -->
      <div class="flex justify-center items-center mb-4">
        <h3 class="text-lg font-bold text-gray-900 dark:text-white">Профиль пользователя</h3>
      </div>

      <!-- Состояние загрузки данных пользователя -->
      <div v-if="isLoading" class="flex justify-center py-8">
        <div
          class="w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
        ></div>
      </div>

      <!-- Данные пользователя -->
      <div v-else-if="user" class="flex flex-col items-center">
        <!-- Аватар -->
        <div
          @click="openAvatarPreview"
          class="mb-3 relative w-30 h-30 rounded-full flex items-center justify-center shadow-2xl ring-4 ring-white dark:ring-gray-800 transition-opacity"
        >
          <!-- Изображение (всегда в DOM, скрыто пока загружается) -->
          <img
            v-if="user.profile?.avatarUrl"
            v-show="!isAvatarLoading"
            :src="user.profile.avatarUrl"
            class="w-full h-full rounded-full object-cover"
            alt="аватар"
            @load="onAvatarLoaded"
            @error="onAvatarError"
          />

          <!-- Спиннер загрузки (показывается поверх, пока isAvatarLoading) -->
          <div v-if="isAvatarLoading" class="absolute inset-0 flex items-center justify-center">
            <div
              class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
            ></div>
          </div>

          <!-- Иконка удалённого пользователя (если нет аватара) -->
          <div
            v-if="!user.profile?.avatarUrl && isDeleted"
            class="w-full h-full rounded-full flex items-center justify-center bg-gradient-to-br from-gray-400 to-gray-600"
          >
            <svg
              width="48"
              height="48"
              viewBox="0 0 24 24"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
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
          </div>

          <!-- Инициалы (если нет аватара и не удалён) -->
          <div
            v-if="!user.profile?.avatarUrl && !isDeleted"
            class="w-full h-full rounded-full flex items-center justify-center text-white font-bold text-3xl"
            :class="avatarGradientClass"
          >
            {{ avatarInitials }}
          </div>
        </div>

        <h2 class="text-xl font-bold text-gray-900 dark:text-white">{{ fullName }}</h2>
        <p class="mb-3 text-gray-500 dark:text-gray-400">@{{ user.userName }}</p>

        <!-- Статус -->
        <div v-if="!isDeleted" class="flex items-center gap-1.5">
          <span
            class="w-3 h-3 rounded-full"
            :class="userStatus === 'ONLINE' ? 'bg-green-500' : 'bg-gray-400'"
          ></span>
          <span class="text-sm text-gray-600 dark:text-gray-300">{{ statusDisplayText }}</span>
        </div>
        <div v-else class="mt-2 flex items-center gap-1 text-red-600">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            width="20"
            height="20"
            fill="#EF4444"
          >
            <path
              d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"
            />
          </svg>
          <span class="text-sm font-medium">Удалён</span>
        </div>

        <button
          @click="handleClose"
          class="mt-4 w-full py-2 bg-gray-200 hover:bg-gray-300 dark:bg-gray-700 dark:hover:bg-gray-600 rounded-lg font-bold transition"
        >
          Закрыть
        </button>
      </div>

      <!-- Ошибка -->
      <div v-else-if="error" class="text-red-600 text-center py-4">
        {{ error }}
      </div>
    </div>
  </div>

  <!-- Модалка -->
  <AvatarPreviewModal
    :visible="showAvatarPreview"
    :image-url="avatarUrl || ''"
    :alt="fullName"
    @close="showAvatarPreview = false"
  />
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'
import AvatarPreviewModal from '@/components/modals/AvatarPreviewModal.vue'
import type { User } from '@/types/user.types'

const props = defineProps<{
  visible: boolean
  userId: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const userStore = useUserStore()

const user = ref<User | null>(null)
const isLoading = ref(false)
const error = ref<string | null>(null)
const isAvatarLoading = ref(false)

const currentTime = ref(Date.now())
let timeUpdateInterval: ReturnType<typeof setInterval> | null = null

const showAvatarPreview = ref(false)
const avatarUrl = computed(() => user.value?.profile.avatarUrl || null)
const openAvatarPreview = () => {
  if (user.value?.profile.avatarUrl) {
    showAvatarPreview.value = true
  }
}

// Загрузка пользователя
const loadUser = async () => {
  if (!props.userId) return
  isLoading.value = true
  error.value = null
  try {
    const data = await userStore.getUserById(props.userId)
    user.value = data
    // Если есть аватар, начинаем загрузку
    if (data?.profile?.avatarUrl) {
      isAvatarLoading.value = true
    } else {
      isAvatarLoading.value = false
    }
  } catch (err) {
    error.value = err instanceof Error ? err.message : 'Ошибка загрузки профиля'
  } finally {
    isLoading.value = false
  }
}

// Сброс состояния при закрытии
const resetState = () => {
  isAvatarLoading.value = false
  error.value = null
}

// Обработчики загрузки/ошибки аватара
const onAvatarLoaded = () => {
  isAvatarLoading.value = false
}

const onAvatarError = () => {
  isAvatarLoading.value = false
  // Если аватар не загрузился, очищаем URL, чтобы показывать инициалы
  if (user.value?.profile) {
    user.value.profile.avatarUrl = undefined
  }
}

watch(
  () => props.visible,
  (newVal) => {
    if (newVal && props.userId) {
      loadUser()
    } else {
      resetState()
    }
  },
  { immediate: true },
)

// Следим за изменением avatarUrl (например, если пользователь обновил аватар)
watch(
  () => user.value?.profile?.avatarUrl,
  (newUrl) => {
    if (newUrl) {
      isAvatarLoading.value = true
    } else {
      isAvatarLoading.value = false
    }
  },
  { immediate: true },
)

// Вычисляемые свойства
const fullName = computed(() => {
  if (!user.value) return ''
  const { firstName, lastName } = user.value.profile || {}
  return `${firstName ?? ''} ${lastName ?? ''}`.trim() || 'Без имени'
})

const avatarInitials = computed(() => {
  if (!user.value) return ''
  const name = fullName.value
  return getInitialsFromName(name)
})

const avatarGradientClass = computed(() => {
  if (!user.value) return 'bg-gradient-to-br from-gray-500 to-gray-700'
  if (user.value.profile?.avatarThumbUrl) return ''
  return getAvatarGradient(user.value.id)
})

const userStatus = computed(() => {
  if (!user.value) return 'OFFLINE'
  return userStore.getUserStatus(user.value.id) || 'OFFLINE'
})

const userLastSeen = computed(() => {
  if (!user.value) return null
  return userStore.getUserLastSeen(user.value.id)
})

const formatLastSeen = (lastSeen: string | number): string => {
  if (!lastSeen) return ''
  let timestamp: number
  if (typeof lastSeen === 'number') {
    timestamp = lastSeen * 1000
  } else {
    timestamp = Date.parse(lastSeen)
    if (isNaN(timestamp)) return ''
  }
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return ''
  const now = currentTime.value
  const diffMs = now - timestamp
  const diffMins = Math.floor(diffMs / 60000)
  const diffHours = Math.floor(diffMs / 3600000)
  const diffDays = Math.floor(diffMs / 86400000)
  if (diffMins < 1) return 'был(а) только что'
  if (diffMins < 60) return `был(а) ${diffMins} мин. назад`
  if (diffHours < 24) return `был(а) ${diffHours} ч. назад`
  if (diffDays === 1) return 'был(а) вчера'
  return date.toLocaleDateString('ru-RU', { day: 'numeric', month: 'long', year: 'numeric' })
}

const formattedLastSeen = computed(() => {
  if (!userLastSeen.value) return null
  return formatLastSeen(userLastSeen.value)
})

const statusDisplayText = computed(() => {
  if (userStatus.value === 'ONLINE') return 'Онлайн'
  if (formattedLastSeen.value) return formattedLastSeen.value
  return 'не в сети'
})

const isDeleted = computed(() => user.value?.isDeleted === true)

const handleClose = () => {
  emit('close')
}

// Обновление времени каждую минуту
onMounted(() => {
  timeUpdateInterval = setInterval(() => {
    currentTime.value = Date.now()
  }, 60000)
})

onUnmounted(() => {
  if (timeUpdateInterval) {
    clearInterval(timeUpdateInterval)
    timeUpdateInterval = null
  }
})
</script>

<style scoped>
@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
.animate-spin {
  animation: spin 1s linear infinite;
}
</style>
