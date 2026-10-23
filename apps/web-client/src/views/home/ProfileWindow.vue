<template>
  <div class="flex justify-center p-4">
    <!-- Основной контент (когда пользователь загружен) -->
    <div
      v-if="userStore.user && userStore.isInitialized && !userStore.loading"
      class="w-full max-w-sm bg-white dark:bg-gray-900 rounded-3xl shadow-2xl overflow-hidden"
    >
      <!-- Аватар с обводкой -->
      <div
        class="pt-8 pb-4 bg-linear-to-b from-blue-50 to-white dark:from-gray-800 dark:to-gray-900"
      >
        <div class="flex justify-center" @click="openImageModal">
          <div class="relative">
            <div
              :class="[
                'w-40 h-40 rounded-full flex items-center justify-center shadow-xl ring-4 ring-white dark:ring-gray-800 transition-opacity',
                avatarGradientClass,
                isUploading ? 'opacity-50' : '',
              ]"
            >
              <img
                v-if="avatarUrl"
                :src="avatarUrl"
                :key="avatarKey"
                class="w-full h-full rounded-full object-cover"
                alt="avatar"
              />
              <span v-else class="text-white text-6xl font-bold uppercase">
                {{ avatarInitials }}
              </span>
            </div>
            <!-- Спиннер при загрузке (поверх аватара) -->
            <div v-if="isUploading" class="absolute inset-0 flex items-center justify-center">
              <div
                class="w-8 h-8 border-4 border-white border-t-transparent rounded-full animate-spin"
              ></div>
            </div>
            <!-- Онлайн индикатор -->
            <div
              v-if="isOnline"
              class="absolute bottom-2 right-2 w-5 h-5 bg-green-500 rounded-full border-2 border-white dark:border-gray-800"
              title="В сети"
            ></div>
          </div>
        </div>

        <!-- Кнопка загрузки аватара -->
        <div class="flex justify-center mt-4">
          <button
            @click="triggerFileInput"
            :disabled="isUploading"
            class="flex items-center gap-2 px-4 py-2 bg-blue-50 dark:bg-gray-700 hover:bg-blue-100 dark:hover:bg-gray-700 text-blue-600 dark:text-blue-400 rounded-full transition-all duration-200 text-sm font-medium shadow-sm hover:shadow focus:outline-none focus:ring-2 focus:ring-blue-300 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12"
              />
            </svg>
            <span>{{ avatarUrl ? 'Сменить аватар' : 'Загрузить аватар' }}</span>
          </button>
        </div>

        <!-- Инлайн-редактирование имени и фамилии -->
        <div class="text-center mt-4 px-6">
          <!-- Режим редактирования -->
          <div v-if="editingName" class="space-y-3">
            <div>
              <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">Имя</label>
              <input
                v-model="editFirstName"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400 dark:bg-gray-800 dark:text-white"
                placeholder="Имя"
              />
            </div>
            <div>
              <label class="block text-xs text-gray-500 dark:text-gray-400 mb-1">Фамилия</label>
              <input
                v-model="editLastName"
                type="text"
                class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400 dark:bg-gray-800 dark:text-white"
                placeholder="Фамилия (необязательно)"
              />
            </div>
            <div class="flex justify-center gap-2 mt-2">
              <button
                @click="saveName"
                :disabled="isSaving"
                class="px-4 py-1.5 bg-green-500 text-white rounded-full text-sm hover:bg-green-600 transition disabled:opacity-50"
              >
                {{ isSaving ? 'Сохранение...' : 'Сохранить' }}
              </button>
              <button
                @click="cancelEditName"
                class="px-4 py-1.5 bg-gray-300 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-full text-sm hover:bg-gray-400 dark:hover:bg-gray-600 transition"
              >
                Отмена
              </button>
            </div>
          </div>
          <!-- Режим просмотра -->
          <div v-else>
            <div class="flex justify-center items-center gap-2">
              <h2 class="text-2xl font-bold text-gray-800 dark:text-white">
                {{ userStore.fullName || 'Без имени' }}
              </h2>
              <button @click="startEditName" class="text-gray-400 hover:text-blue-500 transition">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    stroke-width="2"
                    d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
                  />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- Инлайн-редактирование имени пользователя -->
        <div class="text-center mt-3 px-6">
          <p class="text-sm font-medium text-gray-400 uppercase tracking-wide mb-1">
            Имя пользователя
          </p>
          <!-- РЕЖИМ РЕДАКТИРОВАНИЯ USERNAME -->
          <div v-if="editingUsername" class="flex flex-col items-center gap-2">
            <input
              v-model="editUsername"
              type="text"
              class="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-400 dark:bg-gray-800 dark:text-white"
              placeholder="@username"
            />
            <div class="flex gap-2">
              <button
                @click="saveUsername"
                :disabled="isSaving"
                class="px-4 py-1.5 bg-green-500 text-white rounded-full text-sm hover:bg-green-600 transition disabled:opacity-50"
              >
                {{ isSaving ? 'Сохранение...' : 'Сохранить' }}
              </button>
              <button
                @click="cancelEditUsername"
                class="px-4 py-1.5 bg-gray-300 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-full text-sm hover:bg-gray-400 dark:hover:bg-gray-600 transition"
              >
                Отмена
              </button>
            </div>
            <p v-if="usernameError" class="text-xs text-red-500 mt-1">{{ usernameError }}</p>
          </div>
          <!-- РЕЖИМ ПРОСМОТРА USERNAME -->
          <div v-else class="flex justify-center items-center gap-2">
            <button
              @click="copyUsername"
              class="text-gray-400 hover:text-blue-400 transition"
              title="Копировать username"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M8 5H6a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2v-1M8 5a2 2 0 002 2h2a2 2 0 002-2M8 5a2 2 0 012-2h2a2 2 0 012 2m0 0h2a2 2 0 012 2v3m2 4H10m0 0l3-3m-3 3l3 3"
                />
              </svg>
            </button>
            <p class="text-lg text-gray-900 dark:text-white">@{{ userStore.user.userName }}</p>
            <button
              @click="startEditUsername"
              class="text-gray-400 hover:text-blue-500 transition"
              title="Редактировать username"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  stroke-width="2"
                  d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"
                />
              </svg>
            </button>
          </div>
        </div>

        <!-- Дополнительная информация -->
        <div class="mt-4 px-6 text-center">
          <p class="text-sm text-gray-400 dark:text-gray-500 mt-1">
            Зарегистрирован: {{ formatDate(userStore.user.createdAt.toString()) }}
          </p>
        </div>
      </div>

      <!-- Кнопка выхода и удаления аккаунта -->
      <div class="px-6 pb-6 flex justify-between gap-3">
        <!-- Кнопка выхода -->
        <button
          @click="confirmLogout"
          class="flex items-center justify-center gap-2 text-sm py-1.5 px-3 rounded-xl transition-colors text-gray-600 hover:text-gray-800 dark:text-red-400 dark:hover:text-red-300 font-medium flex-shrink-0 min-w-[100px]"
        >
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"
            />
          </svg>
          <span class="leading-none">Выйти</span>
        </button>

        <!-- Кнопка удалить аккаунт -->
        <button
          @click="confirmDeleteAccount"
          class="flex items-center justify-center gap-2 text-sm py-1.5 px-3 rounded-xl transition-colors hover:bg-red-50 dark:hover:bg-red-900/20 text-red-600 hover:text-red-700 dark:text-red-500 dark:hover:text-red-400 font-medium flex-shrink-0 min-w-[100px]"
        >
          <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
            />
          </svg>
          <span class="leading-none">Удалить</span>
        </button>
      </div>
    </div>

    <!-- Состояние загрузки – спиннер внутри аватара -->
    <div
      v-else-if="!userStore.isInitialized || userStore.loading"
      class="w-full max-w-sm bg-white dark:bg-gray-900 rounded-3xl shadow-2xl overflow-hidden"
    >
      <div
        class="pt-8 pb-4 bg-gradient-to-b from-blue-50 to-white dark:from-gray-800 dark:to-gray-900"
      >
        <div class="flex justify-center">
          <div class="relative">
            <div
              class="w-40 h-40 rounded-full bg-gray-200 dark:bg-gray-700 flex items-center justify-center shadow-xl ring-4 ring-white dark:ring-gray-800"
            >
              <div
                class="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
              ></div>
            </div>
          </div>
        </div>
        <div class="text-center mt-4 px-6">
          <div class="h-6 bg-gray-200 dark:bg-gray-700 rounded w-32 mx-auto mb-2"></div>
          <div class="h-4 bg-gray-200 dark:bg-gray-700 rounded w-40 mx-auto"></div>
        </div>
      </div>
      <div class="px-6 pb-6">
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded-xl mb-3"></div>
        <div class="h-10 bg-gray-200 dark:bg-gray-700 rounded-xl"></div>
      </div>
    </div>

    <!-- Не авторизован -->
    <div v-else class="w-full max-w-sm bg-white dark:bg-gray-900 rounded-3xl shadow-2xl p-8">
      <div class="text-center">
        <div class="text-gray-400 dark:text-gray-500 text-6xl mb-4">👤</div>
        <p class="text-gray-500 dark:text-gray-400 mb-4">Вы не авторизованы</p>
        <button
          @click="goToLogin"
          class="bg-blue-500 hover:bg-blue-600 text-white px-4 py-2 rounded-xl transition-colors"
        >
          Войти
        </button>
      </div>
    </div>

    <!-- Toast уведомление -->
    <Transition
      enter-active-class="transition ease-out duration-200"
      enter-from-class="opacity-0 transform translate-y-2"
      enter-to-class="opacity-100 transform translate-y-0"
      leave-active-class="transition ease-in duration-150"
      leave-from-class="opacity-100 transform translate-y-0"
      leave-to-class="opacity-0 transform translate-y-2"
    >
      <div
        v-if="showToast"
        class="fixed bottom-4 left-1/2 transform -translate-x-1/2 bg-gray-800 text-white px-4 py-2 rounded-lg text-sm shadow-lg z-50"
      >
        Скопировано! 📋
      </div>
    </Transition>

    <!-- Модальное окно подтверждения выхода -->
    <ConfirmDialog
      v-model="showLogoutConfirm"
      title="Выйти из аккаунта?"
      description="Вы уверены, что хотите выйти? Вам потребуется снова войти, чтобы продолжить."
      confirm-text="Выйти"
      confirm-button-class="bg-red-600 hover:bg-red-700"
      @confirm="proceedLogout"
    />

    <!-- Модальное окно подтверждения удаления аккаунта -->
    <ConfirmDialog
      v-model="showDeleteConfirm"
      title="Удалить аккаунт?"
      description="Это действие необратимо. Все ваши данные, сообщения и файлы будут навсегда удалены. Вы уверены, что хотите продолжить?"
      confirm-text="Удалить"
      confirm-button-class="bg-red-700 hover:bg-red-800"
      @confirm="proceedDeleteAccount"
    />

    <!-- Скрытый input для выбора файла -->
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      class="hidden"
      @change="handleFileSelect"
    />
  </div>

  <!-- Модалка для просмотра аватара -->
  <AvatarPreviewModal
    :visible="isImageModalOpen"
    :image-url="avatarUrl || ''"
    :alt="userStore.fullName || 'Аватар пользователя'"
    @close="closeImageModal"
    @error="console.error('Ошибка загрузки аватара')"
  />
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/userStore'
import { useRouter } from 'vue-router'
import { compressImageIfNeeded } from '@/services/business/ImageCompressor'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'
import AvatarPreviewModal from '@/components/modals/AvatarPreviewModal.vue'
import ConfirmDialog from '@/components/ui/ConfirmDialog.vue'

