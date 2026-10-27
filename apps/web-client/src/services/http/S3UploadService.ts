// services/S3UploadService.ts
import { httpClient } from './HttpClient'
import type { FileInitRequest, FileInitResponse } from '@/types/index'

export type UploadProgressCallback = (percent: number) => void

/**
 * Словарь популярных MIME-типов, сгруппированных по категориям.
 * Включает все современные форматы изображений, видео, аудио и документов.
 */
const POPULAR_MIME_TYPES: Record<string, string> = {
  // --- ИЗОБРАЖЕНИЯ (Photos / Graphics) ---
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  webp: 'image/webp',
  heic: 'image/heic',
  heif: 'image/heif',
  svg: 'image/svg+xml',
  bmp: 'image/bmp',
  tiff: 'image/tiff',
  tif: 'image/tiff',
  ico: 'image/x-icon',
  avif: 'image/avif',

  // --- ВИДЕО (Videos) ---
  mp4: 'video/mp4',
  webm: 'video/webm',
  avi: 'video/x-msvideo',
  mov: 'video/quicktime',
  mkv: 'video/x-matroska',
  wmv: 'video/x-ms-wmv',
  flv: 'video/x-flv',
  m4v: 'video/x-m4v',
  '3gp': 'video/3gpp',
  ts: 'video/mp2t',

  // --- АУДИО (Audio) ---
  mp3: 'audio/mpeg',
  wav: 'audio/wav',
  ogg: 'audio/ogg',
  m4a: 'audio/x-m4a',
  flac: 'audio/flac',
  aac: 'audio/aac',
  wma: 'audio/x-ms-wma',
  opus: 'audio/opus',

  // --- ДОКУМЕНТЫ (Documents) ---
  pdf: 'application/pdf',
  txt: 'text/plain',
  rtf: 'application/rtf',
  html: 'text/html',
  htm: 'text/html',
  csv: 'text/csv',
  xml: 'application/xml',

  // Microsoft Office
  doc: 'application/msword',
  docx: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  xls: 'application/vnd.ms-excel',
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ppt: 'application/vnd.ms-powerpoint',
  pptx: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',

  // OpenOffice / LibreOffice
  odt: 'application/vnd.oasis.opendocument.text',
  ods: 'application/vnd.oasis.opendocument.spreadsheet',
  odp: 'application/vnd.oasis.opendocument.presentation',

  // Электронные книги
  epub: 'application/epub+zip',
  fb2: 'application/x-fictionbook+xml',

  // --- АРХИВЫ И КОД (Archives & Code) ---
  zip: 'application/zip',
  rar: 'application/vnd.rar',
  '7z': 'application/x-7z-compressed',
  tar: 'application/x-tar',
  gz: 'application/gzip',
  json: 'application/json',
}

/**
 * Надежное определение MIME-типа файла по его имени/расширению
 */
function getMimeType(file: File): string {
  // 1. Если браузер сам успешно определил тип, используем его
  if (file.type && file.type.trim() !== '') {
    return file.type
  }

  // 2. Если тип пустой, достаем расширение из имени файла
  const filename = file.name || ''
  const dotIndex = filename.lastIndexOf('.')

  if (dotIndex > 0 && dotIndex < filename.length - 1) {
    const extension = filename.slice(dotIndex + 1).toLowerCase()

    const matchedMime = POPULAR_MIME_TYPES[extension]
    // TypeScript видит проверку и гарантирует, что здесь строго string, а не string | undefined
    if (matchedMime) {
      return matchedMime
    }
  }

  // 3. Дефолтный бинарный тип, если ничего не подошло
  return 'application/octet-stream'
}

/**
 * Сервис для загрузки файлов в S3 через presigned URLs
 */
class S3UploadService {
  /**
   * Инициирует загрузку файла: получает uploadUrl и метаданные
   */
  private async initUpload(file: File, mimeType: string): Promise<FileInitResponse> {
    const requestBody: FileInitRequest = {
      fileName: file.name,
      mimeType: mimeType,
      fileSize: file.size.toString(),
    }

    const response = await httpClient.post<FileInitResponse>('/v1/media/upload-url', requestBody)
    return response.data!
  }

  /**
   * Загружает файл напрямую в S3 по uploadUrl
   */
  private uploadToS3(
    uploadUrl: string,
    file: File,
    mimeType: string,
    onProgress?: UploadProgressCallback,
  ): Promise<void> {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest()
      xhr.open('PUT', uploadUrl)

      // Передаем строго вычисленный MIME-тип
      xhr.setRequestHeader('Content-Type', mimeType)

      xhr.upload.onprogress = (event) => {
        if (onProgress && event.lengthComputable) {
          const percent = Math.round((event.loaded * 100) / event.total)
          onProgress(percent)
        }
      }

      xhr.onload = () => {
        if (xhr.status === 200 || xhr.status === 201) {
          resolve()
        } else {
          console.error('Upload failed:', xhr.status, xhr.responseText)
          reject(new Error(`Upload failed with status ${xhr.status}: ${xhr.responseText}`))
        }
      }

      xhr.onerror = () => reject(new Error('Network error during upload'))
      xhr.send(file)
    })
  }

  /**
   * Публичный метод: загружает файл и возвращает его метаданные
   */
  async uploadFile(
    file: File,
    onProgress?: UploadProgressCallback,
  ): Promise<{ fileKey: string; mimeType: string; fileName: string; size: string }> {
    // 1. Вычисляем MIME-тип один раз на клиенте
    const clientMimeType = getMimeType(file)

    // 2. Получаем подписанную ссылку
    const {
      uploadUrl,
      fileKey,
      mimeType: responseMimeType,
      fileName,
      size,
    } = await this.initUpload(file, clientMimeType)

    // 3. Загружаем в S3. Если бэкенд вернул пустой mimeType в FileInitResponse,
    // подстраховываемся и берем наш clientMimeType, чтобы избежать 'string | undefined'
    const finalMimeType = responseMimeType ?? clientMimeType

    await this.uploadToS3(uploadUrl, file, finalMimeType, onProgress)

    return {
      fileKey,
      mimeType: finalMimeType,
      fileName: fileName ?? file.name,
      size: size ?? file.size.toString(),
    }
  }

  /**
   * Получение подписанной ссылки для скачивания файла
   */
  async getDownloadUrl(fileKey: string): Promise<string> {
    const response = await httpClient.post<{ downloadUrl: string }>('/v1/media/download-url', {
      fileKey,
    })
    return response.data!.downloadUrl
  }
}

export const s3UploadService = new S3UploadService()
