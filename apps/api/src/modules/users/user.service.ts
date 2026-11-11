import type { CreateUserDto, UpdateUserDto } from '@messenger/shared-schema';
import type { User, UserRole } from '@messenger/database';
import type { UserRepository } from './user.repository.js';
import { ConflictError, NotFoundError } from '../../lib/errors.js';
import type { NewUser } from '@messenger/database';
import type { UUID } from 'node:crypto';

export interface UserService {
  create(user: NewUser, role?: UserRole): Promise<User>;
  update(id: string, dto: UpdateUserDto): Promise<User>;
  getById(id: string): Promise<User>;
  getByUserName(userName: string): Promise<User>;
  getAll(): Promise<User[]>;
  delete(id: string): Promise<void>;
  softDelete(id: string): Promise<void>;
  isUserExistById(id: string): Promise<boolean>;
  isUserExistByUserName(userName: string): Promise<boolean>;
}

export function createUserService(repo: UserRepository): UserService {
  return {
    async create(user: NewUser) {
      const existing = await repo.existsByUserName(user.userName);
      if (existing) {
        throw new ConflictError(`User with username: ${user.userName} already exists`);
      }

      return repo.create(user);
    },

    async getById(id) {
      const user = await repo.findById(id);
      if (!user) {
        throw new NotFoundError('User', id.toString());
      }
      return user;
    },

    async getByUserName(userName) {
      const user = await repo.findByUserName(userName);
      if (!user) {
        throw new NotFoundError('User', userName);
      }
      return user;
    },

    async getAll() {
      return repo.findAll();
    },

    async update(id, dto) {
      // Проверяем, существует ли сам пользователь
      const existingUser = await repo.findById(id);
      if (!existingUser) {
        throw new NotFoundError('User', id.toString());
      }

      // Если передан новый userName и он отличается от текущего — проверяем уникальность
      if (dto.userName && dto.userName !== existingUser.userName) {
        const isUserNameExist = await repo.existsByUserName(dto.userName);

        if (isUserNameExist) {
          throw new ConflictError(`Username: ${dto.userName} is already taken`);
        }
      }

      // После всех проверок обновляем запись в БД
      const updatedUser = await repo.update(id, dto);
      if (!updatedUser) {
        throw new NotFoundError('User', id);
      }

      return updatedUser;
    },

    async delete(id) {
      await repo.deleteById(id);
    },

    async softDelete(id) {
      const user = await repo.findById(id);
      if (!user) {
        throw new NotFoundError('User', id);
      }

      if (user.isDeleted) {
        throw new NotFoundError('User', id);
      }

      if (user.role === 'admin') {
        throw new ConflictError('Administrator cannot delete his account.');
      }

      const anonymizedData = {
        userName: `deleted_user_${id}`,
        passwordHash: 'REDACTED',
        isDeleted: true,
        profile: {
          firstName: 'deleted',
          lastName: 'deleted',
          avatarKey: null,
        },
      };

      await repo.softDeleteAndAnonymize(id, anonymizedData);
    },

    async isUserExistById(id) {
      return await repo.existsByUserId(id);
    },

    async isUserExistByUserName(userName) {
      return await repo.existsByUserName(userName);
    },
  };
}
