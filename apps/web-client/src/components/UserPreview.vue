<template>
  <div
    @click="handleClick"
    class="flex items-center gap-4 p-3 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-800 transition rounded-lg"
  >
    <!-- Аватар с индикатором статуса -->
    <div class="shrink-0 relative">
      <div
        :class="[
          'w-14 h-14 rounded-full flex items-center justify-center shadow-xl text-white text-xl font-bold uppercase',
          avatarGradientClass,
        ]"
      >
        <img
          v-if="avatarUrl"
          :src="avatarUrl"
          class="w-full h-full rounded-full object-cover"
          alt="avatar"
        />
        <span v-else>{{ initials }}</span>
      </div>
      <!-- Индикатор онлайн/офлайн -->
      <div
        class="absolute bottom-0 right-0 w-3.5 h-3.5 bg-green-500 rounded-full border-2 border-white dark:border-gray-800"
        title="В сети"
      ></div>
    </div>

    <div class="flex-1 min-w-0 border-b border-b-gray-200 dark:border-b-gray-700 pb-3">
      <!-- Полное имя -->
      <h3 class="font-medium text-black dark:text-white text-xl truncate">
        {{ displayName }}
      </h3>
      <!-- Статус (онлайн/офлайн) -->
      <div></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { User } from '@messenger/shared'
import { getAvatarGradient, getInitialsFromName } from '@/utils/avatarGradient'

const props = defineProps<{
  user: User
}>()

const emit = defineEmits<{
  click: [user: User]
}>()

// Отображаемое имя (приоритет: firstName + lastName, firstName, userName)
const displayName = computed(() => {
  const firstName = props.user.profile?.firstName
  const lastName = props.user.profile?.lastName
  if (firstName && lastName) return `${firstName} ${lastName}`
  if (firstName) return firstName
  return props.user.userName
})

// Ссылка на аватар (предпочтительно миниатюра, затем оригинал)
const avatarUrl = computed(() => props.user.profile?.avatarThumbUrl || null)

// Градиент для аватара (если нет картинки)
const avatarGradientClass = computed(() => {
  if (avatarUrl.value) return ''
  return getAvatarGradient(props.user.id)
})

// Инициалы (через утилиту)
const initials = computed(() => {
  const firstName = props.user.profile?.firstName
  const lastName = props.user.profile?.lastName
  let name = ''
  if (firstName && lastName) name = `${firstName} ${lastName}`
  else if (firstName) name = firstName
  else name = props.user.userName
  return getInitialsFromName(name)
})

const handleClick = () => {
  emit('click', props.user)
}
</script>
