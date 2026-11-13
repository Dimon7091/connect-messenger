<template>
  <div
    v-if="visible"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm"
    @click.self="handleClose"
  >
    <div class="bg-white rounded-xl shadow-2xl max-w-lg w-full mx-4 max-h-[90vh] overflow-y-auto">
      <!-- Заголовок -->
      <div class="bg-blue-500 py-3 px-6 rounded-t-xl flex justify-between items-center">
        <h3 class="text-lg font-bold text-white">Детали пользователя</h3>
        <button @click="handleClose" class="text-white hover:text-gray-200 transition">✕</button>
      </div>

      <!-- Тело -->
      <div class="p-6 space-y-4" v-if="user">
        <!-- Аватар и имя -->
        <div class="flex items-center gap-4">
          <div
            :class="[
              'w-20 h-20 rounded-full bg-linear-to-br text-white flex items-center justify-center font-bold text-2xl uppercase shadow-sm',
              avatarGradientClass(user.id),
            ]"
          >
            <img
              v-if="user.profile?.avatarThumbUrl"
              :src="user.profile.avatarThumbUrl"
              class="w-full h-full rounded-full object-cover"
              alt="аватар"
            />
            <span v-else-if="isDeleted">
              <svg width="50" height="50" viewBox="0 0 24 24" fill="none" xmlns="http://w3.org">
                <!-- Контур человека белого цвета -->
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
                <!-- Белый крестик действия -->
                <g stroke="#FFFFFF" stroke-width="2.5" stroke-linecap="round">
                  <line x1="16" y1="14" x2="22" y2="20" />
                  <line x1="22" y1="14" x2="16" y2="20" />
                </g>
              </svg>
            </span>
            <span v-else>{{ getInitials(user) }}</span>
          </div>
          <div>
            <div class="flex flex-col items-start">
              <span
                v-if="isAdmin"
                class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-blue-100 text-blue-800 mb-1"
              >
                👑 Администратор
              </span>
              <span class="text-xl font-bold text-gray-800">{{ fullName }}</span>
            </div>
            <p class="text-sm text-gray-500">@{{ user.userName }}</p>
          </div>
        </div>

        <!-- Детали -->
        <div class="grid grid-cols-2 gap-4 pt-2 border-t border-gray-100">
          <div>
            <p class="text-xs text-gray-400 uppercase font-bold">Статус</p>
            <p class="text-gray-800 font-medium">
              <span
                :class="[
                  'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold',
                  getStatusColor(user.status),
                ]"
              >
                <span
                  :class="[
                    'w-1.5 h-1.5 rounded-full',
                    getStatusDotColor(user.status),
                    user.status === 'ONLINE' ? 'animate-pulse' : '',
                  ]"
                ></span>
                {{ user.status?.toLowerCase() || 'неизвестно' }}
              </span>
            </p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase font-bold">ID</p>
            <p class="text-gray-800 font-medium text-sm">{{ user.id }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase font-bold">Дата регистрации</p>
            <p class="text-gray-800 text-sm">{{ formatDate(user.createdAt) }}</p>
          </div>
          <div>
            <p class="text-xs text-gray-400 uppercase font-bold">Последний визит</p>
            <p class="text-gray-800 text-sm">{{ formatDate(user.lastSeen) }}</p>
          </div>
          <div v-if="user.email" class="col-span-2">
            <p class="text-xs text-gray-400 uppercase font-bold">Email</p>
            <p class="text-gray-800 text-sm">{{ user.email }}</p>
          </div>
          <div v-if="user.roles?.length" class="col-span-2">
            <p class="text-xs text-gray-400 uppercase font-bold">Роли</p>
            <p class="text-gray-800 text-sm">
              {{ user.roles.join(', ') }}
            </p>
          </div>
          <div v-if="user.isBanned !== undefined" class="col-span-2">
            <p class="text-xs text-gray-400 uppercase font-bold">Статус аккаунта</p>
            <p
              v-if="!isDeleted"
              class="text-sm font-medium"
              :class="user.isBanned ? 'text-red-600' : 'text-green-600'"
            >
              {{ user.isBanned ? '🔒 Заблокирован' : '✅ Активен' }}
            </p>
            <p
              v-else-if="isDeleted"
              class="flex items-center gap-1 text-sm font-medium text-red-600"
            >
              <svg xmlns="http://w3.org" viewBox="0 0 24 24" width="26" height="26" fill="#EF4444">
                <path
                  d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"
                />
              </svg>
              <span>Удален</span>
            </p>
          </div>
        </div>

        <!-- Действия (скрываем для администраторов) -->
        <div
          v-if="!isAdmin && !isDeleted"
          class="flex flex-wrap gap-3 pt-4 border-t border-gray-200"
        >
          <button
            @click="handleToggleBlock"
            :disabled="loading"
            :class="[
              'flex-1 min-w-[120px] px-4 py-2 rounded-lg font-bold text-sm transition-all duration-200 flex items-center justify-center gap-2',
              user?.isBanned
                ? 'bg-green-500 hover:bg-green-600 disabled:bg-green-300'
                : 'bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300',
            ]"
          >
            <span
              v-if="loading"
              class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
            ></span>
            <span v-else>{{ user?.isBanned ? '🔓 Разблокировать' : '🔒 Заблокировать' }}</span>
          </button>

          <button
            @click="handleDelete"
            :disabled="loading"
            class="flex-1 min-w-[120px] px-4 py-2 bg-red-500 hover:bg-red-600 disabled:bg-red-300 text-white rounded-lg font-bold text-sm transition-all duration-200 flex items-center justify-center gap-2"
          >
            <span
              v-if="loading"
              class="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"
            ></span>
            <span v-else>🗑️ Удалить</span>
          </button>

          <button
            @click="handleClose"
            class="flex-1 min-w-[120px] px-4 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg font-bold text-sm transition"
          >
            Закрыть
          </button>
        </div>

        <!-- Если админ – только кнопка "Закрыть" -->
        <div v-else class="flex justify-end pt-4 border-t border-gray-200">
          <button
            @click="handleClose"
            class="px-6 py-2 bg-gray-200 hover:bg-gray-300 text-gray-700 rounded-lg font-bold text-sm transition"
          >
            Закрыть
          </button>
        </div>

        <div v-if="errorMessage" class="text-red-600 text-sm bg-red-50 p-2 rounded">
          {{ errorMessage }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { User } from '@/types/user.types'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'

const props = defineProps<{
  visible: boolean
  user: User
  toggleBlock: (userId: string, isNoBanned: boolean) => Promise<User>
  deleteUser: (userId: string) => Promise<User>
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'updateUser', user: User): void
}>()

