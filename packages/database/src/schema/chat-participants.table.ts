import {
  pgTable,
  bigint,
  uuid,
  integer,
  boolean,
  timestamp,
  primaryKey,
  index,
} from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';
import { chatsTable } from './chats.table.js';
import { usersTable } from './users.tables.js';

export const chatParticipantsTable = pgTable(
  'chat_participants',
  {
    // TSID чата (64-битный BIGINT)
    chatId: bigint('chat_id', { mode: 'bigint' })
      .notNull()
      .references(() => chatsTable.id, { onDelete: 'cascade' }),

    // UUID пользователя (нативный тип uuid в PostgreSQL)
    userId: uuid('user_id')
      .notNull()
      .references(() => usersTable.id, { onDelete: 'cascade' }),

    isDeleted: boolean('is_deleted').default(false).notNull(),
    deletedAt: timestamp('deleted_at', { withTimezone: true }),
    unreadCount: integer('unread_count').default(0).notNull(),
    lastReadMessageId: bigint('last_read_message_id', { mode: 'bigint' }),
    lastReadAt: timestamp('last_read_at', { withTimezone: true }),
    isChatEmpty: boolean('is_chat_empty').default(true),
    isMuted: boolean('is_muted').default(false).notNull(),
    pinnedAt: timestamp('pinned_at', { withTimezone: true }),
    joinedAt: timestamp('joined_at', { withTimezone: true }).defaultNow().notNull(),
  },
  table => [
    // Составной первичный ключ из bigint + uuid
    primaryKey({ columns: [table.chatId, table.userId] }),

    // Индекс по UUID пользователя для быстрой загрузки его списка чатов
    index('chat_participants_user_id_idx').on(table.userId),
  ]
);

export const chatParticipantsRelations = relations(chatParticipantsTable, ({ one }) => ({
  chat: one(chatsTable, {
    fields: [chatParticipantsTable.chatId],
    references: [chatsTable.id],
  }),
  user: one(usersTable, {
    fields: [chatParticipantsTable.userId],
    references: [usersTable.id],
  }),
}));

export type ChatParticipant = typeof chatParticipantsTable.$inferSelect;
export type NewChatParticipant = typeof chatParticipantsTable.$inferInsert;
