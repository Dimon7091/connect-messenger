import { pgTable, varchar, bigint, boolean, timestamp } from 'drizzle-orm/pg-core'
import { relations } from 'drizzle-orm'

export const users = pgTable('users', {
  id: bigint('id', { mode: 'number' }).primaryKey(),
  userName: varchar('user_name', { length: 255 }).notNull().unique(),
  role: varchar('role', { length: 50 }).notNull().default('USER'),
  passwordHash: varchar('password_hash', { length: 255 }),
  createdAt: timestamp('created_at', { withTimezone: false, mode: 'date' }).defaultNow().notNull(),
  isFailedLoginLocked: boolean('is_failed_login_locked').default(false).notNull(),
  isBanned: boolean('is_banned').default(false).notNull(),
  isDeleted: boolean('is_deleted').default(false).notNull(),
})

export const profiles = pgTable('profiles', {
  userId: bigint('user_id', { mode: 'number' })
    .primaryKey()
    .references(() => users.id, { onDelete: 'cascade' }),
  firstName: varchar('first_name', { length: 255 }),
  lastName: varchar('last_name', { length: 255 }),
  avatarUrl: varchar('avatar_url', { length: 512 }),
})

export const usersRelations = relations(users, ({ one }) => ({
  profile: one(profiles),
}))

export const profilesRelations = relations(profiles, ({ one }) => ({
  user: one(users, {
    fields: [profiles.userId], 
    references: [users.id],
  }),
}))

export type Profile = typeof profiles.$inferSelect

export type BaseUser = typeof users.$inferSelect

export type User = BaseUser & {
  profile: Omit<Profile, 'userId'> | null
}
