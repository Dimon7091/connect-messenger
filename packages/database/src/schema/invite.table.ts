import { pgTable, varchar, timestamp } from 'drizzle-orm/pg-core';

export const invitationsTable = pgTable('invitations', {
  token: varchar('token', { length: 32 }).primaryKey().notNull(),
  createdAt: timestamp('created_at', { withTimezone: false, mode: 'date' }).defaultNow().notNull(),
});

export type Invitation = typeof invitationsTable.$inferSelect;
export type NewInvitation = typeof invitationsTable.$inferInsert;
