import { httpClient } from './HttpClient'
import type { Chat, Message } from '@/types'
import type { User } from '@messenger/shared-schema'

class MessageService {
  async getChatMessages(
    chatId: string,
    limit: number,
    beforeTimestamp?: string,
  ): Promise<Message[]> {
    const query = new URLSearchParams({
      limit: limit.toString(),
      ...(beforeTimestamp && { beforeTimestamp }), // добавит, только если есть значение
    }).toString()

    const response = await httpClient.get<Message[]>(`/v1/messages/chats/${chatId}?${query}`)
    return response.data!
  }

  async deleteMessages(data: { messagesIds: string[]; chatId: string }): Promise<void> {
    await httpClient.post(`/v1/messages/batch`, data)
  }
}

export const messageService = new MessageService()
