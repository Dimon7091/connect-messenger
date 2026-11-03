import { PGlite } from '@electric-sql/pglite';
import { drizzle as drizzlePglite } from 'drizzle-orm/pglite';
import postgres from 'postgres';
import { drizzle as drizzlePostgres } from 'drizzle-orm/postgres-js';
import * as schema from './schema/index.js';
import { PgDatabase } from 'drizzle-orm/pg-core';

export type DbInstance = PgDatabase<any, any, any>;

export interface DbOptions {
  driver: 'pglite' | 'postgres';
  dataDir?: string;
  connectionString?: string;
  maxPool?: number;
}
export async function createDb(options: DbOptions) {
  if (options.driver === 'pglite') {
    const client = new PGlite(options.dataDir ?? './.pgdata');
    const db = drizzlePglite(client, { schema });

    return {
      db,
      close: () => client.close(),
    };
  }

  const client = postgres(options.connectionString!, { max: options.maxPool ?? 5 });
  const db = drizzlePostgres(client, { schema });

  return {
    db,
    close: () => client.end(),
  };
}
