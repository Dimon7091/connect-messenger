import { drizzle as drizzlePg, type NodePgDatabase } from 'drizzle-orm/node-postgres'
import { drizzle as drizzlePglite, type PgliteDatabase } from 'drizzle-orm/pglite'
import { PGlite } from '@electric-sql/pglite'
import { Pool } from 'pg'
import * as schema from './schema'

export type Db = NodePgDatabase<typeof schema> | PgliteDatabase<typeof schema>

export type DbConfig =
  | { driver: 'pglite'; dataDir?: string }
  | { driver: 'postgres'; connectionString: string; max?: number }

export async function createDb(config: DbConfig): Promise<{ db: Db; close: () => Promise<void> }> {
  if (config.driver === 'pglite') {
    const client = await PGlite.create(config.dataDir ?? 'memory://')
    const db = drizzlePglite(client, { schema })
    return { db, close: () => client.close() }
  }

  const pool = new Pool({ connectionString: config.connectionString, max: config.max })
  const db = drizzlePg(pool, { schema })
  return { db, close: () => pool.end() }
}
