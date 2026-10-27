// src/sw.ts
import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching'
import { initializeApp } from 'firebase/app'
import { getMessaging, onBackgroundMessage } from 'firebase/messaging/sw'

// 1. Стандартный кэш стилей, картинок и иконок PWA от плагина Vite
cleanupOutdatedCaches()
// @ts-expect-error — Сборщик Vite автоматически заменит эту переменную-заглушку на манифест
precacheAndRoute(self.__WB_MANIFEST)

// 2. Ваша личная конфигурация проекта Firebase
const firebaseConfig = {
  apiKey: 'AIzaSyA4trvO5gojS8Tv7VmsOLBDXNWXxsieRkk',
  authDomain: '://firebaseapp.com',
  projectId: 'connect-messanger-f8952',
  storageBucket: 'connect-messanger-f8952.firebasestorage.app',
  messagingSenderId: '481251044166',
  appId: '1:481251044166:web:89d4c2ed0c23daf80e9347',
  measurementId: 'G-GV5T9E9EXC',
}

// 3. Запуск инстанса Firebase в фоновом контексте воркера
const app = initializeApp(firebaseConfig)
const messaging = getMessaging(app)

// 4. Обработчик пушей в фоновом режиме (при заблокированном телефоне или закрытой вкладке)
onBackgroundMessage(messaging, (payload) => {
  console.log('✉️ Получен фоновый пуш через NPM:', payload)

  const notificationTitle = payload.notification?.title || 'Новое сообщение'
  const notificationOptions = {
    body: payload.notification?.body || '',
    icon: '/icon-connect-192.png',
    data: payload.data, // Передаем id чата, чтобы открыть его при клике
  }

  // @ts-expect-error — Указываем линтеру, что метод showNotification нативный для глобального контекста SW
  self.registration.showNotification(notificationTitle, notificationOptions)
})

// 5. Обработка клика по системному уведомлению (открытие нужной переписки)
self.addEventListener('notificationclick', (event: any) => {
  event.notification.close()
  const chatId = event.notification.data?.chat_id

  event.waitUntil(
    // @ts-expect-error — Обращаемся к клиентам открытых окон браузера через нативный self
    self.clients
      .matchAll({ type: 'window', includeUncontrolled: true })
      .then((clientList: any[]) => {
        // Если сайт приложения уже открыт — переключаемся на эту вкладку
        for (const client of clientList) {
          if (client.url.includes('localhost') && 'focus' in client) {
            if (chatId) client.navigate(`/chat/${chatId}`)
            return client.focus()
          }
        }
        // Если сайт был полностью закрыто — открываем вкладку с нуля
        // @ts-expect-error — Встроенный метод для открытия нового окна
        if (self.clients.openWindow) {
          // @ts-expect-error — Открываем окно по нужному пути
          return self.clients.openWindow(chatId ? `/chat/${chatId}` : '/')
        }
      }),
  )
})
