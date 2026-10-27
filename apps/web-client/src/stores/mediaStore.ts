// stores/mediaStore.ts
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { s3UploadService } from '@/services/http/S3UploadService'

interface CachedUrl {
  url: string
  expiresAt: number
  timerId?: number
}

export const useMediaStore = defineStore('media', () => {
  const urlCache = ref<Record<string, CachedUrl>>({})
  const pendingRequests = ref<Record<string, Promise<string>>>({})

  const getDownloadUrl = async (fileKey: string): Promise<string> => {
    // Проверяем кеш
    const cached = urlCache.value[fileKey]
    if (cached && cached.expiresAt > Date.now()) {
      return cached.url
    }

    // Если уже идёт запрос – возвращаем его
    if (pendingRequests.value[fileKey]) {
      return pendingRequests.value[fileKey]!
    }

    const requestPromise = s3UploadService.getDownloadUrl(fileKey)
    pendingRequests.value[fileKey] = requestPromise

    try {
      const url = await requestPromise
      const expiresAt = Date.now() + 50 * 60 * 1000 // 50 минут
      const timerId = window.setTimeout(
        () => {
          // Удаляем через пересоздание объекта
          const { [fileKey]: _, ...rest } = urlCache.value
          urlCache.value = rest
          delete pendingRequests.value[fileKey]
        },
        50 * 60 * 1000,
      )

      // Реактивное обновление через пересоздание объекта
      urlCache.value = { ...urlCache.value, [fileKey]: { url, expiresAt, timerId } }
      return url
    } finally {
      delete pendingRequests.value[fileKey]
    }
  }

  const clearAllTimers = () => {
    for (const key in urlCache.value) {
      if (urlCache.value[key]?.timerId) {
        clearTimeout(urlCache.value[key].timerId)
      }
    }
    urlCache.value = {}
    pendingRequests.value = {}
  }

  return {
    urlCache,
    getDownloadUrl,
    clearAllTimers,
  }
})
