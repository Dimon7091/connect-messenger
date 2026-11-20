import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, getUserWithDb } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';
import { uuid } from 'zod';

const URL = '/api/v1/users';

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
    it('returns 200 when admin delete user', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );

      const admin = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'admin',
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
        method: 'POST',
        url: `${URL}/${alice.id}/anonymize`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(200);
      // Проверяем что данные пользователя затерты в базе данных
      const body = response.json();
      expect(body).toMatchObject({
        userName: `deleted_user_${alice.id}`,
        passwordHash: 'REDACTED',
        isDeleted: true,
        firstName: 'deleted',
      });
    });
  });

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    it('return 403 when user is not admin', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );

      const bob = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'bob',
        'Alice',
        '12345678',
        'user'
      );

      const jwtToken = testApp.jwt.sign({
        sub: bob.id,
        preferred_username: bob.userName,
        role: bob.role,
      });

      const response = await testApp.inject({
        method: 'POST',
        url: `${URL}/${alice.id}/anonymize`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(403);
      // Проверяем что данные пользователя не затерты
      const body = response.json();
      expect(body.code).toBe('FORBIDDEN');
      const aliceWithDb = await getUserWithDb(testApp.db, alice.id);
      expect(aliceWithDb).toMatchObject({
        userName: alice.userName,
        isDeleted: false,
      });
    });

    it('returns 404 when user id is not exists', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );

      const admin = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'admin',
        'Alice',
        '12345678',
        'admin'
      );

      const randomId = crypto.randomUUID();

      const jwtToken = testApp.jwt.sign({
        sub: admin.id,
        preferred_username: admin.userName,
        role: admin.role,
      });

      const response = await testApp.inject({
        method: 'POST',
        url: `${URL}/${randomId}/anonymize`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(404);

      const body = response.json();
      expect(body.code).toBe('USER_NOT_FOUND');
    });
  });
});
