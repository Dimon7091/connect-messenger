import { string, z } from 'zod';
import { OptionalTsidSchema, TsidInputSchema, TsidOutputSchema } from './tsid.schema.js';
import { UuidSchema } from './uuid.schema.js';

export const MessageAttachmentSchema = z.object({
  fileKey: z.string().min(1),
  mimeType: z.string().min(1),
  fileName: z.string().min(1),
  size: z.string().min(1),
});

export const SendMessageRequestSchema = z.object({
  chatId: TsidInputSchema,
  isFistMessage: z.boolean().nullish(),
  messageId: z.string().nullish(), // временный ID от клиента
  senderId: z.uuid(),
  receiverId: z.string(),
  text: z.string().nullish(),
  replyToId: OptionalTsidSchema,
  timestamp: z.string().nullish(),
  attachments: z.array(MessageAttachmentSchema).nullish(),
});

export const MessageResponseSchema = z.object({
  id: z.string(),
  chatId: z.string(),
  createdAt: z.coerce.date(),
  senderId: z.string(),
  receiverId: z.string(),
  text: z.string().nullish(),
  timestamp: z.coerce.date(),
  status: z.enum(['SENT', 'DELIVERED', 'READ', 'FAILED']),
  replyToId: z.string().nullish(),
  attachments: z.array(MessageAttachmentSchema).nullish(),
  deletedBy: z.array(z.string()),
  readBy: z.array(z.string()).nullish(),
});

export type MessageAttachment = z.infer<typeof MessageAttachmentSchema>;
export type MessageResponse = z.infer<typeof MessageResponseSchema>;
export type SendMessageRequest = z.infer<typeof SendMessageRequestSchema>;
