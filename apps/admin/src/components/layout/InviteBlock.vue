<template>
  <!-- Карточка генерации приглашений -->
  <div class="self-start bg-white rounded-xl shadow-lg border border-[#8295ff]/35 overflow-hidden">
    <div class="bg-blue-500 py-2 px-6">
      <h3 class="text-lg font-bold text-white flex items-center gap-2">
        Создать ссылку приглашение
      </h3>
    </div>
    <div class="p-6">
      <!-- Активные приглашения — статусная строка -->
      <div class="flex items-center justify-between mb-4">
        <div class="flex items-center gap-1.5 text-sm">
          <span class="text-gray-600">📨 Активных приглашений:</span>
          <span
            v-if="invitationsStore.isLoading"
            class="inline-flex items-center h-5 w-10 bg-gray-200 rounded-md animate-pulse"
          />
          <span
            v-else
            class="text-lg font-bold text-gray-800 tabular-nums"
          >{{ invitationsStore.count }}</span>
        </div>
        <button
          v-if="!invitationsStore.isLoading && invitationsStore.count > 0"
          @click="handleResetInvitations"
          :disabled="isResetting"
          class="h-7 px-2.5 bg-red-500 hover:bg-red-600 disabled:bg-gray-300 disabled:cursor-not-allowed text-white rounded-lg text-xs font-bold transition-all duration-200 flex items-center justify-center gap-1.5 shrink-0"
        >
          <svg v-if="isResetting" class="animate-spin w-3 h-3" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
            <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" class="opacity-25" />
            <path d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" class="opacity-75" fill="currentColor" />
          </svg>
          <span v-if="isResetting">Удаляется...</span>
          <span v-else>🗑️ Сбросить</span>
        </button>
      </div>

      <div class="space-y-1">
        <textarea
          ref="outputTextarea"
          readonly
          class="w-full h-36 p-4 border-2 border-gray-200 rounded-lg bg-gray-50 text-gray-700 font-mono text-sm resize-none focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all duration-200"
          placeholder="Загрузка текста..."
          v-model="invitationLink"
        ></textarea>
        <div class="flex-row gap-3">
          <div v-if="isInvitationCreated" class="flex items-center justify-center h-10">
            <p>Приглашение одноразовое, действует 24 часа!</p>
          </div>
          <div class="flex gap-6 justify-between">
            <button
              @click="copyToClipboard"
              class="flex-1 h-11 px-6 bg-gray-100 hover:bg-gray-200 text-gray-800 rounded-lg font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 flex items-center justify-center gap-2 shadow-sm"
            >
              📋 Копировать
            </button>
            <button
              @click="generateInvitation"
              :disabled="isLoading"
              class="flex-1 h-11 px-6 bg-blue-500 hover:bg-primary/90 text-white rounded-lg font-bold text-sm transition-all duration-200 hover:scale-105 active:scale-95 flex items-center justify-center gap-2 shadow-md disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <svg
                v-if="isLoading"
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
                />
                <path
                  class="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
              <span v-else>✨ Создать</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { invitationsService } from '@/services/http/InvitationsService'
import { useInvitationsStore } from '@/stores/invitationsStore'
import { useToastStore } from '@/stores/toastStore'

const toastStore = useToastStore()
const invitationsStore = useInvitationsStore()
const invitationLink = ref('')
const isLoading = ref(false)
const isResetting = ref(false)
const outputTextarea = ref<HTMLTextAreaElement | null>(null)
const isInvitationCreated = ref(false)

const handleResetInvitations = async () => {
  if (isResetting.value || invitationsStore.isLoading) return

  isResetting.value = true

  try {
    await invitationsStore.deleteAllInvitations()
    toastStore.success('Все приглашения удалены')
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Ошибка удаления'
    toastStore.error(`Не удалось удалить приглашения: ${msg}`)
  } finally {
    isResetting.value = false
  }
}

const createInvitation = async (): Promise<string> => {
  const invitation = await invitationsService.createInvitation()

  // ✅ Обновляем список активных приглашений после создания
  await invitationsStore.fetchActiveInvitations()
  isInvitationCreated.value = true
  return invitation.invitationUrl
}

const generateInvitation = async () => {
  if (isLoading.value) return
  isLoading.value = true
  try {
    const link = await createInvitation()
    toastStore.success('Пригласительная ссылка создана!')
    invitationLink.value = link
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'Ошибка удаления'
    toastStore.error(`Не удалось удалить приглашения: ${msg}`)
  } finally {
    isLoading.value = false
  }
}

const copyToClipboard = async () => {
  if (!invitationLink.value) return
  try {
    await navigator.clipboard.writeText(invitationLink.value)
  } catch (error) {
    console.error('Ошибка копирования:', error)
    // fallback
    if (outputTextarea.value) {
      outputTextarea.value.select()
      document.execCommand('copy')
    }
  }
}
</script>
