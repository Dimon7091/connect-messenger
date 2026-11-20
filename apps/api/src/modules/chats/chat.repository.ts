// repositories/chat.repository.ts
import {
  eq,
  and,
  ne,
  sql,
  type DbInstance,
  schema,
  type Chat,
  type NewChat,
  type ChatWithParticipants,
  type ChatParticipant,
  type NewChatParticipant,
  inArray,
  desc,
} from '@messenger/database';

const chatsTable = schema.chatsTable;
const chatParticipantsTable = schema.chatParticipantsTable;

// Тип поддерживает как основной инстанс БД, так и объект транзакции (tx)
type DbOrTx = Pick<DbInstance, 'select' | 'insert' | 'update' | 'delete' | 'query'>;

export interface ChatRepository {
  // ---- Чаты ----
  findById(id: bigint): Promise<Chat | undefined>;
  findByIdWithParticipants(id: bigint): Promise<ChatWithParticipants | undefined>;
  findByDirectKey(directKey: string): Promise<Chat | undefined>;
  findUserChatsWithParticipants(userId: string): Promise<ChatWithParticipants[]>;
  create(data: NewChat): Promise<Chat>;
  createDirectIfNotExists(data: NewChat): Promise<Chat | undefined>;
  updateLastMessage(id: bigint, lastMessage: string): Promise<Chat | undefined>;
  deleteById(id: bigint): Promise<boolean>;

  // ---- Участники ----
  findByChatAndUser(chatId: bigint, userId: string): Promise<ChatParticipant | undefined>;
  findAllByChatId(chatId: bigint): Promise<ChatParticipant[]>;
  findActiveByUserId(userId: string): Promise<ChatParticipant[]>;
  addParticipants(data: NewChatParticipant[]): Promise<NewChatParticipant[]>;
  exists(chatId: bigint, userId: string): Promise<boolean>;
  createMany(data: NewChatParticipant[]): Promise<ChatParticipant[]>;
  incrementUnreadForOthers(chatId: bigint, senderId: string): Promise<void>;
  markAsRead(
    chatId: bigint,
    userId: string,
    lastReadMessageId: bigint
  ): Promise<ChatParticipant | undefined>;
  updateSettings(
    chatId: bigint,
    userId: string,
    settings: { isMuted?: boolean; pinnedAt?: Date | null }
  ): Promise<ChatParticipant | undefined>;
  softDelete(chatId: bigint, userId: string): Promise<ChatParticipant | undefined>;
}

