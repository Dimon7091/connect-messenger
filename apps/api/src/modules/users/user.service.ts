import type {
  PaginatedResponse,
  UpdateUserDto,
  UsersOffsetPaginationQuery,
} from '@messenger/shared-schema';
import type { PublicUser, RowUser, UserRole } from '@messenger/database';
import type { UserRepository } from './user.repository.js';
import { ConflictError, NotFoundError } from '../../lib/errors.js';
import type { NewUser } from '@messenger/database';

export interface UserService {
  create(user: NewUser, role?: UserRole): Promise<PublicUser>;
  update(id: string, dto: UpdateUserDto): Promise<PublicUser>;
  getById(id: string): Promise<PublicUser>;
  findAuthUserByUserName(userName: string): Promise<RowUser>;
  getAll(): Promise<PublicUser[]>;
  delete(id: string): Promise<void>;
  softDelete(id: string): Promise<PublicUser>;
  isUserExistById(id: string): Promise<boolean>;
  isUserExistByUserName(userName: string): Promise<boolean>;
  hasAdmin(): Promise<boolean>;
  countAllUsers(): Promise<number>;
  countDeletedUsers(): Promise<number>;
  findPaginated(query: UsersOffsetPaginationQuery): Promise<PaginatedResponse<PublicUser>>;
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

    async findAuthUserByUserName(userName) {
      const user = await repo.findRowByUserName(userName);
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
        firstName: 'deleted',
        lastName: null,
        avatarKey: null,
      };

      const deleteUser = await repo.softDeleteAndAnonymize(id, anonymizedData);
      if (!deleteUser) {
        throw new Error();
      }

      return deleteUser;
    },

    async isUserExistById(id) {
      return await repo.existsByUserId(id);
    },

    async isUserExistByUserName(userName) {
      return await repo.existsByUserName(userName);
    },

    async hasAdmin() {
      return await repo.hasAdmin();
    },

    async countAllUsers() {
      return repo.countAll();
    },

    async countDeletedUsers() {
      return repo.countDeleted();
    },

    findPaginated(query) {
      return repo.findPaginated(query);
    },
  };
}
