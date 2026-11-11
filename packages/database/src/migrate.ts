import { migrate } from 'drizzle-orm/node-postgres/migrator';
import * as path from 'path';
import { createRequire } from 'module';
import type { DbInstance } from './client.js';

const require = createRequire(import.meta.url);

const databasePackageRoot = path.dirname(require.resolve('@messenger/database/package.json'));
const defaultFolder = path.resolve(databasePackageRoot, './drizzle');

export async function runMigrations(db: DbInstance, migrationsFolder = defaultFolder) {
  try {
    console.log('🔄 Running migrations... from:', migrationsFolder);
    await migrate(db as any, { migrationsFolder });
    console.log('✅ Migrations applied');
  } catch (error) {
    console.error('❌ Migrations error:', error);
    process.exit(1);
  }
}
