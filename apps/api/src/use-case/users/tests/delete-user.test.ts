import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, getUserWithDb } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';

const URL = '/api/v1/users/me';

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
    it('returns 204 when user delete himself', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const jwtToken = testApp.jwt.sign({
        sub: user.id,
        preferred_username: user.userName,
        role: user.role,
      });

      const response = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(204);
      // Проверяем что данные пользователя затерты в базе данных
      const dbUser = await getUserWithDb(testApp.db, user.id);
      expect(dbUser).toMatchObject({
        userName: `deleted_user_${user.id}`,
        passwordHash: 'REDACTED',
        isDeleted: true,
        profile: {
          firstName: 'deleted',
          lastName: 'deleted',
        },
      });
    });
  });

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    it('return 404 when request with deleted user jwt', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const jwtToken = testApp.jwt.sign({
        sub: user.id,
        preferred_username: user.userName,
        role: user.role,
      });

      // Удаления пользователя
      const response = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(204);

      // Повторный запрос с jwt удаленного пользователя
      // Убеждаемся что пользователь больше не существует
      const deletedUserResponse = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(deletedUserResponse.statusCode).toBe(404);
    });

    it('returns 409 when admin delete himself', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'admin'
      );

      const jwtToken = testApp.jwt.sign({
        sub: user.id,
        preferred_username: user.userName,
        role: user.role,
      });

      const response = await testApp.inject({
        method: 'DELETE',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(409);
    });
  });
});
