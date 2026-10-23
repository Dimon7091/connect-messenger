export interface Attachment {
  fileKey: string      // уникальный ключ в S3
  mimeType: string     // например 'image/jpeg'
  fileName: string     // оригинальное имя
  size?: number        // размер в байтах (опционально)
}
