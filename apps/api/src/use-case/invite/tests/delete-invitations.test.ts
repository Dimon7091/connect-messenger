import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import {
  countInvitationsInDb,
  createInvitation,
  createInvitationInDb,
  createUserInDb,
} from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';

const URL = '/api/v1/invitations';

describe('CreateInvitationUseCase', () => {
  let testApp: FastifyInstance;

  beforeAll(async () => {
    testApp = await createTestApp({ logger: false });
    await testApp.ready();
  });

  afterAll(async () => {
    await closeTestApp(testApp);
  });

  beforeEach(async () => {
    // Быстрая очистка таблиц перед каждым тестом (занимает ~3 мс)
    await resetDb(testApp.db);
  });

  describe('Positive group', () => {
    it('returns 204 and delete all when user is admin', async () => {
      const admin = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'admin'
      );

      const jwtToken = testApp.jwt.sign({
        sub: admin.id,
        preferred_username: admin.userName,
        role: admin.role,
      });

      // Создаем несколько приглашений
      await createInvitationInDb(testApp.db);
      await createInvitationInDb(testApp.db);

      const deleteResponse = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(deleteResponse.statusCode).toBe(204);

      const invitationsInDb = await countInvitationsInDb(testApp.db);
      expect(invitationsInDb).toBe(0);
    });
  });

  describe('Negative group', () => {
    it('returns 403 when user is not admin', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );

      const jwtToken = testApp.jwt.sign({
        sub: user.id,
        preferred_username: user.userName,
        role: user.role,
      });

      // Создаем несколько приглашений
      await createInvitationInDb(testApp.db);
      await createInvitationInDb(testApp.db);

      const deleteResponse = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(deleteResponse.statusCode).toBe(403);

      const invitationsInDb = await countInvitationsInDb(testApp.db);
      expect(invitationsInDb).toBe(2);
    });
  });
});
