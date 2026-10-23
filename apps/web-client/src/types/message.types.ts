import type { Attachment } from './attachment.types'

export interface Message {
  id: string
  chatId: string
  senderId: string
  receiverId: string
  text: string
  timestamp: string // По умолчанию NOW()
  createdAt: string
  status: 'SENT' | 'DELIVERED' | 'READ' | 'FAILED'
  deletedAt?: Date | null
  attachments?: Attachment[]
  replyToId?: string | null
  readBy?: string | null

  replyContext?: {
    senderName: string
    textSnippet: string
    originalMessageId: string
  }
}
