import z from 'zod';
import { UuidSchema } from './uuid.schema.js';
import { TsidInputSchema } from './tsid.schema.js';

export const ChatIdParamSchema = z.object({
  chatId: TsidInputSchema,
});

export const CreateOrGetChatSchema = z.object({
  companionId: z.uuid(),
});

export const ChatHistoryQuerySchema = z.object({
  chatId: TsidInputSchema,
  userId: UuidSchema,
  limit: z.number().optional(),
  beforeTimestamp: z.date(),
});

export const ChatResponseSchema = z.object({
  id: z.string(),
  type: z.string(),
  participants: z.array(z.string()),
  unreadCount: z.number().int(),
  lastMessage: z.string().nullish(),
  createdAt: z.coerce.date(),
  updatedAt: z.coerce.date(),
  createdBy: z.uuid(),
});

export type CreateOrGetDto = z.infer<typeof CreateOrGetChatSchema>;
export type ChatHistoryQueryRequest = z.infer<typeof ChatHistoryQuerySchema>;
export type ChatResponse = z.infer<typeof ChatResponseSchema>;
