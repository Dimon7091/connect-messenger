import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, getUserWithDb } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';

const URL = '/api/v1/users/statistic';

describe('SoftDeleteUseCase', () => {
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

  // MARK: - Позитивные сценарии
  describe('Positive group', () => {
    it('returns 200 when user role is admin', async () => {
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

      const response = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(200);
    });
  });

  // MARK: - Негативные сценарии
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

      const response = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(403);
    });
  });
});
