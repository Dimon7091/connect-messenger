// services/http/HttpClient.ts
import type { ApiResponse } from '@/types/index'
import { useRouter } from 'vue-router'

/**
 * Опции для HTTP запроса
 * Расширяет стандартные RequestInit, добавляя строгую типизацию для заголовков
 */
interface RequestOptions extends RequestInit {
  headers?: Record<string, string> // Более строгий тип для заголовков
}

/**
 * Интерсептор - перехватчик запросов/ответов для централизованной обработки
 * Позволяет модифицировать запрос до отправки или ответ до получения
 */
interface Interceptor {
  request?: (config: RequestOptions) => RequestOptions // Модифицирует конфиг запроса
  response?: <T>(response: T) => T // Модифицирует ответ (например, для пагинации)
  error?: (error: Error) => void // Обрабатывает глобальные ошибки
}

export class ApiError extends Error {
  status: number
  data: any

  constructor(message: string, status: number, data: any) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data // 🌟 Сюда запишется {"lastName": "..."}
  }
}

/**
 * HTTP клиент для работы с REST API
 * Особенности:
 * - Автоматическое управление JWT токеном (localStorage)
 * - Поддержка интерсепторов (аналог axios interceptors)
 * - Автоматическая обработка FormData для загрузки файлов
 * - Единая обработка ошибок
 * - Типизированные ответы через ApiResponse<T>
 */
class HttpClient {
  private baseURL: string // Базовый URL API (из переменных окружения)
  private token: string | null = null // JWT токен для авторизации
  private interceptors: Interceptor[] = [] // Массив зарегистрированных интерсепторов

  constructor(baseURL: string) {
    this.baseURL = baseURL
    // Автоматически подтягиваем токен из localStorage при инициализации клиента
    this.token = localStorage.getItem('token')
  }

  /**
   * Устанавливает/обновляет JWT токен
   * Автоматически сохраняет/удаляет токен в localStorage
   * @param token - токен или null для удаления
   */
  setToken(token: string | null): void {
    this.token = token
    if (token) {
      localStorage.setItem('token', token) // Сохраняем для последующих сессий
    } else {
      localStorage.removeItem('token') // Очищаем при выходе
    }
  }

  /**
   * Добавляет новый интерсептор в конец цепочки
   * Интерсепторы выполняются в порядке добавления
   * @param interceptor - объект с методами request/response/error
   */
  addInterceptor(interceptor: Interceptor): void {
    this.interceptors.push(interceptor)
  }

  /**
   * Основной метод выполнения HTTP запроса
   * Обрабатывает:
   * - Формирование URL и заголовков
   * - Добавление токена авторизации
   * - Применение интерсепторов
   * - Парсинг JSON ответов
   * - Обработку ошибок
   *
   * @template T - тип данных в ответе
   * @param endpoint - эндпоинт (например '/users')
   * @param options - опции запроса (метод, тело, заголовки и т.д.)
   * @returns Promise с типизированным ответом ApiResponse<T>
   */
  private async request<T>(
    endpoint: string,
    options: RequestOptions = {},
  ): Promise<ApiResponse<T>> {
    const url = `${this.baseURL}${endpoint}` // Полный URL запроса

    // === 1. Формируем базовые заголовки ===
    const headers: Record<string, string> = { ...options.headers }

    // Важно: не добавляем Content-Type для FormData
    // Браузер сам установит правильный Content-Type с boundary для multipart/form-data
    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json'
    }

    // Добавляем JWT токен в заголовок Authorization, если он есть
    if (this.token) {
      headers['Authorization'] = `Bearer ${this.token}`
    }

    // === 2. Формируем финальную конфигурацию запроса ===
    let config: RequestOptions = { ...options, headers }

    // Применяем все интерсепторы запроса (по очереди, модифицируя конфиг)
    this.interceptors.forEach((i) => {
      if (i.request) config = i.request(config)
    })

    // ХАК-ЗАЩИТА: Если после всех интерсепторов в body остался FormData,
    // принудительно удаляем Content-Type, чтобы браузер сделал всё сам
    if (config.body instanceof FormData && config.headers) {
      delete (config.headers as Record<string, string>)['Content-Type']
      // Если у вас используется класс Headers: config.headers.delete('Content-Type');
    }

