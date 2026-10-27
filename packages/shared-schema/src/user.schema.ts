import { z } from "zod";

// --- Регулярные выражения (перенос из Java-аннотаций) ---
const USERNAME_CHARS = /^[a-zA-Z0-9._-]+$/;
const USERNAME_NOT_ONLY_DIGITS_OR_DOTS = /^(?!^[0-9]+$)(?!^\.+$).*$/;
const FIRST_NAME = /^[a-zA-Zа-яА-ЯёЁ]+$/;
const LAST_NAME = /^[a-zA-Zа-яА-ЯёЁ]*$/;

// ===== Api =====
export const CreateUserSchema = z.object({
  userName: z
    .string()
    .min(3, "Имя пользователя должно быть не менее 3 символа")
    .max(15, "Имя пользователя не может превышать 15 символов")
    .regex(
      USERNAME_CHARS,
      "Имя пользователя может содержать только английские буквы, цифры, дефис, подчеркивание, точку",
    )
    .regex(
      USERNAME_NOT_ONLY_DIGITS_OR_DOTS,
      "Имя пользователя не может состоять только из цифр или только из точек",
    ),
  firstName: z
    .string()
    .min(2, "Имя должно быть не менее 2 символа")
    .max(20, "Имя не может превышать 20 символов")
    .regex(
      FIRST_NAME,
      "Имя может содержать только буквы русского или английского языка без пробелов и знаков препинания",
    ),
  lastName: z
    .string()
    .min(2, "Фамилия должна быть не менее 2 символа")
    .max(25, "Фамилия не может превышать 25 символов")
    .regex(LAST_NAME, "Фамилия может содержать только буквы русского или английского языка")
    .nullable(),
  password: z
    .string()
    .min(8, "Пароль должен быть не менее 8 символов")
    .max(20, "Пароль не может превышать 20 символов")
    .refine((v) => v.trim().length > 0, { message: "Пароль не может быть пустым" }),
  invitationToken: z.string({ error: "Отсутствует действующее приглашение" }),
});

export const UpdateUserSchema = z.object({
  userName: z.string().optional(),
  profile: z
    .object({
      firstName: z.string().optional(),
      lastName: z.string().optional(),
    })
    .optional(),
});

export const UserBaseResponseSchema = z.object({
  id: z.string(),
  userName: z.string(),
  profile: z.object({
    firstName: z.string(),
    lastName: z.string().nullable(),
    avatarUrl: z.string().nullable(),
    avatarThumbUrl: z.string().nullable(),
  }),
  isDeleted: z.boolean(),
  isBanned: z.boolean(),
});

export const UserPrivateResponseSchema = UserBaseResponseSchema.extend({
  createdAt: z.coerce.date(),
  blackListIds: z.array(z.string()),
});

export const UserAdminResponseSchema = UserBaseResponseSchema.extend({
  roles: z.array(z.string()),
  lastSeen: z.coerce.date(),
  status: z.string(),
});

export const UserToBlacklistSchema = z.object({
  id: z.string(),
});

export const UserBlockedResponseSchema = z.object({
  blockedUserId: z.string(),
});

// ===== Websocket =====
export const UserStatusResponseSchema = z.object({
  userId: z.string(),
  status: z.enum(["ONLINE", "OFFLINE", "AWAY"]),
  lastSeen: z.string().nullable().optional(),
});

export type MakeOptional<T, K extends keyof T> = Omit<T, K> & Partial<Pick<T, K>>;
export type CreateUserDto = z.infer<typeof CreateUserSchema>;
export type UserToBlacklistDto = z.infer<typeof UserToBlacklistSchema>;
export type UserBlockedResponse = z.infer<typeof UserBlockedResponseSchema>;
export type UserBaseResponse = z.infer<typeof UserBaseResponseSchema>;
export type UserPrivateResponse = z.infer<typeof UserPrivateResponseSchema>;
export type UserAdminResponse = z.infer<typeof UserAdminResponseSchema>;

// ===== Types for Web-client =====
export type User = UserBaseResponse & Partial<Omit<UserPrivateResponse, keyof UserBaseResponse>>;
export type UserStatus = z.infer<typeof UserStatusResponseSchema>;
export type UsersGettingByIdsRequest = {
  usersIds: string[];
};
