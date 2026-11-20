import { httpClient } from './HttpClient';
import type { InvitationResponse, InvitationCountResponse } from '@messenger/shared-schema';

class InvitationsService {
  /** Получение количества активных приглашений */
  async getActiveInvitationsCount(): Promise<InvitationCountResponse> {
    return httpClient.get<InvitationCountResponse>('/v1/invitations');
  }

  /** Создание нового приглашения */
  async createInvitation(): Promise<InvitationResponse> {
    return httpClient.post<InvitationResponse>('/v1/invitations');
  }

  /** Удаление всех активных приглашений */
  async deleteAllInvitations(): Promise<void> {
    return httpClient.delete<void>('/v1/invitations');
  }
}

export const invitationsService = new InvitationsService();
