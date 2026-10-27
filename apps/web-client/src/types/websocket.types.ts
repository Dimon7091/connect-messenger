// types/websocket.ts
import type { Attachment } from './attachment.types'
import type { Chat } from './chat.types'

export type WSEventType =
  | 'connection'
  | 'message_new'
  | 'message_sent'
  | 'message_delivered'
  | 'message_read'
  | 'all_messages_read'
  | 'message_deleted'
  | 'chat_history_cleared'
  | 'user_status'
  | 'user_status_subscribe'
  | 'user_status_unsubscribe'
  | 'typing_start'
  | 'typing_stop'
  | 'chat_store'
  | 'error'
  | 'pong'

export interface MessageNewPayload {
  id: string
  chatId: string
  chat?: Chat
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

export interface SentMessagePayloand {
  chatId: string
  messageId: string // временный ID от клиента
  senderId: string
  receiverId: string
  serverMessageId: string // UUID из БД
  text: string
  status: 'SENT' | 'FAILED'
  timestamp: string
  attachments?: Attachment[]
  replyToId?: string | null
}

export interface MessageDeliveredPayload {
  messageId: string
  chatId: string
  userId: string
}

export interface MessageReadPayload {
  messageId: string
  chatId: string
  userId: string
}

export interface AllMessagesReadPayload {
  chatId: string
}

export interface MessageDeletedPayload {
  chatId: string
  messageId: string
}

export interface MessageDeletedResponse {
  chatId: string
  deletedMessagesIds: string[]
  chatUpdatedAt: string
  unreadCount: number
  lastMessage: string | null
}

export interface ChatHistoryClearedResponse {
  chatId: string
  unreadCount: number
  lastMessage: string | null
  updatedAt: string
}

export type UserStatusType = 'ONLINE' | 'OFFLINE' | 'AWAY'

export interface UserStatusPayload {
  userId: string
  status: UserStatusType
  lastSeen?: string // ISO datetime, опционально
}

export interface UserStatusRequest {
  targetUserId: string
}

export interface TypingPayload {
  chatId: string
  userId: string
}

export interface ErrorPayload {
  message: string
  body?: string
}

export interface ConnectionPayload {
  status: 'connected' | 'disconnected' | 'failed'
}
