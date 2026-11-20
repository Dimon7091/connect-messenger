import type { ChatRepository } from '../modules/chats/chat.repository.js';

export interface TxRepositories {
  chatRepo: ChatRepository;
}

export interface TransactionManager {
  runInTransaction<T>(work: (repos: TxRepositories) => Promise<T>): Promise<T>;
}