export const createChatRepository = (db: DbOrTx): ChatRepository => ({
  // ========== ЧАТЫ ==========

  // MARK: findById
  async findById(id: bigint): Promise<Chat | undefined> {
    const [chat] = await db.select().from(chatsTable).where(eq(chatsTable.id, id)).limit(1);
    return chat;
  },

  // MARK: findByIdWithParticipants
  async findByIdWithParticipants(id: bigint) {
    return db.query.chatsTable.findFirst({
      where: eq(chatsTable.id, id),
      with: {
        participants: true,
      },
    });
  },

  // MARK: findByDirectKey
  async findByDirectKey(directKey: string) {
    const [chat] = await db
      .select()
      .from(chatsTable)
      .where(eq(chatsTable.directKey, directKey))
      .limit(1);
    return chat;
  },

  // MARK: findUserChats
  async findUserChatsWithParticipants(userId: string): Promise<ChatWithParticipants[]> {
    // Подзапрос: ID всех активных чатов пользователя (не делает отдельный запрос в БД, встраивается в основной SQL)
    const userChatIdsSubquery = db
      .select({ chatId: chatParticipantsTable.chatId })
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.userId, userId), eq(chatParticipantsTable.isDeleted, false))
      );

    // Основной запрос от таблицы chatsTable — вернет массив чатов с вложенным массивом participants
    const userChats = await db.query.chatsTable.findMany({
      where: inArray(chatsTable.id, userChatIdsSubquery),
      with: {
        participants: true, // Вложит массив ChatParticipant[] внутрь каждого чата
      },
    });

    return userChats;
  },

  // MARK: create
  async create(data: NewChat): Promise<Chat> {
    const [created] = await db.insert(chatsTable).values(data).returning();
    if (!created) throw new Error('INSERT did not return a chat row');
    return created;
  },

  // MARK: createDirectIfNotExists
  async createDirectIfNotExists(data: NewChat) {
    const [created] = await db
      .insert(chatsTable)
      .values(data)
      .onConflictDoNothing({ target: chatsTable.directKey })
      .returning();
    return created;
  },

  // MARK: updateLastMessage
  async updateLastMessage(id: bigint, lastMessage: string) {
    const [updated] = await db
      .update(chatsTable)
      .set({ lastMessage })
      .where(eq(chatsTable.id, id))
      .returning();
    return updated;
  },

  // MARK: deleteById
  async deleteById(id: bigint): Promise<boolean> {
    const [deleted] = await db
      .delete(chatsTable)
      .where(eq(chatsTable.id, id))
      .returning({ id: chatsTable.id });
    return Boolean(deleted);
  },

  /*
   MARK: - УЧАСТНИКИ
  *
  *
  */
  // ========== УЧАСТНИКИ ==========

  // MARK: findByChatAndUser
  async findByChatAndUser(chatId: bigint, userId: string) {
    const [participant] = await db
      .select()
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .limit(1);
    return participant;
  },

  // MARK: findAllByChatId
  async findAllByChatId(chatId: bigint) {
    return db.select().from(chatParticipantsTable).where(eq(chatParticipantsTable.chatId, chatId));
  },

  // MARK: findActiveByUserId
  async findActiveByUserId(userId: string) {
    return db
      .select()
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.userId, userId), eq(chatParticipantsTable.isDeleted, false))
      );
  },

  async addParticipants(data: NewChatParticipant[]) {
    if (data.length === 0) return [];
    return db.insert(chatParticipantsTable).values(data).onConflictDoNothing().returning();
  },

  // MARK: exists
  async exists(chatId: bigint, userId: string) {
    const [row] = await db
      .select({ chatId: chatParticipantsTable.chatId })
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .limit(1);
    return Boolean(row);
  },

  // MARK: createMany
  async createMany(data: NewChatParticipant[]) {
    if (data.length === 0) return [];
    return db.insert(chatParticipantsTable).values(data).onConflictDoNothing().returning();
  },

  // MARK: incrementUnreadForOthers
  async incrementUnreadForOthers(chatId: bigint, senderId: string) {
    await db
      .update(chatParticipantsTable)
      .set({
        unreadCount: sql`${chatParticipantsTable.unreadCount} + 1`,
        isDeleted: false,
        deletedAt: null,
        isChatEmpty: false,
      })
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), ne(chatParticipantsTable.userId, senderId))
      );
  },

  // MARK: markAsRead
  async markAsRead(chatId: bigint, userId: string, lastReadMessageId: bigint) {
    const [updated] = await db
      .update(chatParticipantsTable)
      .set({
        unreadCount: 0,
        lastReadMessageId,
        lastReadAt: new Date(),
      })
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .returning();
    return updated;
  },

  // MARK: updateSettings
  async updateSettings(
    chatId: bigint,
    userId: string,
    settings: { isMuted?: boolean; pinnedAt?: Date | null }
  ) {
    const [updated] = await db
      .update(chatParticipantsTable)
      .set(settings)
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .returning();
    return updated;
  },

  // MARK: softDelete
  async softDelete(chatId: bigint, userId: string) {
    const [updated] = await db
      .update(chatParticipantsTable)
      .set({
        isDeleted: true,
        deletedAt: new Date(),
        unreadCount: 0,
        pinnedAt: null,
      })
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .returning();
    return updated;
  },
});
