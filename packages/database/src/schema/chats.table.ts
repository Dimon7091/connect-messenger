import { pgTable, bigint, varchar, text, timestamp, uuid } from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';
import { chatParticipantsTable, type ChatParticipant } from './index.js';
import { getTsid } from 'tsid-ts';

export type ChatType = 'DIRECT' | 'GROUP';

export const chatsTable = pgTable('chats', {
  id: bigint('id', { mode: 'bigint' })
    .primaryKey()
    .$defaultFn(() => getTsid().toBigInt()),
  directKey: text('direct_key').unique(),
  type: varchar('type', { length: 20 }).$type<ChatType>().notNull(),
  lastMessage: text('last_message'),
  createdAt: timestamp('created_at', { withTimezone: false, mode: 'date' }).notNull().defaultNow(),
  updatedAt: timestamp('updated_at', { withTimezone: false, mode: 'date' })
    .$onUpdate(() => new Date())
    .notNull()
    .defaultNow(),
  createdBy: uuid('created_by').notNull(),
});

// Связь OneToMany с таблицей участников
export const chatsRelations = relations(chatsTable, ({ many }) => ({
  participants: many(chatParticipantsTable),
}));

export type Chat = typeof chatsTable.$inferSelect;
export type NewChat = typeof chatsTable.$inferInsert;
export type ChatWithParticipants = Chat & {
  participants: ChatParticipant[];
};
