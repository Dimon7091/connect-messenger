import { z } from 'zod';
import { OptionalTsidSchema, TsidInputSchema, TsidOutputSchema } from './tsid.schema.js';
import { UuidSchema } from './uuid.schema.js';
import { ChatResponseSchema } from './chat.schema.js';

export const MessageStatusSchema = z.enum(['SENT', 'FAILED', 'DELIVERED', 'READ']);

export const MessageAttachmentSchema = z.object({
  fileKey: z.string().min(1),
  mimeType: z.string().min(1),
  fileName: z.string().min(1),
  size: z.number(),
});

export const ReplyContextSchema = z.object({
  originalMessageId: z.string(),
  senderName: z.string(),
  textSnippet: z.string(),
});

export const SendMessageRequestSchema = z.object({
  chatId: TsidInputSchema,
  isFirstMessage: z.boolean().nullish(),
  messageId: z.string().nullish(), // временный ID от клиента
  senderId: UuidSchema,
  receiverId: z.string(),
  text: z.string().nullish(),
  replyToId: OptionalTsidSchema,
  timestamp: z.string().nullish(),
  attachments: z.array(MessageAttachmentSchema).nullish(),
});

export const MessagesDeletedRequestSchema = z.object({
  chatId: TsidInputSchema,
  messagesIds: z.array(TsidInputSchema),
});

export const MessageDeletedResponseSchema = z.object({
  chatId: TsidOutputSchema,
  deletedMessagesIds: z.array(TsidOutputSchema),
  chatUpdatedAt: z.coerce.date(),
  unreadCount: z.number().int().nullable(),
  lastMessage: z.string().nullable(),
});

export const MessageResponseSchema = z.object({
  id: z.string(),
  chatId: z.string(),
  createdAt: z.string(),
  senderId: UuidSchema,
  receiverId: UuidSchema,
  text: z.string().nullish(),
  timestamp: z.string(),
  status: MessageStatusSchema,
  replyToId: z.string().nullish(),
  attachments: z.array(MessageAttachmentSchema).nullish(),
  deletedBy: z.array(UuidSchema),
  readBy: z.array(UuidSchema).nullish(),
  replyContext: ReplyContextSchema.nullish(),
});

export const MessageNewResponseSchema = z.object({
  chat: ChatResponseSchema,
  message: MessageResponseSchema,
});

export const MessageSentResponseSchema = z.object({
  messageId: z.string(),
  serverMessageId: TsidOutputSchema,
  status: MessageStatusSchema,
  timestamp: z.string(),
});

export const MessageDeliveredRequestSchema = z.object({
  messageId: TsidInputSchema,
  chatId: TsidInputSchema,
  userId: UuidSchema,
});

export const MessageDeliveredResponseSchema = z.object({
  messageId: z.string(),
  chatId: z.string(),
});

export const MessageReadRequestSchema = z.object({
  messageId: TsidInputSchema,
  chatId: TsidInputSchema,
  userId: UuidSchema,
});

export const MessageReadResponseSchema = z.object({
  messageId: z.string(),
  chatId: z.string(),
});

export const MarkMessageAsDeliveredRequestSchema = z.object({
  messageId: TsidInputSchema,
  chatId: TsidInputSchema,
});

export const MarkMessageAsReadRequestSchema = z.object({
  messageId: TsidInputSchema,
  chatId: TsidInputSchema,
});

export const AllMessagesReadRequestSchema = z.object({
  chatId: TsidInputSchema,
});

export const AllMessagesReadResponseSchema = z.object({
  chatId: z.string(),
});

export const TypingRequestSchema = z.object({
  chatId: TsidInputSchema,
  receiverId: UuidSchema,
});

export const TypingResponseSchema = z.object({
  chatId: z.string(),
  targetUserId: z.string(),
});

export type ReplyContext = z.infer<typeof ReplyContextSchema>;
export type TypingRequest = z.input<typeof TypingRequestSchema>;
export type TypingResponse = z.infer<typeof TypingResponseSchema>;
export type MessageNewResponse = z.infer<typeof MessageNewResponseSchema>;
export type MessageStatus = z.infer<typeof MessageStatusSchema>;
export type MessageSentResponse = z.infer<typeof MessageSentResponseSchema>;
export type MessageAttachment = z.infer<typeof MessageAttachmentSchema>;
export type MessageResponse = z.infer<typeof MessageResponseSchema>;
export type MessageDeliveredRequest = z.input<typeof MessageDeliveredRequestSchema>;
export type MessageDeliveredResponse = z.infer<typeof MessageDeliveredResponseSchema>;
export type SendMessageRequest = z.input<typeof SendMessageRequestSchema>;
export type SendMessageDto = z.infer<typeof SendMessageRequestSchema>;
export type MessageReadRequest = z.input<typeof MessageReadRequestSchema>;
export type MessageReadResponse = z.infer<typeof MessageReadResponseSchema>;
export type MessagesDeletedRequest = z.infer<typeof MessagesDeletedRequestSchema>;
export type MessageDeletedResponse = z.infer<typeof MessageDeletedResponseSchema>;
export type AllMessagesReadRequest = z.input<typeof AllMessagesReadRequestSchema>;
export type AllMessagesReadResponse = z.infer<typeof AllMessagesReadResponseSchema>;
export type MarkMessageAsDeliveredRequest = z.infer<typeof MarkMessageAsDeliveredRequestSchema>;
export type MarkMessageAsReadRequest = z.infer<typeof MarkMessageAsReadRequestSchema>;
