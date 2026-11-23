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
} from '@messenger/database';

const messagesTable = schema.messagesTable;

// Тип поддерживает как основной инстанс БД, так и объект транзакции (tx)
type DbOrTx = Pick<DbInstance, 'select' | 'insert' | 'update' | 'delete' | 'query'>;

export interface FindMessagesParams {
  chatId: bigint;
  userId: string; // Нужен, чтобы исключить сообщения, удаленные этим пользователем (deletedBy)
  limit?: number | undefined;
  beforeTimestamp: Date;
}

export interface MessageRepository {
  findById(id: bigint): Promise<Message | undefined>;
  findChatHistory(params: FindMessagesParams): Promise<Message[]>;
  findLastVisibleMessage(chatId: bigint, userId: string): Promise<Message | undefined>;
  create(data: NewMessage): Promise<Message>;
  updateText(id: bigint, text: string): Promise<Message | undefined>;
  updateStatus(id: bigint, status: MessageStatus): Promise<Message | undefined>;
  markManyAsRead(chatId: bigint, userId: string, upToMessageId: bigint): Promise<void>;
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
  /**
   * Курсорная пагинация истории чата по TSID (от новых к старым).
   * Исключает сообщения, которые пользователь удалил «только для себя» (deletedBy @> '["userId"]').
   */
  async findChatHistory({
    chatId,
    userId,
    limit = 50,
    beforeTimestamp,
  }: FindMessagesParams): Promise<Message[]> {
    // Защита от некорректных лимитов
    const safeLimit = Math.min(Math.max(limit, 1), 100);

    const conditions = [
      eq(messagesTable.chatId, chatId),
      // COALESCE гарантирует корректную работу, даже если в базе лежит NULL
      sql`NOT (COALESCE(${messagesTable.deletedBy}, '[]'::jsonb) @> ${JSON.stringify([userId])}::jsonb)`,
    ];

    if (beforeTimestamp !== undefined) {
      conditions.push(lt(messagesTable.createdAt, beforeTimestamp));
    }

    const rows = await getDb()
      .select()
      .from(messagesTable)
      .where(and(...conditions))
      // Сортируем по тому же полю, по которому идет курсор (createdAt) + id для детерминированности
      .orderBy(desc(messagesTable.createdAt), desc(messagesTable.id))
      .limit(safeLimit);

    // Опционально: разворачиваем, чтобы в чате сообщения шли хронологически сверху вниз
    return rows.reverse();
  },

  // MARK: findLastVisibleMessage
  /**
   * Поиск последнего сообщения в чате, не удаленного пользователем
   * (полезно при удалении последнего сообщения для обновления превью чата)
   */
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

  // MARK: updateStatus
  async updateStatus(id: bigint, status: MessageStatus): Promise<Message | undefined> {
    const [updated] = await getDb()
      .update(messagesTable)
      .set({ status })
      .where(eq(messagesTable.id, id))
      .returning();

    return updated;
  },

  // MARK: markManyAsRead
  /**
   * Массовая отметка сообщений как прочитанных (до upToMessageId включительно):
   * добавляет userId в JSONB-массив read_by, если его там еще нет, и обновляет статус на READ.
   */
  async markManyAsRead(chatId: bigint, userId: string, upToMessageId: bigint): Promise<void> {
    const userJsonArray = JSON.stringify([userId]);

    await getDb()
      .update(messagesTable)
      .set({
        status: 'READ',
        readBy: sql`COALESCE(${messagesTable.readBy}, '[]'::jsonb) || ${userJsonArray}::jsonb`,
      })
      .where(
        and(
          eq(messagesTable.chatId, chatId),
          // Обновляем только сообщения <= upToMessageId
          sql`${messagesTable.id} <= ${upToMessageId}`,
          // Не трогаем свои же сообщения
          sql`${messagesTable.senderId} != ${userId}`,
          // Обновляем только те, где userId еще не записан в read_by
          sql`NOT (COALESCE(${messagesTable.readBy}, '[]'::jsonb) @> ${userJsonArray}::jsonb)`
        )
      );
  },

  // MARK: softDeleteForUser
  /**
   * Мягкое удаление сообщения «только для себя»: добавляет UUID юзера в JSONB-массив deleted_by
   */
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
  /**
   * Физическое удаление сообщения («Удалить у всех»)
   */
  async deleteById(id: bigint): Promise<boolean> {
    const [deleted] = await getDb()
      .delete(messagesTable)
      .where(eq(messagesTable.id, id))
      .returning({ id: messagesTable.id });

    return Boolean(deleted);
  },
});
