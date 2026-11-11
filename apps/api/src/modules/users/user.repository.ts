import { eq, count, and } from '@messenger/database';
import type { DbInstance } from '@messenger/database';
import { usersTable, usersProfilesTable, type User, type NewUser } from '@messenger/database';
import type { UpdateUserDto } from '@messenger/shared-schema';

export interface UserRepository {
  //Базовый CRUD
  create(data: NewUser): Promise<User>;
  update(id: string, data: UpdateUserDto): Promise<User | null>;
  findById(id: string): Promise<User | null>;
  findByUserName(userName: string): Promise<User | null>;
  findAll(): Promise<User[]>;
  deleteById(id: string): Promise<void>;

  // Доп функции
  existsByUserId(id: string): Promise<boolean>;
  existsByUserName(username: string): Promise<boolean>;
  isAdmin(id: string): Promise<boolean>;
  isUserDeleted(id: string): Promise<boolean>;
  softDeleteAndAnonymize(id: string, anonymizedData: NewUser): Promise<void>;
}

export function createUserRepository(db: DbInstance): UserRepository {
  return {
    // MARK: - Базовый CRUD
    async create(data) {
      return await db.transaction(async tx => {
        const { profile, ...userData } = data;

        // Вставка пользователя
        const [savedUser] = await tx.insert(usersTable).values(userData).returning();
        if (!savedUser) throw new Error('INSERT did not return a user row');

        // Вставка профиля (если он передан)
        let profileWithoutId = null;
        if (profile) {
          const [savedProfile] = await tx
            .insert(usersProfilesTable)
            .values({ ...profile, userId: savedUser.id })
            .returning();

          if (!savedProfile) throw new Error('INSERT did not return a profile row');

          const { userId, ...restProfile } = savedProfile;
          profileWithoutId = restProfile;
        }

        // Возврат объединенного объекта
        return {
          ...savedUser,
          profile: profileWithoutId,
        };
      });
    },

    async findById(id): Promise<User | null> {
      const result = await db.query.usersTable.findFirst({
        where: (users, { eq }) => eq(users.id, id),
        with: {
          profile: {
            columns: {
              userId: false,
            },
          },
        },
      });

      return result as User | null;
    },

    async findByUserName(userName) {
      const result = await db.query.usersTable.findFirst({
        where: (users, { eq }) => eq(users.userName, userName),
        with: {
          profile: {
            columns: {
              userId: false,
            },
          },
        },
      });
      return result as User | null;
    },

    async findAll() {
      const result = await db.query.usersTable.findMany({
        with: {
          profile: {
            columns: {
              userId: false,
            },
          },
        },
      });

      return result as User[];
    },

    async update(id, data) {
      if (!data || Object.keys(data).length === 0) {
        return await this.findById(id);
      }

      const { profile, ...userData } = data;
      const hasUserData = Object.keys(userData).length > 0;
      // Проверяем, что в профиле есть реальные данные для обновления
      const hasProfileData = profile && Object.keys(profile).length > 0;

      // Если обновлять нечего (например, прислали { profile: {} })
      if (!hasUserData && !hasProfileData) {
        return await this.findById(id);
      }

      await db.transaction(async tx => {
        if (hasUserData) {
          await tx.update(usersTable).set(userData).where(eq(usersTable.id, id));
        }

        if (hasProfileData) {
          await tx.update(usersProfilesTable).set(profile).where(eq(usersProfilesTable.userId, id));
        }
      });

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

    async isUserDeleted(id) {
      const [result] = await db
        .select({ isDeleted: usersTable.isDeleted })
        .from(usersTable)
        .where(eq(usersTable.id, id));

      return result?.isDeleted ?? false;
    },

    async softDeleteAndAnonymize(id, anonymizedData) {
      const { profile, ...userData } = anonymizedData;
      await db.update(usersTable).set(userData).where(eq(usersTable.id, id));
      if (profile) {
        await db.update(usersProfilesTable).set(profile).where(eq(usersProfilesTable.userId, id));
      }
    },
  };
}
