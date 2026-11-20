<template>
  <div>
    <!-- MARK: card -->
    <div class="bg-white rounded-xl shadow-lg border border-[#8295ff]/35 overflow-hidden">
      <div class="bg-blue-500 py-2 px-6">
        <h3 class="text-lg font-bold text-white flex items-center gap-1">Список пользователей</h3>
      </div>

      <!-- MARK: filters -->
      <div class="bg-linear-to-r from-gray-50 to-white border-b border-gray-200 p-5">
        <div class="flex flex-col md:flex-row gap-4 items-end">
          <div class="flex-1 w-full">
            <label
              for="user-search"
              class="block text-xs font-bold text-gray-600 uppercase tracking-wide mb-2 ml-1"
            >
              🔍 Поиск по имени пользователя
            </label>
            <div class="relative">
              <input
                id="user-search"
                type="text"
                v-model="searchInput"
                placeholder="Введите имя пользователя..."
                class="w-full h-11 px-4 pr-10 rounded-lg border-2 border-gray-200 bg-white text-gray-800 text-sm focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all duration-200"
                autocomplete="off"
                aria-label="Поиск по имени пользователя"
              />
              <button
                v-if="searchInput"
                type="button"
                @click="clearSearch"
                class="absolute right-2 top-1/2 -translate-y-1/2 w-7 h-7 flex items-center justify-center rounded-full text-gray-400 hover:text-gray-700 hover:bg-gray-100 transition-colors"
                aria-label="Очистить поиск"
              >
                ✕
              </button>
            </div>
          </div>

          <div class="flex-1 w-full">
            <label
              for="user-status"
              class="block text-xs font-bold text-gray-600 uppercase tracking-wide mb-2 ml-1"
            >
              📊 Статус
            </label>
            <select
              id="user-status"
              v-model="usersStore.statusFilter"
              @change="handleStatusChange"
              class="custom-select w-full h-11 px-4 pr-10 rounded-lg border-2 border-gray-200 bg-white text-gray-800 text-sm focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none cursor-pointer transition-all duration-200 appearance-none"
            >
              <option value="">Все статусы</option>
              <option value="ONLINE">🟢 Онлайн</option>
              <option value="OFFLINE">⚫ Офлайн</option>
            </select>
          </div>

          <div class="w-full md:w-auto">
            <button
              type="button"
              @click="handleReset"
              :disabled="!hasActiveFilters"
              class="w-full md:w-auto h-11 px-6 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 disabled:hover:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 shadow-md hover:shadow-lg flex items-center justify-center gap-2"
            >
             Сбросить фильтры
            </button>
          </div>
        </div>
      </div>

      <!-- MARK: states -->
      <div
        v-if="usersStore.error"
        class="bg-red-50 border-l-4 border-red-500 p-4 m-4 rounded-lg flex items-center justify-between gap-3"
        role="alert"
      >
        <span class="text-red-700 text-sm font-medium">{{ usersStore.error }}</span>
        <button
          type="button"
          @click="handlReloadUserTable"
          class="text-sm font-bold text-red-700 hover:underline"
        >
          Повторить
        </button>
      </div>

      <div
        v-if="usersStore.loading"
        class="bg-blue-50 border-l-4 border-blue-500 p-4 m-4 rounded-lg"
        role="status"
        aria-live="polite"
      >
        <div class="flex items-center gap-3">
          <div
            class="w-6 h-6 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"
          ></div>
          <span class="text-gray-700 font-medium">Загрузка данных...</span>
        </div>
      </div>

      <!-- MARK: toolbar -->
      <div class="flex items-center flex-wrap gap-x-3 gap-y-1 px-5 py-2 bg-white border-b border-gray-100">
        <!-- Счётчик найденных пользователей -->
        <div class="flex items-baseline gap-1.5 text-sm shrink-0">
          <span class="text-gray-600">Найдено пользователей:</span>
          <span class="text-lg font-bold text-blue-500 tabular-nums">{{ usersStore.totalElements }}</span>
        </div>

        <!-- Статистика (чипсы) -->
        <template v-if="!statStore.isLoading && statStore.totalUsers > 0">
          <span class="text-gray-300 mx-1 shrink-0">|</span>

          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-blue-50/50 text-blue-700 text-xs font-semibold whitespace-nowrap">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
            </svg>
            <span class="tabular-nums">{{ statStore.totalUsers }}</span>
          </span>

          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-green-50/50 text-green-700 text-xs font-semibold whitespace-nowrap">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5.5a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <polyline points="16 3 19 6 23 2" />
            </svg>
            <span class="tabular-nums">{{ statStore.activeUsers }}</span>
          </span>

          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-red-50/50 text-red-600 text-xs font-semibold whitespace-nowrap">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <polyline points="3 6 5 6 20 6" />
              <path d="M18 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
              <line x1="10" y1="11" x2="10" y2="17" />
            </svg>
            <span class="tabular-nums">{{ statStore.deletedUsers }}</span>
          </span>

          <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-md bg-amber-50/50 text-amber-700 text-xs font-semibold whitespace-nowrap">
            <svg class="w-3.5 h-3.5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" />
              <circle cx="9" cy="7" r="4" />
              <circle cx="9" cy="7" r="2" fill="currentColor" />
            </svg>
            <span class="tabular-nums">{{ statStore.onlineUsers }}</span>
          </span>
        </template>

        <!-- Скелетон статистики -->
        <span
          v-else-if="statStore.isLoading"
          class="inline-flex items-center gap-1 px-4 py-1 rounded-md bg-gray-100 animate-pulse text-transparent text-xs"
        >loading</span>

        <!-- Кнопка обновления -->
        <button
          type="button"
          @click="handlReloadUserTable"
          :disabled="usersStore.loading"
          class="ml-auto h-7 px-3 bg-blue-500 hover:bg-blue-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg font-bold text-xs transition-all duration-200 hover:scale-105 active:scale-95 shadow-sm flex items-center justify-center gap-1.5 shrink-0"
        >
          🔄 Обновить
        </button>
      </div>

      <!-- MARK: table -->
      <div class="overflow-x-auto" :aria-busy="usersStore.loading">
        <table class="w-full border-collapse">
          <!-- thead -->
          <thead class="bg-gray-100 border-b border-gray-300">
            <tr>
              <!-- sortable column -->
              <th
                v-for="col in columns"
                :key="col.field"
                scope="col"
                :aria-sort="ariaSort(col.field)"
                :class="[
                  'px-4 py-2 text-left text-sm font-bold text-gray-700 tracking-wider cursor-pointer hover:bg-gray-200 focus:bg-gray-200 focus:outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-blue-500 transition-colors select-none',
                  col.last ? '' : 'border-r border-gray-200',
                ]"
                tabindex="0"
                @click="handleSort(col.field)"
                @keydown.enter.prevent="handleSort(col.field)"
                @keydown.space.prevent="handleSort(col.field)"
              >
                <div class="flex items-center gap-1">
                  <span>{{ col.label }}</span>
                  <SortIcon
                    :field="col.field"
                    :sort-by="usersStore.sortBy"
                    :sort-order="usersStore.sortOrder"
                  />
                </div>
              </th>
            </tr>
          </thead>

          <!-- MARK: tbody -->
          <tbody id="usersTableBody">
            <!-- user row -->
            <tr
              v-for="user in usersStore.users"
              :key="user.id"
              class="border-b border-gray-100 hover:bg-gray-50 transition-colors duration-150 group even:bg-gray-50/30 cursor-pointer focus:outline-none focus-visible:bg-blue-50"
              tabindex="0"
              role="button"
              :aria-label="`Открыть детали: ${usersStore.getFullName(user)}`"
              @click="openUserDetails(user)"
              @keydown.enter.prevent="openUserDetails(user)"
            >
              <!-- avatar + ФИО -->
              <td class="px-3 py-2 border-r border-gray-100">
                <div class="flex items-center gap-3">
                  <div
                    :class="[
                      'w-10 h-10 rounded-full bg-gradient-to-br text-white flex items-center justify-center font-bold text-sm uppercase shadow-sm group-hover:scale-105 transition-transform shrink-0 overflow-hidden',
                      avatarGradientClass(user.id),
                    ]"
                  >
                    <img
                      v-if="getAvatarThumbUrl(user) && !failedAvatars.has(user.id)"
                      :src="getAvatarThumbUrl(user)!"
                      class="w-full h-full rounded-full object-cover"
                      :alt="`Аватар ${usersStore.getFullName(user)}`"
                      loading="lazy"
                      @error="markAvatarFailed(user.id)"
                    />
                    <span v-else-if="isDeleted(user)">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" aria-hidden="true">
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
                    <span v-else>{{ avatarInitialsForAvatar(user) }}</span>
                  </div>
                  <span class="text-gray-800 font-medium text-sm leading-tight">
                    {{ usersStore.getFullName(user) }}
                  </span>
                </div>
              </td>

              <!-- username + флаги -->
              <td class="px-3 py-2 border-r border-gray-100">
                <div class="flex justify-between items-center gap-3">
                  <span class="text-gray-800 font-medium text-sm">
                    {{ user.userName }}
                  </span>
                  <span v-if="user.isBanned" class="text-red-500" title="Заблокирован">
                    <svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true">
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
                  <span v-if="isAdmin(user)" title="Администратор">
                    <svg width="25" height="25" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path
                        d="M12 22C12 22 20 18 20 12V5L12 3L4 5V12C4 18 12 22 12 22Z"
                        fill="#2196F3"
                        stroke="#1565C0"
                        stroke-width="2"
                        stroke-linejoin="round"
                      />
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
                  <span v-if="isDeleted(user)" title="Удалён">
                    <svg viewBox="0 0 24 24" width="26" height="26" fill="#EF4444" aria-hidden="true">
                      <path
                        d="M16 9v10H8V9h8m-1.5-6h-5l-1 1H5v2h14V4h-3.5l-1-1zM18 7H6v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7z"
                      />
                    </svg>
                  </span>
                </div>
              </td>

              <!-- статус -->
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

              <!-- последний визит -->
              <td class="px-3 py-2 border-r border-gray-100 text-gray-600 text-sm">
                {{ formatDate(user.lastSeen?.toString()) }}
              </td>

              <!-- регистрация -->
              <td class="px-3 py-2 text-gray-600 text-sm">
                {{ formatDate(user.createdAt?.toString()) }}
              </td>
            </tr>

            <!-- пустое состояние -->
            <tr v-if="!usersStore.loading && usersStore.users.length === 0">
              <td colspan="5" class="px-5 py-12 text-center text-gray-500">
                <div class="flex flex-col items-center gap-2">
                  <svg
                    class="w-12 h-12 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                  >
                    <path
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      stroke-width="2"
                      d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z"
                    ></path>
                  </svg>
                  <span v-if="hasActiveFilters">Ничего не найдено. Попробуйте изменить фильтры.</span>
                  <span v-else>Пользователи отсутствуют</span>
                  <button
                    v-if="hasActiveFilters"
                    type="button"
                    @click="handleReset"
                    class="mt-2 text-sm font-bold text-blue-500 hover:underline"
                  >
                    Сбросить фильтры
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- MARK: pagination -->
      <div
        v-if="usersStore.totalPages > 0"
        class="flex justify-center items-center gap-3 py-5 px-4 bg-gray-50 border-t border-gray-200"
      >
        <button
          type="button"
          @click="usersStore.prevPage()"
          :disabled="usersStore.loading || usersStore.currentPage <= 1"
          class="px-4 py-2 border-2 border-gray-300 bg-white rounded-lg text-sm font-semibold text-gray-600 transition-all duration-200 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-white shadow-sm"
          aria-label="Предыдущая страница"
        >
          « Назад
        </button>

        <nav class="flex gap-2 items-center" aria-label="Пагинация">
          <template v-for="(item, idx) in pageItems" :key="`p-${idx}`">
            <span v-if="item === '...'" class="px-2 text-gray-400 select-none" aria-hidden="true">
              …
            </span>
            <button
              v-else
              type="button"
              @click="usersStore.setPage(item)"
              :disabled="usersStore.loading"
              :aria-current="usersStore.currentPage === item ? 'page' : undefined"
              :aria-label="`Страница ${item}`"
              :class="[
                'min-w-10 h-10 px-3 border-2 rounded-lg text-sm font-bold transition-all duration-200 disabled:cursor-not-allowed',
                usersStore.currentPage === item
                  ? 'border-blue-500 bg-blue-500 text-white shadow-md'
                  : 'border-gray-300 bg-white text-gray-600 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-50',
              ]"
            >
              {{ item }}
            </button>
          </template>
        </nav>

        <button
          type="button"
          @click="usersStore.nextPage()"
          :disabled="usersStore.loading || usersStore.currentPage >= usersStore.totalPages"
          class="px-4 py-2 border-2 border-gray-300 bg-white rounded-lg text-sm font-semibold text-gray-600 transition-all duration-200 hover:bg-gray-100 hover:border-gray-400 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-white shadow-sm"
          aria-label="Следующая страница"
        >
          Вперед »
        </button>
      </div>
    </div>
  </div>

  <!-- MARK: modal -->
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
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useUsersStore } from '@/stores/usersStore'
import { useStatisticsStore } from '@/stores/usersStatisticStore'
import SortIcon from '@/components/ui/SortIcon.vue'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'
import type { UserAdminResponse } from '@messenger/shared-schema'
import UserDetailsModal from '@/components/modals/UserDetailsModal.vue'