// Локальное состояние
const loading = ref(false)
const errorMessage = ref<string | null>(null)

// Является ли пользователь администратором
const isAdmin = computed(() => {
  if (!props.user) return false
  const roles = props.user.roles || []
  return roles.includes('ROLE_ADMIN')
})

// Вычисляемые
const fullName = computed(() => {
  if (!props.user) return ''
  const { firstName, lastName } = props.user.profile || {}
  return `${firstName ?? ''} ${lastName ?? ''}`.trim() || 'Без имени'
})

const formatDate = (dateString?: string) => {
  if (!dateString) return '—'
  const date = new Date(dateString)
  return new Intl.DateTimeFormat('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
    .format(date)
    .replace(',', '')
}

const getInitials = (user: User) => {
  const name = fullName.value
  return getInitialsFromName(name)
}

const avatarGradientClass = (userId: string) => {
  return getAvatarGradient(userId)
}

const getStatusColor = (status: string) => {
  return status === 'ONLINE' ? 'bg-green-400 text-gray-900' : 'bg-gray-200 text-gray-600'
}

const getStatusDotColor = (status: string) => {
  return status === 'ONLINE' ? 'bg-green-800' : 'bg-gray-500'
}

const isDeleted = computed(() => !!props.user?.isDeleted)

// Обработчики
const handleClose = () => {
  emit('close')
  errorMessage.value = null
  loading.value = false
}

const handleToggleBlock = async () => {
  if (!props.user) return
  if (isAdmin.value) {
    errorMessage.value = 'Невозможно заблокировать администратора'
    return
  }
  const action = props.user.isBanned ? 'разблокировать' : 'заблокировать'
  if (!confirm(`Вы уверены, что хотите ${action} пользователя ${fullName.value}?`)) return

  loading.value = true
  errorMessage.value = null

  try {
    const updatedUser = await props.toggleBlock(props.user.id, !props.user.isBanned)
    emit('updateUser', updatedUser)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Ошибка операции'
  } finally {
    loading.value = false
  }
}

const handleDelete = async () => {
  if (!props.user) return
  if (isAdmin.value) {
    errorMessage.value = 'Невозможно удалить администратора'
    return
  }
  if (!confirm(`Вы уверены, что хотите удалить пользователя ${fullName.value}?`)) return

  loading.value = true
  errorMessage.value = null
  try {
    // Получаем обновлённого пользователя
    const updatedUser = await props.deleteUser(props.user.id)
    // Обновляем данные в родителе, чтобы модалка показала новый статус
    emit('updateUser', updatedUser)
    // Не закрываем окно – пользователь видит, что аккаунт помечен как "Удален"
    // При желании можно закрыть через 2 секунды:
    // setTimeout(() => emit('close'), 2000)
  } catch (err) {
    errorMessage.value = err instanceof Error ? err.message : 'Ошибка удаления'
  } finally {
    loading.value = false
  }
}
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
