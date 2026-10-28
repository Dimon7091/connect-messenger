import { migrate } from 'drizzle-orm/node-postgres/migrator'
import * as path from 'path'
import type { Db } from './client'

const defaultFolder = path.resolve(__dirname, '../drizzle')

export async function runMigrations(db: Db, migrationsFolder = defaultFolder) {
  try {
    console.log('🔄 Running migrations...')
    await migrate(db as any, { migrationsFolder })
    console.log('✅ Migrations applied')
  } catch (error) {
    console.error('❌ Migrations error:', error)
    process.exit(1)
  }
}
