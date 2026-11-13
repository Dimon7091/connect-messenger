import { httpClient } from './HttpClient';
import type { InvitationResponse, InvitationCountResponse } from '@messenger/shared-schema';

class InvitationsService {
  /** Получение количества активных приглашений */
  async getActiveInvitationsCount(): Promise<InvitationCountResponse> {
    return httpClient.get<InvitationCountResponse>('/v1/admin/invitations/count');
  }

  /** Создание нового приглашения */
  async createInvitation(): Promise<InvitationResponse> {
    return httpClient.post<InvitationResponse>('/v1/admin/invitations');
  }
}

export const invitationsService = new InvitationsService();