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
  limit: z.coerce.number().int(),
  beforeTimestamp: z.iso
    .datetime()
    .transform(val => new Date(val))
    .optional(),
});

export const ChatType = z.enum(['DIRECT', 'GROUP']);

export const ChatResponseSchema = z.object({
  id: z.string(),
  type: ChatType,
  participants: z.array(z.string()),
  unreadCount: z.number().int(),
  lastMessage: z.string().nullish(),
  createdAt: z.string(),
  updatedAt: z.string(),
  createdBy: UuidSchema,
});

export type CreateOrGetDto = z.infer<typeof CreateOrGetChatSchema>;
export type ChatHistoryQueryRequest = z.infer<typeof ChatHistoryQuerySchema>;
export type ChatResponse = z.infer<typeof ChatResponseSchema>;
