import {
  usersTable,
  type DbInstance,
  type NewChat,
  type NewUser,
  type RowUser,
} from '@messenger/database';
import type { HashService } from '../plugins/hash.plugin.js';
import { createHmac, randomBytes } from 'node:crypto';
import type { InviteService } from '../modules/invite/index.js';
import { invitationsTable } from '@messenger/database';
import { chatsTable } from '../../../../packages/database/src/schema/chats.table.js';

export function signFakeJwt(payload: object, secret: string): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url');

  const signature = createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url');

  return `${header}.${body}.${signature}`;
}

// MARK: - Create user in db
export async function createUserInDb(
  db: DbInstance,
  hashService: HashService,
  userName: string,
  firstName: string,
  password: string,
  role?: 'admin' | 'user'
) {
  const passwordHash = hashService.hash(password);
  const testUser: NewUser = {
    userName: userName,
    firstName: firstName,
    role: role ?? 'user',
    passwordHash: passwordHash,
  };

  const [savedUser] = await db.insert(usersTable).values(testUser).returning();
  if (!savedUser) throw new Error('INSERT did not return a user row');
  return savedUser;
}

// MARK: - Get user with db
export async function getUserWithDb(db: DbInstance, id: string) {
  const result = await db.query.usersTable.findFirst({
    where: (users, { eq }) => eq(users.id, id),
  });
  return (result as RowUser) ?? null;
}

export async function createInvitationInDb(db: DbInstance) {
  const data = {
    token: randomBytes(24).toString('base64url'),
  };

  const [invitation] = await db.insert(invitationsTable).values(data).returning();
  return invitation!;
}

export async function countInvitationsInDb(db: DbInstance) {
  const data = {
    token: randomBytes(24).toString('base64url'),
  };

  const invitations = await db.$count(invitationsTable);
  return invitations;
}
