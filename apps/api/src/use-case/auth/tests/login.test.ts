import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import type { FastifyInstance } from 'fastify';
import {
  CreateUserSchema,
  type AuthRequestDto,
  type CreateUserDto,
} from '@messenger/shared-schema';
import { createUserInDb } from '../../../test/factories.js';

const URL = '/api/v1/auth/login';

// MARK: - Вход пользователя
describe('LoginUseCase (IT)', () => {
  let testApp: FastifyInstance;

  beforeEach(async () => {
    testApp = await createTestApp({ logger: false });
    await testApp.ready();
  });

  afterEach(async () => {
    await closeTestApp(testApp);
  });

  // MARK: Позитивные сценарии
  describe('Positive group', () => {
    it('returns 200 and auth response', async () => {
      const createdUser = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const authRequestDto: AuthRequestDto = {
        userName: 'alice',
        password: '12345678',
      };

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: authRequestDto,
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toMatchObject({
        user: {
          id: expect.any(String),
          userName: createdUser?.userName,
          profile: {
            firstName: createdUser.profile?.firstName,
            lastName: null,
            avatarUrl: null,
          },
          role: 'user',
          createdAt: expect.any(String),
          blackListIds: [],
          isBanned: false,
          isDeleted: false,
        },
        token: expect.any(String),
      });
    });
  });

  // MARK: Негативные сценарии
  describe('Negative group', () => {
    it('returns 401 when username is invalid', async () => {
      await createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678');

      const authRequestDto: AuthRequestDto = {
        userName: 'invalid-username',
        password: '12345678',
      };

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: authRequestDto,
      });

      expect(response.statusCode).toBe(401);
      const body = response.json();
      expect(body.code).toBe('INVALID_CREDENTIALS');
    });

    it('returns 401 when password is invalid', async () => {
      await createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678');

      const authRequestDto: AuthRequestDto = {
        userName: 'alice',
        password: 'invalid-password',
      };

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: authRequestDto,
      });

      expect(response.statusCode).toBe(401);
      const body = response.json();
      expect(body.code).toBe('INVALID_CREDENTIALS');
    });
  });
});
