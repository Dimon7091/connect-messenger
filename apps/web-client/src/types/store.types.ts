import type { Chat } from './chat.types'
import type { Message } from './message.types'
import type { User } from '@messenger/shared-schema'

export interface UserStoreState {
  profile: User | null
  contacts: User[]
  token: string | null
}

export interface ChatStoreState {
  chats: Chat[]
  currentChatId: string | null
  messagesCache: Map<string, Message[]>
  typingUsers: Map<string, Set<string>>
}

export interface UIStoreState {
  isLoading: boolean
  error: string | null
  sidebarOpen: boolean
  modal: ModalInfo | null
  notifications: Notification[]
}

export interface ModalInfo {
  component: string
  props: Record<string, unknown>
}

export interface Notification {
  id: number
  title: string
  body?: string
  type: 'info' | 'success' | 'error' | 'warning'
  duration?: number
}