const usersStore = useUsersStore()
const statStore = useStatisticsStore()

// MARK: types
type SortField = 'firstName' | 'userName' | 'status' | 'lastSeen' | 'createdAt'

interface ColumnDef {
  field: SortField
  label: string
  last?: boolean
}

const columns: ColumnDef[] = [
  { field: 'firstName', label: 'Имя, Фамилия' },
  { field: 'userName', label: 'Имя пользователя' },
  { field: 'status', label: 'Статус' },
  { field: 'lastSeen', label: 'Дата посещения' },
  { field: 'createdAt', label: 'Дата регистрации', last: true },
]

// MARK: search
const searchInput = ref(usersStore.searchQuery)
let searchTimeout: ReturnType<typeof setTimeout> | null = null

watch(searchInput, (value) => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    usersStore.setSearchQuery(value)
  }, 500)
})

watch(
  () => usersStore.searchQuery,
  (value) => {
    if (value !== searchInput.value) searchInput.value = value
  },
)

const clearSearch = () => {
  searchInput.value = ''
  if (searchTimeout) clearTimeout(searchTimeout)
  usersStore.setSearchQuery('')
}

onUnmounted(() => {
  if (searchTimeout) clearTimeout(searchTimeout)
})

// MARK: filters
const handleStatusChange = () => {
  usersStore.setStatusFilter(usersStore.statusFilter)
}

