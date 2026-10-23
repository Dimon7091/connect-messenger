import type { User } from './user.types'
import type { Message } from './message.types'

export interface Chat {
  id: string
  type: 'DIRECT' | 'GROUP'
  name?: string
  avatar?: string
  participants: string[]
  lastMessage: string
  unreadCount: number
  createdAt: string
  updatedAt: string
  createdBy?: string
}