const userStore = useUserStore()
const router = useRouter()
const showToast = ref(false)
const showLogoutConfirm = ref(false)
const showDeleteConfirm = ref(false) // новое состояние
const isUploading = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
const avatarKey = ref(0)

// Состояния для инлайн-редактирования
const editingName = ref(false)
const editingUsername = ref(false)
const editFirstName = ref('')
const editLastName = ref('')
const editUsername = ref('')
const isSaving = ref(false)
const usernameError = ref('')

// ==================== Computed свойства ====================
const isOnline = computed(() => {
  const userId = userStore.user?.id
  if (!userId) return false
  const status = userStore.getUserStatus(userId)
  return status === 'ONLINE'
})

const avatarUrl = computed(() => userStore.user?.profile?.avatarUrl || null)

const avatarGradientClass = computed(() => {
  if (avatarUrl.value) return ''
  const userId = userStore.user?.id
  if (userId) return getAvatarGradient(userId)
  return 'bg-gradient-to-br from-gray-500 to-gray-700'
})

const avatarInitials = computed(() => {
  const user = userStore.user
  if (!user) return '?'
  const fullName = userStore.fullName || user.userName || ''
  return getInitialsFromName(fullName)
})

// ==================== Методы аватара ====================
const triggerFileInput = () => {
  fileInput.value?.click()
}

