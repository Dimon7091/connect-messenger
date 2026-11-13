<template>
  <div class="col-span-2">
    <!-- Карточка управления пользователями -->
    <div class="bg-white rounded-xl shadow-lg border border-[#8295ff]/35 overflow-hidden">
      <div class="bg-blue-500 py-2 px-6">
        <h3 class="text-lg font-bold text-white flex items-center gap-1">Список пользователей</h3>
      </div>

      <!-- Блок фильтрации (без изменений) -->
      <div class="bg-linear-to-r from-gray-50 to-white border-b border-gray-200 p-5">
        <div class="flex flex-col md:flex-row gap-4 items-end">
          <!-- Поиск по имени -->
          <div class="flex-1 w-full">
            <label class="block text-xs font-bold text-gray-600 uppercase tracking-wide mb-2 ml-1">
              🔍 Поиск по имени пользователя
            </label>
            <input
              type="text"
              v-model="usersStore.searchQuery"
              @input="handleSearch"
              placeholder="Введите имя пользователя..."
              class="w-full h-11 px-4 rounded-lg border-2 border-gray-200 bg-white text-gray-800 text-sm focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all duration-200"
              autocomplete="off"
            />
          </div>

          <!-- Фильтр по статусу -->
          <div class="flex-1 w-full">
            <label class="block text-xs font-bold text-gray-600 uppercase tracking-wide mb-2 ml-1">
              📊 Статус
            </label>
            <select
              v-model="usersStore.statusFilter"
              @change="handleStatusChange"
              class="custom-select w-full h-11 px-4 pr-10 rounded-lg border-2 border-gray-200 bg-white text-gray-800 text-sm focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none cursor-pointer transition-all duration-200 appearance-none"
            >
              <option value="">Все статусы</option>
              <option value="Онлайн">🟢 Онлайн</option>
              <option value="Офлайн">⚫ Офлайн</option>
            </select>
          </div>

          <!-- Кнопка сброса -->
          <div class="w-full md:w-auto">
            <button
              @click="handleReset"
              class="w-full md:w-auto h-11 px-6 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 shadow-md hover:shadow-lg flex items-center justify-center gap-2"
            >
              🔄 Сбросить фильтры
            </button>
          </div>
        </div>
      </div>

      <!-- Индикатор загрузки -->
      <div
        v-if="usersStore.loading"
        class="bg-blue-50 border-l-4 border-blue-500 p-4 m-4 rounded-lg"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-6 h-6 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
          ></div>
          <span class="text-gray-700 font-medium">Загрузка данных...</span>
        </div>
      </div>

      <!-- Статистика -->
      <div class="flex items-center gap-4 px-5 pt-2 pb-3 bg-white border-b border-gray-100">
        <div class="flex items-baseline gap-2 text-sm">
          <span class="text-gray-600">Найдено пользователей:</span>
          <span class="text-lg font-bold text-blue-500">{{ usersStore.totalElements }}</span>
        </div>
        <button
          @click="handlReloadUserTable"
          class="w-full md:w-auto h-8 px-4 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 shadow-md hover:shadow-lg flex items-center justify-center gap-2"
        >
          🔄 Обновить
        </button>
      </div>

      <!-- Таблица пользователей -->
      <div class="overflow-x-auto">
        <table class="w-full border-collapse">
          <!-- заголовок -->
          <thead class="bg-gray-100 border-b-1 border-gray-300">
            <tr>
              <!-- заголовок "Имя, Фамилия" -->
              <th
                class="px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider border-r border-gray-200 cursor-pointer hover:bg-gray-200 transition-colors select-none"
                @click="handleSort('firstName')"
              >
                <div class="flex items-center gap-1">
                  <span>Имя, Фамилия</span>
                  <SortIcon
                    :field="'firstName'"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>

              <!-- заголовок "Имя пользователя" -->
              <th
                class="px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider border-r border-gray-200 cursor-pointer hover:bg-gray-200 transition-colors select-none"
                @click="handleSort('userName')"
              >
                <div class="flex items-center gap-1">
                  <span>Имя пользователя</span>
                  <SortIcon
                    :field="'userName'"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>

              <!-- заголовок "Статус" -->
              <th
                class="px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider border-r border-gray-200 cursor-pointer hover:bg-gray-200 transition-colors select-none"
                @click="handleSort('status')"
              >
                <div class="flex items-center gap-1">
                  <span>Статус</span>
                  <SortIcon
                    :field="'status'"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>

              <!-- заголовок "Дата посещения" -->
              <th
                class="px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider border-r border-gray-200 cursor-pointer hover:bg-gray-200 transition-colors select-none"
                @click="handleSort('lastSeen')"
              >
                <div class="flex items-center gap-1">
                  <span>Дата посещения</span>
                  <SortIcon
                    :field="'lastSeen'"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>

              <!-- заголовок "Дата регистрации" -->
              <th
                class="px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider cursor-pointer hover:bg-gray-200 transition-colors select-none"
                @click="handleSort('createdAt')"
              >
                <div class="flex items-center gap-1">
                  <span>Дата регистрации</span>
                  <SortIcon
                    :field="'createdAt'"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>
            </tr>
          </thead>

          <!-- Тело (без изменений) -->
          <tbody id="usersTableBody">
            <tr
              v-for="user in usersStore.users"
              :key="user.id"
              class="border-b border-gray-100 hover:bg-gray-50 transition-colors duration-150 group even:bg-gray-50/30"
              @click="openUserDetails(user)"
            >
              <!-- столбец имя фамилия-->
              <td class="px-3 py-2 border-r border-gray-100">
                <div class="flex items-center gap-3">
                  <!-- аватар -->
                  <div
                    :class="[
                      'w-10 h-10 rounded-full bg-linear-to-br text-white flex items-center justify-center font-bold text-sm uppercase shadow-sm group-hover:scale-105 transition-transform shrink-0',
                      avatarGradientClass(user.id),
                    ]"
                  >
                    <img
                      v-if="getAvatarThumbUrl(user)"
                      :src="getAvatarThumbUrl(user)"
                      class="w-full h-full rounded-full object-cover"
                      alt="avatar"
                    />
                    <span v-else-if="isDeleted(user)">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
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
                    <span v-else>{{ avatarInitialsForAvatar(user) }}</span>
                  </div>
                  <span class="text-gray-800 font-medium text-sm leading-tight">
                    {{ usersStore.getFullName(user) }}
                  </span>
                </div>
              </td>
              <!-- столбец имя пользователя -->
              <td class="px-3 py-2 border-r border-gray-100">
                <div class="flex justify-between items-center gap-3">
                  <span class="text-gray-800 font-medium text-sm">
                    {{ user.userName }}
                  </span>
                  <!-- Иконка заблокированного юзера -->
                  <span class="text-red" v-if="user.isBanned">
                    <svg viewBox="0 0 24 24" width="22" height="22">
                      <path
                        d="M 7 10 V 7 A 5 5 0 0 1 17 7 V 10"
                        fill="none"
                        stroke="red"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                      <rect
                        x="4"
                        y="10"
                        width="16"
                        height="11"
                        rx="3"
                        fill="none"
                        stroke="red"
                        stroke-width="2"
                      />

                      <circle cx="12" cy="15.5" r="1.5" fill="red" />
                    </svg>
                  </span>
                  <!-- Иконка админа -->
                  <span v-if="isAdmin(user)">
                    <svg width="25" height="25" viewBox="0 0 24 24" fill="none">
                      <!-- Щит (фон) -->
                      <path
                        d="M12 22C12 22 20 18 20 12V5L12 3L4 5V12C4 18 12 22 12 22Z"
                        fill="#2196F3"
                        stroke="#1565C0"
                        stroke-width="2"
                        stroke-linejoin="round"
                      />
                      <!-- Буква А по центру -->
                      <text
                        x="12"
                        y="15.5"
                        fill="#FFFFFF"
                        font-family="Arial, sans-serif"
                        font-weight="bold"
                        font-size="12"
                        text-anchor="middle"
                      >
                        A
                      </text>
                    </svg>
                  </span>
                  <!-- Иконка удаленного юзера -->
                  <span v-if="isDeleted(user)">
                    <svg viewBox="0 0 24 24" width="26" height="26" fill="#EF4444">
                      <path
                        d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"
                      />
                    </svg>
                  </span>
                </div>
              </td>
              <!-- столбец статус -->
              <td class="px-3 py-2 border-r border-gray-100">
                <span
                  :class="[
                    'inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold',
                    usersStore.getStatusColor(user.status),
                  ]"
                >
                  <span
                    :class="[
                      'w-1.5 h-1.5 rounded-full',
                      usersStore.getStatusDotColor(user.status),
                      user.status === 'ONLINE' ? 'animate-pulse' : '',
                    ]"
                  ></span>
                  {{ user.status.toLowerCase() }}
                </span>
              </td>
              <!-- столбец дата посещения-->
              <td class="px-3 py-2 border-r border-gray-100 text-gray-600 text-sm">
                {{ formatDate(user.lastSeen) }}
              </td>
              <!-- столбец дата регистрации -->
              <td class="px-3 py-2 text-gray-600 text-sm">
                {{ formatDate(user.createdAt) }}
              </td>
            </tr>

            <!-- Пустое состояние -->
            <tr v-if="!usersStore.loading && usersStore.users.length === 0">
              <td colspan="5" class="px-5 py-12 text-center text-gray-500">
                <div class="flex flex-col items-center gap-2">
                  <svg
                    class="w-12 h-12 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  <span>Пользователи не найдены</span>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Пагинация (без изменений) -->
      <div
        v-if="usersStore.totalPages > 0"
        class="flex justify-center items-center gap-3 py-5 px-4 bg-gray-50 border-t border-gray-200"
      >
        <button
          @click="usersStore.prevPage()"
          :disabled="usersStore.currentPage === 0"
          class="px-4 py-2 border-2 border-gray-300 bg-white rounded-lg text-sm font-semibold text-gray-600 transition-all duration-200 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-white shadow-sm"
        >
          « Назад
        </button>

        <div class="flex gap-2 items-center">
          <button
            v-for="page in visiblePages"
            :key="page"
            @click="usersStore.setPage(page - 1)"
            :class="[
              'min-w-10 h-10 px-3 border-2 rounded-lg text-sm font-bold transition-all duration-200',
              usersStore.currentPage === page - 1
                ? 'border-blue-500 bg-blue-500 text-white shadow-md'
                : 'border-gray-300 bg-white text-gray-600 hover:bg-gray-100 hover:border-gray-400',
            ]"
          >
            {{ page }}
          </button>
        </div>

        <button
          @click="usersStore.nextPage()"
          :disabled="usersStore.currentPage + 1 >= usersStore.totalPages"
          class="px-4 py-2 border-2 border-gray-300 bg-white rounded-lg text-sm font-semibold text-gray-600 transition-all duration-200 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-white shadow-sm"
        >
          Вперед »
        </button>
      </div>
    </div>
  </div>
  <!-- Модальное окно -->
  <UserDetailsModal
    :visible="showModal"
    :user="selectedUser"
    @close="closeModal"
    @update-user="handleUpdateUser"
    :toggle-block="usersStore.setUserBlock"
    :delete-user="usersStore.deleteUser"
  />
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useUsersStore } from '@/stores/users'
import SortIcon from '@/components/ui/SortIcon.vue'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'
import type { User } from '@/types/user.types'
import UserDetailsModal from '@/components/modals/UserDetailsModal.vue'

