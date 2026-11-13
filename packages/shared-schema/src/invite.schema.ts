import { z } from 'zod';

export const InvitationResponseSchema = z.object({
  invitationUrl: z.string(),
});

export const InvitationsCountResponseSchema = z.object({
  activeInvitationsCount: z.number(),
});

export type InvitationResponse = z.infer<typeof InvitationResponseSchema>;
export type InvitationCountResponse = z.infer<typeof InvitationsCountResponseSchema>;