const handleSort = (field: SortField) => {
  usersStore.setSort(field)
}

const handleReset = () => {
  if (searchTimeout) clearTimeout(searchTimeout)
  searchInput.value = ''
  usersStore.resetFilters()
}

// MARK: pagination
const pageItems = computed<(number | '...')[]>(() => {
  const total = usersStore.totalPages
  const current = usersStore.currentPage
  if (total <= 1) return total === 1 ? [1] : []

  const radius = 1
  const pages = new Set<number>([1, total])
  for (let p = current - radius; p <= current + radius; p++) {
    if (p >= 1 && p <= total) pages.add(p)
  }
  const sorted = [...pages].sort((a, b) => a - b)
  const result: (number | '...')[] = []
  for (let i = 0; i < sorted.length; i++) {
    if (i > 0 && sorted[i]! - sorted[i - 1]! > 1) result.push('...')
    result.push(sorted[i]!)
  }
  return result
})

const ariaSort = (field: SortField): 'ascending' | 'descending' | 'none' => {
  if (usersStore.sortBy !== field) return 'none'
  return usersStore.sortOrder === 'asc' ? 'ascending' : 'descending'
}

const hasActiveFilters = computed(
  () => !!searchInput.value || !!usersStore.statusFilter || !!usersStore.sortBy,
)

