export * as schema from './schema/index.js';
export * from 'drizzle-orm';
export { createDb } from './client.js';
export type { DbOptions, DbInstance } from './client.js';
export { usersTable, usersProfilesTable } from './schema/index.js';
export type { User, NewUser, UserRole} from './schema/index.js';
export { runMigrations } from './migrate.js';