const usersStore = useUsersStore()

// Debounce для поиска
let searchTimeout: number | null = null

const handleSearch = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    usersStore.setSearchQuery(usersStore.searchQuery)
  }, 500)
}

const handleStatusChange = () => {
  usersStore.setStatusFilter(usersStore.statusFilter)
}

const handleReset = () => {
  usersStore.resetFilters()
  // Сброс сортировки также можно сделать внутри resetFilters
  // или вызвать отдельно:
  // usersStore.setSort(null) // если есть такой метод
}

// Обработчик сортировки
const handleSort = (field: string) => {
  usersStore.setSort(field)
}

// Форматирование даты
const formatDate = (dateString: string) => {
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

const avatarInitialsForAvatar = (user: User) => {
  if (!user) return ''
  const fullName = usersStore.getFullName(user)
  return getInitialsFromName(fullName)
}
// Функция для получения URL аватара (если есть)
const getAvatarThumbUrl = (user: User) => {
  return user?.profile?.avatarThumbUrl || null
}

// Функция для градиента – принимает userId (строка)
const avatarGradientClass = (userId: string) => {
  if (userId) {
    return getAvatarGradient(userId)
  }
  return 'bg-gradient-to-br from-gray-500 to-gray-700'
}

const isAdmin = (user: User) => {
  if (!user) return false
  const roles = user.roles || []
  if (roles.includes('ROLE_ADMIN')) {
    return true
  }
  return false
}

const isDeleted = (user: User | null | undefined): boolean => {
  return user?.isDeleted === true
}

// Пагинация - видимые страницы
const visiblePages = computed(() => {
  const total = usersStore.totalPages
  const current = usersStore.currentPage
  const delta = 2
  const range: number[] = []

  for (let i = Math.max(0, current - delta); i <= Math.min(total - 1, current + delta); i++) {
    range.push(i + 1)
  }

  return range
})

const handlReloadUserTable = () => {
  usersStore.fetchUsers()
}

// Модальное окно
const showModal = ref(false)
const selectedUser = ref<User | null>(null)

const openUserDetails = (user: User) => {
  selectedUser.value = user
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleUpdateUser = (updatedUser: User) => {
  selectedUser.value = updatedUser // обновляем выбранного пользователя в модалке
  // таблица уже обновлена внутри setUserBlock
}

// Загружаем данные при монтировании
onMounted(() => {
  usersStore.fetchUsers()
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
