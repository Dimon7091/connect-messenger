import { sql, type DbInstance } from '@messenger/database';

export async function resetDb(db: DbInstance): Promise<void> {
  await db.execute(sql`
    DO $$
    DECLARE
      tables text;
    BEGIN
      SELECT string_agg(quote_ident(tablename), ', ')
      INTO tables
      FROM pg_tables
      WHERE schemaname = 'public'
        AND tablename NOT LIKE '__drizzle%';

      IF tables IS NOT NULL THEN
        EXECUTE 'TRUNCATE TABLE ' || tables || ' RESTART IDENTITY CASCADE';
      END IF;
    END $$;
  `);
}
