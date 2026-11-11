import type { FastifyInstance } from 'fastify';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, signFakeJwt } from '../../../test/factories.js';

const URL = '/api/v1/users/me';

describe('GetUserUseCase', () => {
  let testApp: FastifyInstance;

  beforeEach(async () => {
    testApp = await createTestApp({ logger: false });
    await testApp.ready();
  });

  afterEach(async () => {
    await closeTestApp(testApp);
  });

  // MARK: - Позитивные сценарии
  describe('Positive group', () => {
    it('returns 200 and user with valid jwt', async () => {
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
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toMatchObject({
        id: user.id,
        userName: user?.userName,
        profile: {
          firstName: user.profile?.firstName,
          lastName: null,
          avatarUrl: null,
        },
        role: 'user',
        createdAt: expect.any(String),
        blackListIds: [],
        isBanned: false,
        isDeleted: false,
      });
    });

    it('returns 200 and target user with valid jwt', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const bob = await createUserInDb(testApp.db, testApp.hashService, 'bob', 'Bob', '12345678');

      const aliceJwt = testApp.jwt.sign({
        sub: alice.id,
        preferred_username: alice.userName,
        role: alice.role,
      });

      const bobJwt = testApp.jwt.sign({
        sub: bob.id,
        preferred_username: bob.userName,
        role: bob.role,
      });

      const aliceResponse = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${aliceJwt}`,
          'Content-Type': 'application/json',
        },
      });

      const bobResponse = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${bobJwt}`,
          'Content-Type': 'application/json',
        },
      });

      expect(aliceResponse.statusCode).toBe(200);
      const aliceBody = aliceResponse.json();
      expect(aliceBody).toMatchObject({
        id: alice.id,
        userName: alice?.userName,
        profile: {
          firstName: alice.profile?.firstName,
          lastName: null,
          avatarUrl: null,
        },
        role: 'user',
        createdAt: expect.any(String),
        blackListIds: [],
        isBanned: false,
        isDeleted: false,
      });

      expect(bobResponse.statusCode).toBe(200);
      const bobBody = bobResponse.json();
      expect(bobBody).toMatchObject({
        id: bob.id,
        userName: bob?.userName,
        profile: {
          firstName: bob.profile?.firstName,
          lastName: null,
          avatarUrl: null,
        },
        role: 'user',
        createdAt: expect.any(String),
        blackListIds: [],
        isBanned: false,
        isDeleted: false,
      });
    });
  });

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    it('returns 401 when Authorization header is missing', async () => {
      const response = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(401);
    });
  });

  it('returns 401 with invalid jwt token string', async () => {
    const response = await testApp.inject({
      method: 'GET',
      url: URL,
      headers: {
        Authorization: 'Bearer completely_broken_jwt_token_string',
        'Content-Type': 'application/json',
      },
    });

    expect(response.statusCode).toBe(401);
  });

  it('returns 404 when user exists in JWT but does not exist in database', async () => {
    await createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678');
    // Генерируем валидный JWT, но зашиваем туда несуществующий UUID
    const fakeUserId = '00000000-0000-0000-0000-000000000000';

    const jwtToken = testApp.jwt.sign({
      sub: fakeUserId,
      preferred_username: 'ghost_user',
      role: 'user',
    });

    const response = await testApp.inject({
      method: 'GET',
      url: URL,
      headers: {
        Authorization: `Bearer ${jwtToken}`,
        'Content-Type': 'application/json',
      },
    });

    expect(response.statusCode).toBe(404);
  });

  // MARK: Тест на фейковый токен
  describe('Face jwt token', () => {
    let testApp: FastifyInstance;

    beforeEach(async () => {
      testApp = await createTestApp({ logger: false });

      await testApp.ready();
    });

    afterEach(async () => {
      await closeTestApp(testApp);
    });

    it('returns 401 when JWT is signed with an invalid secret', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const fakeToken = signFakeJwt(
        {
          sub: user.id,
          preferred_username: user.userName,
          role: 'user',
        },
        'wrong-secret-key'
      );

      const response = await testApp.inject({
        method: 'GET',
        url: URL,
        headers: {
          Authorization: `Bearer ${fakeToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(401);
      const body = response.json();
      expect(body.code).toBe('UNAUTHORIZED');
    });
  });
});