    try {
      // === 3. Выполняем запрос ===
      const response = await fetch(url, config)

      // === 4. Обрабатываем ответ ===
      // Проверяем, вернул ли сервер JSON (или пустой ответ типа 204 No Content)
      const isJson = response.headers.get('content-type')?.includes('application/json')
      const rawData = isJson ? await response.json() : {}

      // Если статус ответа не 2xx - выбрасываем ошибку
      if (!response.ok) {
        // Пытаемся извлечь сообщение об ошибке из ответа сервера
        // Поддерживаем разные форматы ошибок: { error: '...' } или { message: '...' }
        if (
          rawData.errorCode === 'AUTH_ACCOUNT_BANNED' ||
          rawData.action === 'LOGOUT_AND_REDIRECT'
        ) {
          localStorage.removeItem('token')
          // Если мы уже на странице логина, не перенаправляем снова
          if (window.location.pathname !== '/login') {
            const encodedMessage = encodeURIComponent(rawData.message)
            window.location.href = `/login?error=${encodedMessage}`
          }
        }

        // Формируем понятный текст для логов, но внутрь передаем статус и сам rawData
        const errorMessage = rawData.message || rawData.error || `Error ${response.status}`

        throw new ApiError(errorMessage, response.status, rawData)
      }

      // === 5. Применяем интерсепторы ответа ===
      // Применяем интерсепторы к сырым данным
      let processedData: T = rawData
      this.interceptors.forEach((i) => {
        if (i.response) processedData = i.response(processedData)
      })

      // === 6. Возвращаем стандартизированный ответ ===
      // Оборачиваем данные в структуру ApiResponse
      const apiResponse: ApiResponse<T> = {
        data: processedData,
        success: true,
        status: response.status,
      }

      return apiResponse
    } catch (error) {
      // === 7. Обрабатываем ошибку через интерсепторы ===
      // Это полезно для глобального логирования ошибок или показа уведомлений
      this.interceptors.forEach((i) => {
        if (i.error) i.error(error as Error)
      })

      // Возвращаем ошибку в стандартном формате ApiResponse
      throw error // Пробрасываем ошибку дальше, чтобы обработать на уровне компонента
    }
  }

  // === Публичные методы для различных HTTP методов ===

  /**
   * GET запрос - получение данных
   * @param endpoint - эндпоинт (например '/users/123')
   * @example const { data: user } = await httpClient.get<User>('/users/123')
   */
  async get<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'GET' })
  }

  /**
   * POST запрос - создание нового ресурса
   * @param endpoint - эндпоинт (например '/users')
   * @param data - отправляемые данные (автоматически преобразуются в JSON)
   * @example const { data: newUser } = await httpClient.post<User>('/users', { name: 'John' })
   */
  async post<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'POST',
      body: JSON.stringify(data), // Автоматическая сериализация в JSON
    })
  }

  /**
   * PUT запрос - полное обновление ресурса
   * @param endpoint - эндпоинт (например '/users/123')
   * @param data - новые данные для ресурса
   * @example await httpClient.put<User>('/users/123', { name: 'Jane' })
   */
  async put<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PUT',
      body: JSON.stringify(data),
    })
  }

  /**
   * PATCH запрос - частичное обновление ресурса
   * @param endpoint - эндпоинт (например '/users/123')
   * @param data - частичные данные для обновления
   * @example await httpClient.patch<User>('/users/123', { name: 'Jane' })
   */
  async patch<T>(endpoint: string, data?: unknown): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  /**
   * DELETE запрос - удаление ресурса
   * @param endpoint - эндпоинт (например '/users/123')
   * @example await httpClient.delete('/users/123')
   */
  async delete<T>(endpoint: string): Promise<ApiResponse<T>> {
    return this.request<T>(endpoint, { method: 'DELETE' })
  }

  /**
   * Загрузка файла на сервер
   * Автоматически использует FormData, что позволяет отправлять файлы
   *
   * @param endpoint - эндпоинт для загрузки (например '/upload')
   * @param file - файл для загрузки
   * @param additionalData - дополнительные поля формы (текстовые или другие файлы)
   * @returns Promise с ответом сервера
   *
   * @example
   * const file = event.target.files[0]
   * const { data: result } = await httpClient.uploadFile<{ url: string }>('/upload', file, {
   *   userId: '123',
   *   category: 'avatar'
   * })
   */
  async uploadFile<T>(
    endpoint: string,
    file: File,
    additionalData?: Record<string, string | Blob>,
  ): Promise<ApiResponse<T>> {
    const formData = new FormData()
    formData.append('file', file) // Добавляем файл с ключом 'file'

    // Добавляем дополнительные поля, если они есть
    if (additionalData) {
      Object.entries(additionalData).forEach(([key, value]) => {
        formData.append(key, value)
      })
    }

    // Отправляем как обычный POST, но с FormData
    // request() метод сам поймет, что body - FormData, и не добавит Content-Type
    return this.request<T>(endpoint, {
      method: 'POST',
      body: formData,
      // Важно: headers не указываем! request() автоматически пропустит Content-Type
    })
  }
}

// === Создаем и экспортируем единственный экземпляр клиента (Singleton) ===
// URL берется из переменных окружения, с fallback-значением '/api' для разработки
export const httpClient = new HttpClient(import.meta.env.VITE_API_URL || '/api')