// MARK: avatars
const failedAvatars = ref<Set<string>>(new Set())

const markAvatarFailed = (id: string) => {
  failedAvatars.value = new Set(failedAvatars.value).add(id)
}

const avatarInitialsForAvatar = (user: UserAdminResponse) => {
  if (!user) return ''
  return getInitialsFromName(usersStore.getFullName(user))
}

const getAvatarThumbUrl = (user: UserAdminResponse): string | null => {
  return user?.avatarThumbUrl || null
}

const avatarGradientClass = (userId: string) => {
  return userId ? getAvatarGradient(userId) : 'bg-gradient-to-br from-gray-500 to-gray-700'
}

// MARK: utils
const formatDate = (dateString?: string) => {
  if (!dateString) return '—'
  const date = new Date(dateString)
  if (Number.isNaN(date.getTime())) return '—'
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

const isAdmin = (user: UserAdminResponse) => user?.role === 'admin'
const isDeleted = (user: UserAdminResponse | null | undefined): boolean =>
  user?.isDeleted === true

const handlReloadUserTable = () => {
  usersStore.fetchUsers()
}

// MARK: modal
const showModal = ref(false)
const selectedUser = ref<UserAdminResponse | null>(null)

const openUserDetails = (user: UserAdminResponse) => {
  selectedUser.value = user
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  selectedUser.value = null
}

const handleUpdateUser = (updatedUser: UserAdminResponse) => {
  selectedUser.value = updatedUser
}

onMounted(() => {
  usersStore.fetchUsers()

  // Инициализация статистики пользователей
  const token = localStorage.getItem('token')
  if (token) {
    statStore.init()
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
