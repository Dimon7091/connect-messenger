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
} from '@messenger/database';

const chatsTable = schema.chatsTable;
const chatParticipantsTable = schema.chatParticipantsTable;

type DbOrTx = Pick<DbInstance, 'select' | 'insert' | 'update' | 'delete' | 'query'>;

export interface ChatRepository {
  // ---- Чаты (всегда возвращают с участниками) ----
  findById(id: bigint): Promise<ChatWithParticipants | undefined>;
  findByDirectKey(directKey: string): Promise<ChatWithParticipants | undefined>;
  findUserChats(userId: string): Promise<ChatWithParticipants[]>;
  create(data: NewChat): Promise<Chat>;
  createDirectIfNotExists(data: NewChat): Promise<Chat | undefined>;
  updateLastMessage(id: bigint, lastMessage: string): Promise<Chat | undefined>;
  deleteById(id: bigint): Promise<boolean>;

  // ---- Участники ----
  findByChatAndUser(chatId: bigint, userId: string): Promise<ChatParticipant | undefined>;
  findAllByChatId(chatId: bigint): Promise<ChatParticipant[]>;
  findActiveByUserId(userId: string): Promise<ChatParticipant[]>;
  addParticipants(data: NewChatParticipant[]): Promise<ChatParticipant[]>;
  exists(chatId: bigint, userId: string): Promise<boolean>;
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

export const createChatRepository = (getDb: () => DbOrTx): ChatRepository => ({
  // ========== ЧАТЫ ==========

  // MARK: findById
  async findById(id: bigint): Promise<ChatWithParticipants | undefined> {
    return getDb().query.chatsTable.findFirst({
      where: eq(chatsTable.id, id),
      with: {
        participants: true,
      },
    });
  },

  // MARK: findByDirectKey
  async findByDirectKey(directKey: string): Promise<ChatWithParticipants | undefined> {
    return getDb().query.chatsTable.findFirst({
      where: eq(chatsTable.directKey, directKey),
      with: {
        participants: true,
      },
    });
  },

  // MARK: findUserChats
  async findUserChats(userId: string): Promise<ChatWithParticipants[]> {
    const db = getDb();

    const userChatIdsSubquery = db
      .select({ chatId: chatParticipantsTable.chatId })
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.userId, userId), eq(chatParticipantsTable.isDeleted, false))
      );

    return db.query.chatsTable.findMany({
      where: inArray(chatsTable.id, userChatIdsSubquery),
      with: {
        participants: true,
      },
    });
  },

  // MARK: create
  async create(data: NewChat): Promise<Chat> {
    const [created] = await getDb().insert(chatsTable).values(data).returning();
    if (!created) throw new Error('INSERT did not return a chat row');
    return created;
  },

  // MARK: createDirectIfNotExists
  async createDirectIfNotExists(data: NewChat) {
    const [created] = await getDb()
      .insert(chatsTable)
      .values(data)
      .onConflictDoNothing({ target: chatsTable.directKey })
      .returning();
    return created;
  },

  // MARK: updateLastMessage
  async updateLastMessage(id: bigint, lastMessage: string) {
    const [updated] = await getDb()
      .update(chatsTable)
      .set({ lastMessage })
      .where(eq(chatsTable.id, id))
      .returning();
    return updated;
  },

  // MARK: deleteById
  async deleteById(id: bigint): Promise<boolean> {
    const [deleted] = await getDb()
      .delete(chatsTable)
      .where(eq(chatsTable.id, id))
      .returning({ id: chatsTable.id });
    return Boolean(deleted);
  },

  // ========== УЧАСТНИКИ ==========

  // MARK: findByChatAndUser
  async findByChatAndUser(chatId: bigint, userId: string) {
    const [participant] = await getDb()
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
    return getDb()
      .select()
      .from(chatParticipantsTable)
      .where(eq(chatParticipantsTable.chatId, chatId));
  },

  // MARK: findActiveByUserId
  async findActiveByUserId(userId: string) {
    return getDb()
      .select()
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.userId, userId), eq(chatParticipantsTable.isDeleted, false))
      );
  },

  // MARK: addParticipants
  async addParticipants(data: NewChatParticipant[]) {
    if (data.length === 0) return [];
    return getDb().insert(chatParticipantsTable).values(data).onConflictDoNothing().returning();
  },

  // MARK: exists
  async exists(chatId: bigint, userId: string) {
    const [row] = await getDb()
      .select({ chatId: chatParticipantsTable.chatId })
      .from(chatParticipantsTable)
      .where(
        and(eq(chatParticipantsTable.chatId, chatId), eq(chatParticipantsTable.userId, userId))
      )
      .limit(1);
    return Boolean(row);
  },

  // MARK: incrementUnreadForOthers
  async incrementUnreadForOthers(chatId: bigint, senderId: string) {
    await getDb()
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
    const [updated] = await getDb()
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
    const [updated] = await getDb()
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
    const [updated] = await getDb()
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
