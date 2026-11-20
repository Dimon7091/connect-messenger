import { AsyncLocalStorage } from 'node:async_hooks';
import type { DbInstance } from '@messenger/database';

type DbTransaction = Parameters<Parameters<DbInstance['transaction']>[0]>[0];
export type DbOrTx = DbInstance | DbTransaction;

export interface TransactionManager {
  runInTransaction<T>(work: () => Promise<T>): Promise<T>;
}

const txStorage = new AsyncLocalStorage<DbTransaction>();

export const createTxManager = (db: DbInstance) => ({
  getDb: (): DbOrTx => txStorage.getStore() ?? db,

  async runInTransaction<T>(work: () => Promise<T>): Promise<T> {
    if (txStorage.getStore()) return work(); // Если уже в транзакции — продолжаем её
    return db.transaction(async tx => txStorage.run(tx, work));
  },
});
