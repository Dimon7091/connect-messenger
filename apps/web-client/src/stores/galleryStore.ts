import { defineStore } from 'pinia'
import { ref } from 'vue'

export interface GalleryImage {
  url: string
  fileKey: string
  fileName?: string
}

export const useGalleryStore = defineStore('gallery', () => {
  const images = ref<GalleryImage[]>([])
  const currentIndex = ref(0)
  const isOpen = ref(false)

  function openGallery(imagesList: GalleryImage[], index: number) {
    images.value = imagesList
    currentIndex.value = index
    isOpen.value = true
  }

  function closeGallery() {
    isOpen.value = false
    images.value = []
    currentIndex.value = 0
  }

  function next() {
    if (currentIndex.value < images.value.length - 1) {
      currentIndex.value++
    }
  }

  function prev() {
    if (currentIndex.value > 0) {
      currentIndex.value--
    }
  }

  return {
    images,
    currentIndex,
    isOpen,
    openGallery,
    closeGallery,
    next,
    prev,
  }
})
