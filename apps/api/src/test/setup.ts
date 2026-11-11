import { buildApp, type AppOptions } from '../app.js';
import { runMigrations } from '@messenger/database';
import type { FastifyInstance } from 'fastify';

/**
 * Создает и подготавливает экземпляр приложения для тестов
 */
export async function createTestApp(options?: AppOptions): Promise<FastifyInstance> {
  // Защитная проверка для безопасности ваших данных
  if (process.env.NODE_ENV !== 'test') {
    throw new Error(
      `🚫 Запуск тестов заблокирован! NODE_ENV должен быть 'test', сейчас: '${process.env.NODE_ENV}'. ` +
        `Используйте команду 'pnpm test'.`
    );
  }

  return await buildApp(options);
}

export async function closeTestApp(app: FastifyInstance): Promise<void> {
  await app.close();
}
