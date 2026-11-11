import { pgTable, varchar, boolean, timestamp, uuid, index, pgEnum } from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';

// Таблица Пользователей
export const userRole = pgEnum('user_role', ['admin', 'user']);

export const usersTable = pgTable('users', {
  id: uuid('id').primaryKey().defaultRandom(),
  userName: varchar('user_name', { length: 255 }).notNull().unique(),

  // Используем объявленный enum
  role: userRole('role').notNull().default('user'),

  passwordHash: varchar('password_hash', { length: 255 }).notNull(),
  createdAt: timestamp('created_at', { withTimezone: false, mode: 'date' }).defaultNow().notNull(),
  isFailedLoginLocked: boolean('is_failed_login_locked').default(false).notNull(),
  isBanned: boolean('is_banned').default(false).notNull(),
  isDeleted: boolean('is_deleted').default(false).notNull(),
});

// Таблица Профилей
export const usersProfilesTable = pgTable(
  'usersProfiles',
  {
    userId: uuid('user_id')
      .primaryKey()
      .references(() => usersTable.id, { onDelete: 'cascade' }),
    firstName: varchar('first_name', { length: 255 }).notNull(),
    lastName: varchar('last_name', { length: 255 }),
    avatarUrl: varchar('avatar_url', { length: 512 }),
  },
  table => [index('users_profiles_user_id_idx').on(table.userId)]
);

export const usersRelations = relations(usersTable, ({ one }) => ({
  profile: one(usersProfilesTable),
}));

export const profilesRelations = relations(usersProfilesTable, ({ one }) => ({
  user: one(usersTable, {
    fields: [usersProfilesTable.userId],
    references: [usersTable.id],
  }),
}));

export type UserRole = typeof usersTable.$inferSelect.role;

export type usersProfiles = typeof usersProfilesTable.$inferSelect;
export type BaseUser = typeof usersTable.$inferSelect;

export type User = BaseUser & {
  profile: Omit<usersProfiles, 'userId'> | null;
};

export type NewUser = typeof usersTable.$inferInsert & {
  profile?: Omit<typeof usersProfilesTable.$inferInsert, 'userId'> | null;
};
