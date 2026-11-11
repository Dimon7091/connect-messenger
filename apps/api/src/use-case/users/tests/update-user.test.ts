import type { FastifyInstance } from 'fastify';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, signFakeJwt } from '../../../test/factories.js';
import type { UpdateUserDto } from '@messenger/shared-schema';

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
    it('returns 200 and updated user when update all fields', async () => {
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

      const updatedUserDto: UpdateUserDto = {
        userName: 'update-username',
        profile: {
          firstName: 'Updated-fistName',
          lastName: 'Updated-lastName',
        },
      };

      const response = await testApp.inject({
        method: 'PATCH',
        url: URL,
        body: updatedUserDto,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toMatchObject({
        id: user.id,
        userName: updatedUserDto.userName,
        profile: {
          firstName: updatedUserDto.profile?.firstName,
          lastName: updatedUserDto.profile?.lastName,
          avatarUrl: null,
        },
        role: 'user',
        createdAt: expect.any(String),
        blackListIds: [],
        isBanned: false,
        isDeleted: false,
      });
    });

    it('returns 200 and user when updating with empty object', async () => {
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

      const updatedUserDto: UpdateUserDto = {};

      const response = await testApp.inject({
        method: 'PATCH',
        url: URL,
        body: updatedUserDto,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toMatchObject({
        id: user.id,
        userName: 'alice',
        profile: {
          firstName: 'Alice',
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

    it('returns 200 and updated user when update only userName', async () => {
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

      const updatedUserDto: UpdateUserDto = {
        userName: 'update-username',
      };

      const response = await testApp.inject({
        method: 'PATCH',
        url: URL,
        body: updatedUserDto,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(200);

      const body = response.json();
      expect(body).toMatchObject({
        id: user.id,
        userName: updatedUserDto.userName,
        profile: {
          firstName: user.profile?.firstName,
          lastName: user.profile?.lastName,
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
    it('returns 404 when user does not exist', async () => {
      const response = await testApp.inject({
        method: 'PATCH',
        url: `${URL}/99999`,
        payload: { name: 'New Name' },
      });

      expect(response.statusCode).toBe(404);
      expect(response.json().code).toBe('NOT_FOUND');
    });

    it('returns 409 when user update username which is already taken', async () => {
      const alice = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678'
      );

      const bob = await createUserInDb(testApp.db, testApp.hashService, 'bob', 'Bob', '12345678');

      const bobJwtToken = testApp.jwt.sign({
        sub: bob.id,
        preferred_username: bob.userName,
        role: bob.role,
      });

      const bobUpdateDto: UpdateUserDto = {
        userName: alice.userName,
      };

      const response = await testApp.inject({
        method: 'PATCH',
        url: URL,
        body: bobUpdateDto,
        headers: {
          Authorization: `Bearer ${bobJwtToken}`,
          'Content-Type': 'application/json',
        },
      });

      expect(response.statusCode).toBe(409);
      expect(response.json().code).toBe('CONFLICT');
    });
  });
});
