// src/db/schema/users.ts
import { pgTable, varchar, boolean, timestamp, uuid, index, pgEnum } from 'drizzle-orm/pg-core';

export const userRole = pgEnum('user_role', ['admin', 'user']);

export const usersTable = pgTable(
  'users',
  {
    id: uuid('id').primaryKey().defaultRandom(),

    // --- Auth ---
    userName: varchar('user_name', { length: 255 }).notNull().unique(),
    passwordHash: varchar('password_hash', { length: 255 }).notNull(),
    role: userRole('role').notNull().default('user'),

    // --- Profile ---
    firstName: varchar('first_name', { length: 255 }).notNull(),
    lastName: varchar('last_name', { length: 255 }),
    avatarUrl: varchar('avatar_url', { length: 512 }),

    // --- Флаги ---
    isFailedLoginLocked: boolean('is_failed_login_locked').notNull().default(false),
    isBanned: boolean('is_banned').notNull().default(false),
    isDeleted: boolean('is_deleted').notNull().default(false),

    // --- Timestamps ---
    createdAt: timestamp('created_at', { withTimezone: false, mode: 'date' })
      .notNull()
      .defaultNow(),
  },
  t => [
    // Индексы под пагинацию: (sortCol, id). Направление в индексе — ASC;
    index('users_created_at_id_idx').on(t.createdAt, t.id),
    index('users_user_name_id_idx').on(t.userName, t.id),
    index('users_first_name_id_idx').on(t.firstName, t.id),
    index('users_last_name_id_idx').on(t.lastName, t.id),

    // Составной под частый фильтр + сортировку
    index('users_role_created_at_id_idx').on(t.role, t.createdAt, t.id),
  ]
);

// ---------------------------------------------------------------------------
// MARK: - Типы
// ---------------------------------------------------------------------------

export type UserRole = typeof usersTable.$inferSelect.role;

/**
 * Плоский пользователь
 */
export type RowUser = typeof usersTable.$inferSelect;

export type NewUser = typeof usersTable.$inferInsert;

/**
 * Публичный shape — без passwordHash.
 */
export type PublicUser = Omit<RowUser, 'passwordHash'>;
