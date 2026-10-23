import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import './assets/main.css'

import { registerSW } from 'virtual:pwa-register'

registerSW({
  immediate: true,
  onNeedRefresh() {
    // показать пользователю «Доступно обновление»
  },
  onOfflineReady() {
    // показать «Готово к работе офлайн»
  },
})

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')
