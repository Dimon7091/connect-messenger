// services/ImageCompressor.ts

/**
 * Сжимает изображение до заданного размера, сохраняя максимальное качество.
 * @param file - исходный файл
 * @param maxSizeMB - максимальный размер в МБ (по умолчанию 2)
 * @param maxWidth - максимальная ширина (по умолчанию 1600)
 * @param maxHeight - максимальная высота (по умолчанию 1600)
 */
export const compressImageIfNeeded = async (
  file: File,
  maxSizeMB: number = 2,
  maxWidth: number = 1600,
  maxHeight: number = 1600,
): Promise<File> => {
  // Если файл и так меньше лимита, возвращаем его без изменений
  if (file.size <= maxSizeMB * 1024 * 1024) {
    return file
  }

  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.readAsDataURL(file)
    reader.onload = (e) => {
      const img = new Image()
      img.src = e.target?.result as string
      img.onload = () => {
        // Масштабируем с сохранением пропорций
        let width = img.width
        let height = img.height
        if (width > height) {
          if (width > maxWidth) {
            height = Math.round((height * maxWidth) / width)
            width = maxWidth
          }
        } else {
          if (height > maxHeight) {
            width = Math.round((width * maxHeight) / height)
            height = maxHeight
          }
        }

        const canvas = document.createElement('canvas')
        canvas.width = width
        canvas.height = height
        const ctx = canvas.getContext('2d')
        ctx?.drawImage(img, 0, 0, width, height)

        let quality = 0.92
        let blob: Blob | null = null
        const targetSize = maxSizeMB * 1024 * 1024

        // Пробуем качество от 0.92 вниз до 0.5
        for (let q = 0.92; q >= 0.5; q -= 0.03) {
          const dataUrl = canvas.toDataURL('image/jpeg', q)
          blob = dataURLtoBlob(dataUrl)
          if (blob.size <= targetSize) {
            quality = q
            break
          }
        }

        // Если даже с минимальным качеством 0.5 файл всё ещё больше
        if (blob && blob.size > targetSize) {
          // Уменьшаем разрешение дополнительно
          const scale = Math.min(1, Math.sqrt(targetSize / blob.size))
          const newWidth = Math.round(width * scale)
          const newHeight = Math.round(height * scale)
          const smallCanvas = document.createElement('canvas')
          smallCanvas.width = newWidth
          smallCanvas.height = newHeight
          const smallCtx = smallCanvas.getContext('2d')
          smallCtx?.drawImage(canvas, 0, 0, newWidth, newHeight)
          blob = dataURLtoBlob(smallCanvas.toDataURL('image/jpeg', 0.85))
        }

        // Если всё ещё больше, сжимаем сильнее
        if (blob && blob.size > targetSize) {
          const dataUrl = canvas.toDataURL('image/jpeg', 0.7)
          blob = dataURLtoBlob(dataUrl)
        }

        // Создаём итоговый файл
        const compressedFile = new File([blob!], file.name.replace(/\.[^.]+$/, '.jpg'), {
          type: 'image/jpeg',
        })
        resolve(compressedFile)
      }
      img.onerror = reject
    }
    reader.onerror = reject
  })
}

// Вспомогательная функция преобразования Data URL в Blob
const dataURLtoBlob = (dataUrl: string): Blob => {
  const arr = dataUrl.split(',')
  const mime = arr[0]?.match(/:(.*?);/)![1]
  const bstr = atob(arr[1]!)
  let n = bstr.length
  const u8arr = new Uint8Array(n)
  while (n--) {
    u8arr[n] = bstr.charCodeAt(n)
  }
  return new Blob([u8arr], { type: mime })
}
