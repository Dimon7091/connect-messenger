import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import type { FastifyInstance } from 'fastify';
import { CreateUserSchema, type CreateUserDto } from '@messenger/shared-schema';
import { closeTestApp, createTestApp } from '../../../test/setup.js';
import { createUserInDb } from '../../../test/factories.js';

const URL = '/api/v1/auth/register';

describe('RegisterUseCase (IT)', () => {
  let testApp: FastifyInstance;

  beforeEach(async () => {
    testApp = await createTestApp({ logger: false });
    await testApp.ready();
  });

  afterEach(async () => {
    await closeTestApp(testApp);
  });

  const validUserDto: CreateUserDto = {
    userName: 'alice',
    firstName: 'Alice',
    lastName: 'Alice',
    password: '12345678',
    invitationToken: 'invite-token',
  };

  // MARK: - Позитивные сценарии
  describe('Positive group', () => {
    it('returns 201 and created user with valid body(all fields)', async () => {
      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: validUserDto,
      });

      expect(response.statusCode).toBe(201);

      const body = response.json();
      expect(body).toMatchObject({
        user: {
          id: expect.any(String),
          userName: validUserDto.userName,
          profile: {
            firstName: validUserDto.firstName,
            lastName: validUserDto.lastName,
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

    it('returns 201 and created user with valid body(without lastName)', async () => {
      const { lastName, ...dtoWithOutLastName } = validUserDto;

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: dtoWithOutLastName,
      });

      expect(response.statusCode).toBe(201);

      const body = response.json();
      expect(body).toMatchObject({
        user: {
          id: expect.any(String),
          userName: dtoWithOutLastName.userName,
          profile: {
            firstName: dtoWithOutLastName.firstName,
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

  // MARK: - Негативные сценарии
  describe('Negative group', () => {
    // Хелпер для проверки конкретного поля на ожидаемую ошибку
    const assertFieldError = (data: unknown, field: string, expectedMessage: string) => {
      const result = CreateUserSchema.safeParse(data);
      expect(result.success).toBe(false);
      if (!result.success) {
        const fieldIssue = result.error.issues.find(issue => issue.path.includes(field));
        expect(fieldIssue).toBeDefined();
        expect(fieldIssue?.message).toBe(expectedMessage);
      }
    };

    it('returns 409 when username already exist', async () => {
      await createUserInDb(testApp.db, testApp.hashService, 'alice', 'Alice', '12345678');

      const newUserDto: CreateUserDto = {
        userName: 'alice',
        firstName: 'Alice',
        password: '12345678',
        invitationToken: 'invite-token',
      };

      const response = await testApp.inject({
        method: 'POST',
        url: URL,
        payload: newUserDto,
      });

      expect(response.statusCode).toBe(409);
    });

    // MARK: Проверка валидации
    describe('validate userName', () => {
      it.each([
        ['ab', 'Имя пользователя должно быть не менее 3 символа'],
        ['a'.repeat(16), 'Имя пользователя не может превышать 15 символов'],
        [
          'alex@99!',
          'Имя пользователя может содержать только английские буквы, цифры, дефис, подчеркивание, точку',
        ],
        ['12345...', 'Имя пользователя не может состоять только из цифр или только из точек'],
      ])('не должен пропускать userName "%s" -> ошибка: "%s"', (value, expectedError) => {
        assertFieldError({ ...validUserDto, userName: value }, 'userName', expectedError);
      });
    });

    describe('validate firstName', () => {
      it.each([
        ['А', 'Имя должно быть не менее 2 символа'],
        ['А'.repeat(21), 'Имя не может превышать 20 символов'],
        [
          'Иван Петр',
          'Имя может содержать только буквы русского или английского языка без пробелов и знаков препинания',
        ],
      ])('не должен пропускать firstName "%s"', (value, expectedError) => {
        assertFieldError({ ...validUserDto, firstName: value }, 'firstName', expectedError);
      });
    });

    describe('validate lastName', () => {
      it.each([
        ['Я', 'Фамилия должна быть не менее 2 символа'],
        ['И'.repeat(26), 'Фамилия не может превышать 25 символов'],
      ])('не должен пропускать lastName "%s"', (value, expectedError) => {
        assertFieldError({ ...validUserDto, lastName: value }, 'lastName', expectedError);
      });
    });

    describe('validate password', () => {
      it.each([
        ['short1', 'Пароль должен быть не менее 8 символов'],
        ['p'.repeat(21), 'Пароль не может превышать 20 символов'],
        ['        ', 'Пароль не может быть пустым'],
      ])('не должен пропускать пароль "%s"', (value, expectedError) => {
        assertFieldError({ ...validUserDto, password: value }, 'password', expectedError);
      });
    });

    describe('validate invitationToken', () => {
      it('должен падать, если токен отсутствует в объекте', () => {
        const { invitationToken, ...dtoWithoutToken } = validUserDto;
        const result = CreateUserSchema.safeParse(dtoWithoutToken);

        expect(result.success).toBe(false);
        if (!result.success) {
          const tokenIssue = result.error.issues.find(i => i.path.includes('invitationToken'));
          expect(tokenIssue).toBeDefined();
          expect(tokenIssue?.code).toBe('invalid_type');
        }
      });
    });
  });
});
