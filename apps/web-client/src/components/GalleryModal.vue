<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="opacity-0 scale-95"
      enter-to-class="opacity-100 scale-100"
      leave-active-class="transition duration-150 ease-in"
      leave-from-class="opacity-100 scale-100"
      leave-to-class="opacity-0 scale-95"
    >
      <div
        v-if="galleryStore.isOpen"
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 select-none"
        @click.self="close"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
        @mousedown="handleMouseDown"
        @mousemove="handleMouseMove"
        @mouseup="handleMouseUp"
        @mouseleave="handleMouseUp"
      >
        <!-- Верхняя панель с кнопками -->
        <div class="fixed top-4 left-4 z-[101] flex items-center gap-4">
          <!-- Кнопка закрытия -->
          <button
            @click="close"
            class="text-white hover:text-gray-300 transition p-2"
            title="Закрыть"
          >
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
          <!-- Кнопка скачивания -->
          <button
            @click="downloadCurrentImage"
            class="text-white hover:text-gray-300 transition p-2"
            title="Скачать"
          >
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2"
                d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4 4m0 0l-4-4m4 4V4"
              />
            </svg>
          </button>
        </div>

        <div class="relative w-full h-full flex items-center justify-center overflow-hidden">
          <!-- Соседнее изображение (под текущим) – просто fade, без смещения -->
          <div
            v-if="adjacentImage"
            class="absolute inset-0 flex items-center justify-center"
            :style="{
              opacity: Math.min(1, Math.abs(dragOffset) / 200),
              transition: isDragging ? 'none' : 'opacity 0.4s ease',
            }"
          >
            <img
              :src="adjacentImage.url"
              :alt="adjacentImage.fileName || 'Соседнее'"
              class="max-w-[90vw] max-h-[85vh] object-contain rounded-lg shadow-2xl"
            />
          </div>

          <!-- Текущее изображение – уезжает синхронно с пальцем -->
          <div
            class="absolute inset-0 flex items-center justify-center"
            :style="{
              transform: `translateX(${dragOffset}px) scale(${1 - Math.abs(dragOffset) * 0.002})`,
              transition: isDragging
                ? 'none'
                : 'transform 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94)',
              willChange: 'transform',
            }"
          >
            <img
              :key="currentIndex"
              :src="galleryStore.images[currentIndex]?.url"
              :alt="galleryStore.images[currentIndex]?.fileName || 'Изображение'"
              class="max-w-full max-h-full object-contain shadow-2xl"
              draggable="false"
            />
          </div>
        </div>

        <!-- Кнопки навигации (десктоп) -->
        <button
          v-if="galleryStore.images.length > 1 && !isTouchDevice"
          @click="prev"
          class="absolute left-4 top-1/2 -translate-y-1/2 z-[101] text-white hover:text-gray-300 transition p-2"
        >
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M15 19l-7-7 7-7"
            />
          </svg>
        </button>
        <button
          v-if="galleryStore.images.length > 1 && !isTouchDevice"
          @click="next"
          class="absolute right-4 top-1/2 -translate-y-1/2 z-[101] text-white hover:text-gray-300 transition p-2"
        >
          <svg class="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path
              stroke-linecap="round"
              stroke-linejoin="round"
              stroke-width="2"
              d="M9 5l7 7-7 7"
            />
          </svg>
        </button>

        <div
          v-if="galleryStore.images.length > 1"
          class="absolute bottom-10 left-1/2 -translate-x-1/2 z-[101] bg-gray-500/20 text-white px-4 py-1.5 rounded-full text-sm"
        >
          {{ currentIndex + 1 }} / {{ galleryStore.images.length }}
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { useGalleryStore } from '@/stores/galleryStore'

const galleryStore = useGalleryStore()

const isTouchDevice = ref(false)
const dragOffset = ref(0)
const isDragging = ref(false)
const startX = ref(0)
const currentX = ref(0)

const currentIndex = computed(() => galleryStore.currentIndex)

