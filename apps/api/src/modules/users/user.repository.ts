import { eq, and, ilike } from '@messenger/database';
import type { DbInstance, PublicUser } from '@messenger/database';
import { usersTable, type RowUser, type NewUser } from '@messenger/database';
import type {
  PaginatedResponse,
  UpdateUserDto,
  UsersOffsetPaginationQuery,
} from '@messenger/shared-schema';
import {
  buildOrderBy,
  createPaginatedResponse,
  getPaginationParams,
} from '../../utils/pagination.js';

export interface UserRepository {
  //Базовый CRUD
  create(data: NewUser): Promise<PublicUser>;
  update(id: string, data: UpdateUserDto): Promise<PublicUser | null>;
  findById(id: string): Promise<RowUser | null>;
  findRowByUserName(userName: string): Promise<RowUser | null>;
  findAll(): Promise<PublicUser[]>;
  deleteById(id: string): Promise<void>;

  // Доп функции
  existsByUserId(id: string): Promise<boolean>;
  existsByUserName(username: string): Promise<boolean>;
  isAdmin(id: string): Promise<boolean>;
  hasAdmin(): Promise<boolean>;
  isUserDeleted(id: string): Promise<boolean>;
  softDeleteAndAnonymize(id: string, anonymizedData: NewUser): Promise<PublicUser | null>;
  countAll(): Promise<number>;
  countDeleted(): Promise<number>;

  // Пагинация
  findPaginated(query: UsersOffsetPaginationQuery): Promise<PaginatedResponse<PublicUser>>;
}

// Словарь разрешенных колонок для сортировки пользователей
const USER_SORT_COLUMNS = {
  createdAt: usersTable.createdAt,
  userName: usersTable.userName,
  firstName: usersTable.firstName,
} as const;

export function createUserRepository(db: DbInstance): UserRepository {
  return {
    // MARK: - Базовый CRUD
    async create(data) {
      const [savedUser] = await db.insert(usersTable).values(data).returning();
      if (!savedUser) throw new Error('INSERT did not return a user row');
      return savedUser;
    },

    async findById(id): Promise<RowUser | null> {
      const result = await db.query.usersTable.findFirst({
        where: (users, { eq }) => eq(users.id, id),
      });
      return (result as RowUser) ?? null;
    },

    async findRowByUserName(userName) {
      const result = await db.query.usersTable.findFirst({
        where: (users, { eq }) => eq(users.userName, userName),
      });
      return (result as RowUser) ?? null;
    },

    async findAll() {
      const result = await db.query.usersTable.findMany();
      return result as PublicUser[];
    },

    async update(id, data) {
      if (!data || Object.keys(data).length === 0) {
        return await this.findById(id);
      }

      await db.update(usersTable).set(data).where(eq(usersTable.id, id));
      return (await this.findById(id)) ?? null;
    },

    async deleteById(id) {
      await db.delete(usersTable).where(eq(usersTable.id, id)).returning();
    },

    // MARK: - Доп функции
    async existsByUserId(id) {
      const [result] = await db
        .select({ id: usersTable.id })
        .from(usersTable)
        .where(eq(usersTable.id, id))
        .limit(1);

      return !!result;
    },

    async existsByUserName(userName) {
      const [result] = await db
        .select({ id: usersTable.id })
        .from(usersTable)
        .where(eq(usersTable.userName, userName))
        .limit(1);

      return !!result;
    },

    async isAdmin(userId) {
      const [user] = await db
        .select({ role: usersTable.role })
        .from(usersTable)
        .where(and(eq(usersTable.id, userId), eq(usersTable.isDeleted, false)))
        .limit(1);

      return user?.role === 'admin';
    },

    async hasAdmin(): Promise<boolean> {
      const [admin] = await db
        .select({ id: usersTable.id })
        .from(usersTable)
        .where(eq(usersTable.role, 'admin'))
        .limit(1);

      return Boolean(admin);
    },

    async isUserDeleted(id) {
      const [result] = await db
        .select({ isDeleted: usersTable.isDeleted })
        .from(usersTable)
        .where(eq(usersTable.id, id));

      return result?.isDeleted ?? false;
    },

    async softDeleteAndAnonymize(id, anonymizedData) {
      const [deletedUser] = await db
        .update(usersTable)
        .set(anonymizedData)
        .where(eq(usersTable.id, id))
        .returning();
      return deletedUser ?? null;
    },

    async countAll() {
      return await db.$count(usersTable);
    },

    async countDeleted() {
      return await db.$count(usersTable, and(eq(usersTable.isDeleted, true)));
    },

    // MARK: Пагинация
    async findPaginated(query) {
      const { limit, offset } = getPaginationParams(query);
      const orderBy = buildOrderBy(USER_SORT_COLUMNS, query.sortBy, query.sortOrder, usersTable.id);

      // Формируем условие поиска, если передан параметр search
      const whereClause = query.search
        ? ilike(usersTable.userName, `%${query.search}%`)
        : undefined;

      // Передаем whereClause в ОБА запроса
      const [items, total] = await Promise.all([
        db
          .select()
          .from(usersTable)
          .where(whereClause)
          .orderBy(...orderBy)
          .limit(limit)
          .offset(offset),
        db.$count(usersTable, whereClause),
      ]);

      return createPaginatedResponse(items as PublicUser[], total, query);
    },
  };
}
