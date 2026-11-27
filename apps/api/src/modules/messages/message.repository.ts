// repositories/message.repository.ts
import {
  eq,
  and,
  lt,
  desc,
  sql,
  inArray,
  type DbInstance,
  schema,
  type Message,
  type NewMessage,
  type MessageStatus,
  or,
  ne,
} from '@messenger/database';

const messagesTable = schema.messagesTable;

type DbOrTx = Pick<DbInstance, 'select' | 'insert' | 'update' | 'delete' | 'query'>;

export interface FindMessagesParams {
  chatId: bigint;
  userId: string;
  limit?: number | undefined;
  beforeTimestamp?: Date | undefined;
}

export interface MessageRepository {
  findById(id: bigint): Promise<Message | undefined>;
  findChatHistory(params: FindMessagesParams): Promise<Message[]>;
  findLastVisibleMessage(chatId: bigint, userId: string): Promise<Message | undefined>;
  create(data: NewMessage): Promise<Message>;
  updateText(id: bigint, text: string): Promise<Message | undefined>;

  // --- Явные переходы статуса (SENT → DELIVERED → READ) ---
  markAsSent(id: bigint, senderId: string): Promise<Message | undefined>;
  markAsDelivered(id: bigint, receiverId: string): Promise<Message | undefined>;
  markAsRead(id: bigint, receiverId: string): Promise<Message | undefined>;

  // --- Generic (оставлен для обратной совместимости) ---
  updateStatus(id: bigint, userId: string, status: MessageStatus): Promise<Message | undefined>;

  markAllChatMessagesAsRead(chatId: bigint, userId: string): Promise<boolean>;
  softDeleteForUser(id: bigint, userId: string): Promise<boolean>;
  deleteById(id: bigint): Promise<boolean>;
}

