<template>
  <section class="flex-row items-center shadow-lg p-3 rounded-xl border border-[#8295ff]/35">
    <div class="flex justify-center">
      <h3 class="text-xl">Активных приглашений</h3>
    </div>
    <div class="flex justify-center">
      <span v-if="invitationsStore.isLoading" class="pt-2 text-xl font-bold"> Загрузка... </span>
      <span v-else-if="invitationsStore.error" class="pt-2 text-xl font-bold text-red-500">
        Ошибка:{{ invitationsStore.error }}
      </span>
      <span v-else class="pt-2 text-xl font-bold">
        {{ invitationsStore.count }}
      </span>
    </div>
  </section>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useInvitationsStore } from '@/stores/invitations'

const invitationsStore = useInvitationsStore()

// Загружаем статистику при загрузке приложения
onMounted(() => {
  // Проверяем, есть ли токен (пользователь авторизован)
  const token = localStorage.getItem('token')
  if (token) {
    invitationsStore.init()
  }
})
</script>
