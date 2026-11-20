import type { FastifyInstance } from 'fastify';
import { afterAll, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, getUserWithDb } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';
import { UserBaseResponseSchema } from '@messenger/shared-schema';

const URL = '/api/v1/users';

describe('SearchUsersUseCase', () => {
  let testApp: FastifyInstance;

  beforeAll(async () => {
    testApp = await createTestApp({ logger: false });
    await testApp.ready();
  });

  afterAll(async () => {
    await closeTestApp(testApp);
  });

  beforeEach(async () => {
    await resetDb(testApp.db);
  });

  // MARK: - Позитивные сценарии
  describe('Positive group', () => {
    it('returns response matching strict user contract', async () => {
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
        method: 'GET',
        url: `${URL}/search?username=alice`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();

      const contractSchema = UserBaseResponseSchema.strict().array().length(1);
      const parsed = contractSchema.safeParse(body);

      expect(parsed.success).toBe(true);
    });

    it('returns 200 and users data when jwt is valid', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'user'
      );
      const alex = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alex',
        'Alex',
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

      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'user',
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
        url: `${URL}/search?username=a`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toHaveLength(2);
      expect(body).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ id: alice.id, userName: 'alice' }),
          expect.objectContaining({ id: alex.id, userName: 'alex' }),
        ])
      );
    });
  });

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    it('returns 401 when user response without jwt', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'user',
        'User',
        '12345678',
        'user'
      );

      const response = await testApp.inject({
        method: 'GET',
        url: `${URL}/search?username=a`,
      });

      expect(response.statusCode).toBe(401);
    });
  });
});
