import {
  usersProfilesTable,
  usersTable,
  type DbInstance,
  type NewUser,
  type User,
} from '@messenger/database';
import type { HashService } from '../plugins/hash.plugin.js';
import { createHmac } from 'node:crypto';

export function signFakeJwt(payload: object, secret: string): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url');

  const signature = createHmac('sha256', secret).update(`${header}.${body}`).digest('base64url');

  return `${header}.${body}.${signature}`;
}

export async function createUserInDb(
  db: DbInstance,
  hashService: HashService,
  userName: string,
  firstName: string,
  password: string,
  role?: 'admin' | 'user'
) {
  const passwordHash = hashService.hash(password);
  const newUser: NewUser = {
    userName: userName,
    profile: {
      firstName: firstName,
    },
    role: role ?? 'user',
    passwordHash: passwordHash,
  };

  return await db.transaction(async tx => {
    const { profile, ...userData } = newUser;

    // Вставка пользователя
    const [savedUser] = await tx.insert(usersTable).values(userData).returning();
    if (!savedUser) throw new Error('INSERT did not return a user row');

    // Вставка профиля (если он передан)
    let profileWithoutId = null;
    if (profile) {
      const [savedProfile] = await tx
        .insert(usersProfilesTable)
        .values({ ...profile, userId: savedUser.id })
        .returning();

      if (!savedProfile) throw new Error('INSERT did not return a profile row');

      const { userId, ...restProfile } = savedProfile;
      profileWithoutId = restProfile;
    }

    // Возврат объединенного объекта
    return {
      ...savedUser,
      profile: profileWithoutId,
    };
  });
}

export async function getUserWithDb(db: DbInstance, id: string) {
  const result = await db.query.usersTable.findFirst({
    where: (users, { eq }) => eq(users.id, id),
    with: {
      profile: {
        columns: {
          userId: false,
        },
      },
    },
  });
  return result as User | null;
}
