import { httpClient } from './HttpClient'
import type { Chat } from '@/types'
import type { User } from '@messenger/shared-schema'

class ChatService {
  async getChats(): Promise<Chat[]> {
    // Интерсептор сам выбросит ошибку при failure
    const response = await httpClient.get<Chat[]>(`/v1/chats`)
    return response.data!
  }

  async getDirectChat(chatId: string): Promise<Chat> {
    // Интерсептор сам выбросит ошибку при failure
    const response = await httpClient.get<Chat>(`/v1/chats`)
    return response.data!
  }

  async createOrGetDirectChat(companionId: string): Promise<Chat> {
    const response = await httpClient.post<Chat>('/v1/chats', { companionId })
    return response.data!
  }

  async updateChat(chatId: string, data: Partial<Chat>): Promise<Chat> {
    const response = await httpClient.put<Chat>(`/v1/chats${chatId}`, data)
    return response.data!
  }

  async deleteChat(chatId: string): Promise<void> {
    await httpClient.delete(`/v1/chats/${chatId}`)
  }

  async clearChatHistory(chatId: string): Promise<void> {
    await httpClient.delete(`/v1/chats/${chatId}/history`)
  }

  async getChatByParticipants(
    participantId1: string,
    participantId2: string,
  ): Promise<Chat | undefined> {
    const response = await httpClient.get<Chat>(
      `/v1/chats/participants?id1=${participantId1}&id2=${participantId2}`,
    )
    return response.data
  }

  async addParticipant(chatId: string, userId: string): Promise<Chat> {
    const response = await httpClient.post<Chat>(`/v1/chats${chatId}/participants`, { userId })
    return response.data!
  }

  async removeParticipant(chatId: string, userId: string): Promise<void> {
    await httpClient.delete(`/v1/chats${chatId}/participants/${userId}`)
  }
}

export const chatService = new ChatService()
