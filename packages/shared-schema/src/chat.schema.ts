import z from 'zod';

export const CreateOrGetChatSchema = z.object({
  companionId: z.uuid(),
});

const BigIntToString = z.union([z.bigint(), z.string()]).transform(val => val.toString());

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
export type ChatResponse = z.infer<typeof ChatResponseSchema>;