const handleFileSelect = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  const allowedTypes = ['image/jpeg', 'image/png', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    alert('Неподдерживаемый формат. Используйте JPEG, PNG или WEBP.')
    input.value = ''
    return
  }

  isUploading.value = true
  try {
    const compressedFile = await compressImageIfNeeded(file, 2, 2400, 2400)
    await userStore.uploadAvatar(compressedFile)
    avatarKey.value++
  } catch (error) {
    console.error('Ошибка загрузки аватара:', error)
    alert('Не удалось загрузить аватар. Попробуйте позже.')
  } finally {
    isUploading.value = false
    input.value = ''
  }
}

// ==================== Редактирование имени и фамилии ====================
const startEditName = () => {
  editFirstName.value = userStore.user?.profile?.firstName || ''
  editLastName.value = userStore.user?.profile?.lastName || ''
  editingName.value = true
}

const cancelEditName = () => {
  editingName.value = false
  editFirstName.value = ''
  editLastName.value = ''
}

const saveName = async () => {
  const firstName = editFirstName.value.trim()
  if (!firstName) {
    alert('Имя не может быть пустым')
    return
  }
  isSaving.value = true
  try {
    const lastName = editLastName.value.trim()
    await userStore.updateProfile({
      firstName,
      lastName: lastName === '' ? null : lastName,
    })
    editingName.value = false
  } catch (error) {
    console.error('Ошибка сохранения имени:', error)
    alert('Не удалось сохранить имя. Попробуйте позже.')
  } finally {
    isSaving.value = false
  }
}