export const createMessageRepository = (getDb: () => DbOrTx): MessageRepository => ({
  // MARK: findById
  async findById(id: bigint): Promise<Message | undefined> {
    const [message] = await getDb()
      .select()
      .from(messagesTable)
      .where(eq(messagesTable.id, id))
      .limit(1);

    return message;
  },

  // MARK: findChatHistory
  async findChatHistory({
    chatId,
    userId,
    limit = 50,
    beforeTimestamp,
  }: FindMessagesParams): Promise<Message[]> {
    const safeLimit = Math.min(Math.max(limit, 1), 100);

    const conditions = [
      eq(messagesTable.chatId, chatId),
      or(eq(messagesTable.senderId, userId), eq(messagesTable.receiverId, userId)),
      sql`NOT (COALESCE(${messagesTable.deletedBy}, '[]'::jsonb) @> ${JSON.stringify([userId])}::jsonb)`,
    ];

    if (beforeTimestamp !== undefined) {
      const dateValue =
        beforeTimestamp instanceof Date ? beforeTimestamp : new Date(beforeTimestamp);
      conditions.push(lt(messagesTable.createdAt, dateValue));
    }

    return await getDb()
      .select()
      .from(messagesTable)
      .where(and(...conditions))
      .orderBy(desc(messagesTable.createdAt))
      .limit(safeLimit);
  },

  // MARK: findLastVisibleMessage
  async findLastVisibleMessage(chatId: bigint, userId: string): Promise<Message | undefined> {
    const [message] = await getDb()
      .select()
      .from(messagesTable)
      .where(
        and(
          eq(messagesTable.chatId, chatId),
          sql`NOT (${messagesTable.deletedBy} @> ${JSON.stringify([userId])}::jsonb)`
        )
      )
      .orderBy(desc(messagesTable.id))
      .limit(1);

    return message;
  },

  // MARK: create
  async create(data: NewMessage): Promise<Message> {
    const [created] = await getDb().insert(messagesTable).values(data).returning();
    if (!created) {
      throw new Error('INSERT did not return a message row');
    }
    return created;
  },

  // MARK: updateText
  async updateText(id: bigint, text: string): Promise<Message | undefined> {
    const [updated] = await getDb()
      .update(messagesTable)
      .set({ text })
      .where(eq(messagesTable.id, id))
      .returning();

    return updated;
  },

  // MARK: markAsSent
  /**
   * Переводит сообщение в статус SENT.
   * Вызывать на сервере после успешной обработки `message_sent` от отправителя.
   * - Проверяет, что сообщение принадлежит этому отправителю.
   * - Не понижает статус: если уже DELIVERED/READ, ничего не делает.
   * - Допустимый переход: PENDING/FAILED → SENT (при retry).
   */
  async markAsSent(id: bigint, senderId: string): Promise<Message | undefined> {
    const [updated] = await getDb()
      .update(messagesTable)
      .set({ status: 'SENT' })
      .where(
        and(
          eq(messagesTable.id, id),
          eq(messagesTable.senderId, senderId),
          ne(messagesTable.status, 'SENT'),
          ne(messagesTable.status, 'DELIVERED'),
          ne(messagesTable.status, 'READ')
        )
      )
      .returning();

    return updated;
  },

  // MARK: markAsDelivered
  /**
   * Переводит сообщение в статус DELIVERED.
   * Вызывать на сервере после `message_delivered` от получателя.
   * - Проверяет, что действие выполняет именно получатель.
   * - Не понижает статус: если уже READ, ничего не делает.
   * - Допустимый переход: SENT/FAILED → DELIVERED.
   */
  async markAsDelivered(id: bigint, receiverId: string): Promise<Message | undefined> {
    const [updated] = await getDb()
      .update(messagesTable)
      .set({ status: 'DELIVERED' })
      .where(
        and(
          eq(messagesTable.id, id),
          eq(messagesTable.receiverId, receiverId),
          ne(messagesTable.status, 'DELIVERED'),
          ne(messagesTable.status, 'READ')
        )
      )
      .returning();

    return updated;
  },

  // MARK: markAsRead
  /**
   * Переводит сообщение в статус READ и добавляет получателя в `read_by`.
   * Вызывать на сервере после `message_read` от получателя.
   * - Проверяет, что действие выполняет именно получатель.
   * - Идемпотентно: если userId уже в `read_by`, UPDATE не выполняется.
   */
  async markAsRead(id: bigint, receiverId: string): Promise<Message | undefined> {
    const userJsonArray = JSON.stringify([receiverId]);

    const [updated] = await getDb()
      .update(messagesTable)
      .set({
        status: 'READ',
        readBy: sql`COALESCE(${messagesTable.readBy}, '[]'::jsonb) || ${userJsonArray}::jsonb`,
      })
      .where(
        and(
          eq(messagesTable.id, id),
          eq(messagesTable.receiverId, receiverId),
          sql`NOT (COALESCE(${messagesTable.readBy}, '[]'::jsonb) @> ${userJsonArray}::jsonb)`
        )
      )
      .returning();

    return updated;
  },

  // MARK: updateStatus
  /**
   * @deprecated Предпочитайте markAsSent / markAsDelivered / markAsRead.
   * Оставлен как нижнеуровневый generic для edge-кейсов (например, FAILED).
   */
  async updateStatus(
    id: bigint,
    userId: string,
    status: MessageStatus
  ): Promise<Message | undefined> {
    const conditions = [
      eq(messagesTable.id, id),
      or(eq(messagesTable.receiverId, userId), eq(messagesTable.senderId, userId)),
      ne(messagesTable.status, status),
    ];

    if (status === 'DELIVERED') {
      conditions.push(ne(messagesTable.status, 'READ'));
    }

    const [updated] = await getDb()
      .update(messagesTable)
      .set({ status })
      .where(and(...conditions))
      .returning();

    return updated;
  },

  // MARK: markAllChatMessagesAsRead
  async markAllChatMessagesAsRead(chatId: bigint, userId: string): Promise<boolean> {
    const userJsonArray = JSON.stringify([userId]);

    const updated = await getDb()
      .update(messagesTable)
      .set({
        status: 'READ',
        readBy: sql`COALESCE(${messagesTable.readBy}, '[]'::jsonb) || ${userJsonArray}::jsonb`,
      })
      .where(
        and(
          eq(messagesTable.chatId, chatId),
          or(eq(messagesTable.receiverId, userId), eq(messagesTable.senderId, userId)),
          sql`${messagesTable.senderId} != ${userId}`,
          sql`NOT (COALESCE(${messagesTable.readBy}, '[]'::jsonb) @> ${userJsonArray}::jsonb)`
        )
      )
      .returning({ id: messagesTable.id });

    return updated.length > 0;
  },

  // MARK: softDeleteForUser
  async softDeleteForUser(id: bigint, userId: string): Promise<boolean> {
    const userJsonArray = JSON.stringify([userId]);

    const [updated] = await getDb()
      .update(messagesTable)
      .set({
        deletedBy: sql`CASE 
          WHEN ${messagesTable.deletedBy} @> ${userJsonArray}::jsonb THEN ${messagesTable.deletedBy}
          ELSE ${messagesTable.deletedBy} || ${userJsonArray}::jsonb
        END`,
      })
      .where(eq(messagesTable.id, id))
      .returning({ id: messagesTable.id });

    return Boolean(updated);
  },

  // MARK: deleteById
  async deleteById(id: bigint): Promise<boolean> {
    const [deleted] = await getDb()
      .delete(messagesTable)
      .where(eq(messagesTable.id, id))
      .returning({ id: messagesTable.id });

    return Boolean(deleted);
  },
});
