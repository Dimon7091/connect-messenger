import type { FastifyInstance } from 'fastify';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, signFakeJwt } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';
import { string } from 'zod';

const URL = '/api/v1/users/batch';

describe('GetUsersUseCase', () => {
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
    it('returns 200 and matching users for valid ids array', async () => {
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
        'Bob',
        '12345678',
        'user'
      );
      const charlie = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'charlie',
        'Charlie',
        '12345678',
        'user'
      );

      const jwtToken = testApp.jwt.sign({
        sub: alice.id,
        preferred_username: alice.userName,
        role: alice.role,
      });

      const response = await testApp.inject({
        method: 'POST',
        url: `${URL}`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
        payload: {
          ids: [alice.id, bob.id],
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toHaveLength(2);
      expect(body).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ id: alice.id, userName: 'alice' }),
          expect.objectContaining({ id: bob.id, userName: 'bob' }),
        ])
      );
    });
  });

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    it('returns 400 when ids contain invalid uuid', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );
      const jwtToken = testApp.jwt.sign({
        sub: alice.id,
        preferred_username: alice.userName,
        role: alice.role,
      });

      const response = await testApp.inject({
        method: 'POST',
        url: `${URL}`,
        headers: { Authorization: `Bearer ${jwtToken}` },
        payload: {
          ids: ['not-a-valid-uuid'],
        },
      });

      expect(response.statusCode).toBe(400);
    });
  });
});
