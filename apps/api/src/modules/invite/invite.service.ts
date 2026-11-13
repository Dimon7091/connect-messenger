import type { Invitation } from '@messenger/database';
import type { InviteRepository } from './invite.repository.js';
import { randomBytes } from 'node:crypto';
import { InvitationError } from '../../lib/errors.js';

export interface InviteService {
  create(): Promise<Invitation>;
  validateAndConsumeInvitation(token: string): Promise<void>;
  getActiveCount(): Promise<number>;
}

export function createInviteService(repo: InviteRepository): InviteService {
  return {
    async create() {
      const newInvitation = {
        token: randomBytes(24).toString('base64url'),
      };

      return await repo.create(newInvitation);
    },

    async validateAndConsumeInvitation(token) {
      const invitation = await repo.findByToken(token);

      if (!invitation) {
        throw new InvitationError('Invitation is invalid');
      }

      repo.deleteByToken(token);
    },

    async getActiveCount() {
      return await repo.activeCount();
    },
  };
}
