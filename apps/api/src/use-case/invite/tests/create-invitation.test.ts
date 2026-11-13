import type { FastifyInstance } from 'fastify';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb, getUserWithDb } from '../../../test/factories.js';

const URL = '/api/v1/invitations';

describe('CreateInvitationUseCase', () => {
  let testApp: FastifyInstance;

  beforeEach(async () => {
    testApp = await createTestApp({ logger: false });

    await testApp.ready();
  });

  afterEach(async () => {
    await closeTestApp(testApp);
  });

  describe('Positive group', () => {
    it('returns 201 and invitation when user role is admin', async () => {
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
        method: 'POST',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(201);

      const body = response.json();
      expect(body).toMatchObject({
        invitationUrl: expect.any(String),
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

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        headers: {
          Authorization: `Bearer ${jwtToken}`,
        },
      });

      expect(response.statusCode).toBe(403);

      const body = response.json();
      expect(body.code).toBe('FORBIDDEN');
      expect(body.invitationUrl).toBeUndefined();
    });

    it('returns 401 when request without jwt', async () => {
      const user = await createUserInDb(
        testApp.db,
        testApp.hashService,
        'alice',
        'Alice',
        '12345678',
        'admin'
      );

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
      });

      expect(response.statusCode).toBe(401);

      const body = response.json();
      expect(body.code).toBe('UNAUTHORIZED');
      expect(body.invitationUrl).toBeUndefined();
    });
  });
});
