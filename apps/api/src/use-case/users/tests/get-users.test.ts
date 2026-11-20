import type { FastifyInstance } from 'fastify';
import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, signFakeJwt } from '../../../test/factories.js';
import { resetDb } from '../../../test/helpers.js';

const URL = '/api/v1/users';

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
        url: `${URL}/me`,
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
        firstName: user.firstName,
        lastName: null,
        avatarUrl: null,
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
        url: `${URL}/me`,
        headers: {
          Authorization: `Bearer ${aliceJwt}`,
          'Content-Type': 'application/json',
        },
      });

      const bobResponse = await testApp.inject({
        method: 'GET',
        url: `${URL}/me`,
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
        firstName: alice.firstName,
        lastName: null,
        avatarUrl: null,
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
        firstName: bob.firstName,
        lastName: null,
        avatarUrl: null,
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
      url: `${URL}/me`,
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
      url: `${URL}/me`,
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

  // MARK: Админ эндпоинт
  describe('Users with pagination', () => {
    it('return 200 and user list when user role admin', async () => {
      createUserInDb(testApp.db, testApp.hashService, 'bob', 'Alice', '12345678', 'user');
      createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678', 'user');

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
        method: 'GET',
        url: `${URL}?page=1&limit=10`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(200);
      const body = response.json();
      expect(Array.isArray(body.items)).toBe(true);
      expect(body.items).toHaveLength(3);
    });

    it('return 403 when user role is not admin', async () => {
      createUserInDb(testApp.db, testApp.hashService, 'bob', 'Alice', '12345678', 'user');
      createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678', 'user');

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
        url: `${URL}?page=1&limit=10`,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(403);
      const body = response.json();
      expect(body.code).toBe('FORBIDDEN');
    });
  });
});