// Соседнее изображение (следующее или предыдущее) – показывается под текущим
const adjacentImage = computed(() => {
  const delta = currentX.value - startX.value
  if (Math.abs(delta) < 10) return null
  if (delta < 0 && currentIndex.value < galleryStore.images.length - 1) {
    return galleryStore.images[currentIndex.value + 1]
  } else if (delta > 0 && currentIndex.value > 0) {
    return galleryStore.images[currentIndex.value - 1]
  }
  return null
})

const checkTouch = () => {
  isTouchDevice.value = window.matchMedia('(pointer: coarse)').matches
}

onMounted(() => {
  checkTouch()
  window.addEventListener('resize', checkTouch)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkTouch)
})

const close = () => galleryStore.closeGallery()
const prev = () => galleryStore.prev()
const next = () => galleryStore.next()

// ===== Скачивание текущего изображения =====
const downloadCurrentImage = async () => {
  const currentImage = galleryStore.images[currentIndex.value]
  if (!currentImage) return

  const url = currentImage.url
  const fileName = currentImage.fileName || 'image.jpg'

  try {
    // Если URL имеет префикс blob:, можно использовать его напрямую
    if (url.startsWith('blob:')) {
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      return
    }

    // Иначе скачиваем через fetch
    const response = await fetch(url)
    if (!response.ok) throw new Error('Network response was not ok')
    const blob = await response.blob()

    const link = document.createElement('a')
    const objectUrl = URL.createObjectURL(blob)
    link.href = objectUrl
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(objectUrl)
  } catch (error) {
    console.error('Ошибка скачивания:', error)
    // Альтернативный вариант: открыть в новой вкладке
    window.open(url, '_blank')
  }
}

// ===== Drag логика =====
const startDrag = (clientX: number) => {
  if (galleryStore.images.length <= 1) return
  isDragging.value = true
  startX.value = clientX
  currentX.value = clientX
  dragOffset.value = 0
}

const moveDrag = (clientX: number) => {
  if (!isDragging.value) return
  const delta = clientX - startX.value
  const maxOffset = window.innerWidth * 0.4
  let offset = Math.max(-maxOffset, Math.min(maxOffset, delta))
  if (currentIndex.value === 0 && delta > 0) {
    offset = delta * 0.2
  } else if (currentIndex.value === galleryStore.images.length - 1 && delta < 0) {
    offset = delta * 0.2
  }
  dragOffset.value = offset
  currentX.value = clientX
}

const endDrag = async () => {
  if (!isDragging.value) return

  const delta = currentX.value - startX.value
  const threshold = 80

  if (Math.abs(delta) > threshold) {
    isDragging.value = true

    if (delta < 0 && currentIndex.value < galleryStore.images.length - 1) {
      next()
    } else if (delta > 0 && currentIndex.value > 0) {
      prev()
    }

    dragOffset.value = 0

    await nextTick()
    requestAnimationFrame(() => {
      isDragging.value = false
    })
  } else {
    dragOffset.value = 0
    isDragging.value = false
  }
}

watch(currentIndex, () => {
  dragOffset.value = 0
})

// Touch
const handleTouchStart = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (touch) startDrag(touch.clientX)
}
const handleTouchMove = (e: TouchEvent) => {
  const touch = e.touches[0]
  if (touch) moveDrag(touch.clientX)
}
const handleTouchEnd = () => endDrag()

// Mouse
let isMouseDown = false
const handleMouseDown = (e: MouseEvent) => {
  if (isTouchDevice.value) return
  const target = e.target as HTMLElement
  if (target.closest('button')) return
  isMouseDown = true
  startDrag(e.clientX)
}
const handleMouseMove = (e: MouseEvent) => {
  if (!isMouseDown) return
  moveDrag(e.clientX)
}
const handleMouseUp = () => {
  if (isMouseDown) {
    isMouseDown = false
    endDrag()
  }
}
</script>