// ==================== Редактирование username ====================
const startEditUsername = () => {
  editUsername.value = userStore.user?.userName || ''
  usernameError.value = ''
  editingUsername.value = true
}

const cancelEditUsername = () => {
  editingUsername.value = false
  editUsername.value = ''
  usernameError.value = ''
}

const saveUsername = async () => {
  const newUsername = editUsername.value.trim()
  if (!newUsername) {
    usernameError.value = 'Имя пользователя не может быть пустым'
    return
  }
  if (!/^[a-zA-Z0-9_]{3,20}$/.test(newUsername)) {
    usernameError.value = 'Только латиница, цифры и _, 3–20 символов'
    return
  }
  isSaving.value = true
  usernameError.value = ''
  try {
    await userStore.updateUserName(newUsername)
    editingUsername.value = false
  } catch (error: any) {
    if (error.response?.status === 409) {
      usernameError.value = 'Имя пользователя уже занято'
    } else {
      usernameError.value = 'Ошибка сервера. Попробуйте позже.'
    }
    console.error(error)
  } finally {
    isSaving.value = false
  }
}

// ==================== Другие методы ====================
const copyUsername = async () => {
  const username = userStore.user?.userName
  if (!username) return

  try {
    // 1. Пытаемся использовать современный Clipboard API (десктоп, новые браузеры)
    await navigator.clipboard.writeText(username)
    showToast.value = true
    setTimeout(() => (showToast.value = false), 2000)
  } catch (err) {
    // 2. Fallback для мобильных устройств и старых браузеров (через execCommand)
    const textArea = document.createElement('textarea')
    textArea.value = username
    // Скрываем элемент
    textArea.style.position = 'fixed'
    textArea.style.top = '-9999px'
    textArea.style.left = '-9999px'
    textArea.style.width = '1px'
    textArea.style.height = '1px'
    document.body.appendChild(textArea)
    textArea.focus()
    textArea.select()

    try {
      const successful = document.execCommand('copy')
      document.body.removeChild(textArea)
      if (successful) {
        showToast.value = true
        setTimeout(() => (showToast.value = false), 2000)
      } else {
        // Если execCommand не сработал (бывает на некоторых устройствах)
        alert('Не удалось скопировать. Попробуйте вручную.')
      }
    } catch (e) {
      document.body.removeChild(textArea)
      alert('Не удалось скопировать. Попробуйте вручную.')
    }
  }
}

const confirmLogout = () => {
  showLogoutConfirm.value = true
}

const closeLogoutConfirm = () => {
  showLogoutConfirm.value = false
}

const proceedLogout = () => {
  userStore.logout()
  router.push('/login')
  closeLogoutConfirm()
}

// ==================== Удаление аккаунта ====================
const confirmDeleteAccount = () => {
  showDeleteConfirm.value = true
}

const proceedDeleteAccount = async () => {
  try {
    await userStore.deleteAccount() // предполагаем, что такой метод есть в userStore
    userStore.clearSession() // очищаем сессию после удаления
    router.push('/login')
  } catch (error) {
    console.error('Ошибка удаления аккаунта:', error)
    alert('Не удалось удалить аккаунт. Попробуйте позже.')
  } finally {
    showDeleteConfirm.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}

const formatDate = (dateString?: string) => {
  if (!dateString) return '—'
  const date = new Date(dateString)
  return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit', year: 'numeric' })
}

const isImageModalOpen = ref(false)

const openImageModal = () => {
  if (avatarUrl.value) {
    isImageModalOpen.value = true
  }
}

const closeImageModal = () => {
  isImageModalOpen.value = false
}
</script>
