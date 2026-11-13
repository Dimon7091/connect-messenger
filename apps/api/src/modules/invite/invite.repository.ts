import {
  eq,
  count,
  schema,
  type DbInstance,
  type Invitation,
  type NewInvitation,
} from '@messenger/database';

export interface InviteRepository {
  create(data: NewInvitation): Promise<Invitation>;
  findByToken(token: string): Promise<Invitation | null>;
  activeCount(): Promise<number>;
  deleteByToken(token: string): Promise<void>;
}

const invitationsTable = schema.invitationsTable;

export function createInviteRepository(db: DbInstance): InviteRepository {
  return {
    async create(data) {
      const [invitation] = await db.insert(invitationsTable).values(data).returning();
      return invitation!;
    },

    async findByToken(token) {
      const [invitation] = await db
        .select()
        .from(schema.invitationsTable)
        .where(eq(schema.invitationsTable.token, token));
      return invitation ?? null;
    },

    async activeCount() {
      return await db.$count(invitationsTable);
    },

    async deleteByToken(token) {
      await db.delete(invitationsTable).where(eq(invitationsTable.token, token));
    },
  };
}
