import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb } from '../../../test/factories.js';
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
    it('returns 200 and active invitation count when user role is admin', async () => {
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

      // Создаем одно приглашение
      await testApp.inject({
        method: 'POST',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      const getResponse = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(getResponse.statusCode).toBe(200);

      const body = getResponse.json();
      expect(body).toMatchObject({
        activeInvitationsCount: 1,
      });
    });
  });

  describe('Negative group', () => {
    it('returns 403 when user role is user', async () => {
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

      // Создаем одно приглашение
      await testApp.inject({
        method: 'POST',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      const response = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(403);

      const body = response.json();
      expect(body.code).toBe('FORBIDDEN');
      expect(body.activeInvitationsCount).toBeUndefined();
    });
  });
});
