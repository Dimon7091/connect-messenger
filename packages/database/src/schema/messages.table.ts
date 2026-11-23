import { pgTable, bigint, uuid, text, varchar, timestamp, jsonb, index } from 'drizzle-orm/pg-core';
import { relations } from 'drizzle-orm';
import { chatsTable, usersTable } from './index.js';
import { getTsid } from 'tsid-ts';
// import { generateTsid } from '../utils/tsid';

// Типы для JSONB-полей и статуса сообщения
export type MessageStatus = 'SENT' | 'DELIVERED' | 'READ' | 'FAILED';

export interface MessageAttachment {
  fileKey: string;
  mimeType: string;
  fileName: string;
  size: string;
}

export const messagesTable = pgTable(
  'messages',
  {
    id: bigint('id', { mode: 'bigint' })
      .primaryKey()
      .$defaultFn(() => getTsid().toBigInt()),

    chatId: bigint('chat_id', { mode: 'bigint' })
      .notNull()
      .references(() => chatsTable.id, { onDelete: 'cascade' }),

    senderId: uuid('sender_id')
      .notNull()
      .references(() => usersTable.id, { onDelete: 'cascade' }),

    receiverId: uuid('receiver_id')
      .notNull()
      .references(() => usersTable.id, {
        onDelete: 'set null',
      }),

    text: text('text'),

    timestamp: timestamp('timestamp', { withTimezone: true, mode: 'date' }).defaultNow().notNull(),
    createdAt: timestamp('created_at', { withTimezone: true, mode: 'date' }).defaultNow().notNull(),
    status: varchar('status', { length: 20 }).$type<MessageStatus>().default('SENT').notNull(),
    replyToId: bigint('reply_to_id', { mode: 'bigint' }),

    // JSONB-поля с автовыводом строгих TypeScript-типов
    attachments: jsonb('attachments').$type<MessageAttachment[]>().default([]),
    deletedBy: jsonb('deleted_by').$type<string[]>().default([]).notNull(),
    readBy: jsonb('read_by').$type<string[]>().default([]),
  },
  table => [
    index('idx_messages_chat_timestamp').on(table.chatId, table.timestamp, table.readBy),

    // Дополнительный индекс для сверхбыстрой курсорной пагинации истории по TSID (WHERE chat_id = ? AND id < ?)
    index('idx_messages_chat_id_desc').on(table.chatId, table.id),
  ]
);

// Связи для db.query (если понадобятся для подгрузки автора или цитируемого сообщения)
export const messagesRelations = relations(messagesTable, ({ one }) => ({
  chat: one(chatsTable, {
    fields: [messagesTable.chatId],
    references: [chatsTable.id],
  }),
  sender: one(usersTable, {
    fields: [messagesTable.senderId],
    references: [usersTable.id],
  }),
  replyTo: one(messagesTable, {
    fields: [messagesTable.replyToId],
    references: [messagesTable.id],
  }),
}));

export type Message = typeof messagesTable.$inferSelect;
export type NewMessage = typeof messagesTable.$inferInsert;
