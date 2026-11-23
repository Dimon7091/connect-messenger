import { z } from 'zod';

const MAX_BIGINT = 9223372036854775807n;

export const TsidInputSchema = z
  .string()
  .min(1)
  .max(19)
  .transform((val, ctx) => {
    // Быстрая проверка: первый символ не должен быть пробелом или знаком,
    // а BigInt() сам выбросит ошибку, если внутри есть нецифровые символы (кроме пробелов по краям)
    if (val.charCodeAt(0) < 48 || val.charCodeAt(0) > 57) {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Invalid TSID' });
      return z.NEVER;
    }

    try {
      const id = BigInt(val);
      if (id <= 0n || id > MAX_BIGINT) {
        ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'TSID out of range' });
        return z.NEVER;
      }
      return id;
    } catch {
      ctx.addIssue({ code: z.ZodIssueCode.custom, message: 'Invalid TSID' });
      return z.NEVER;
    }
  });

export const TsidOutputSchema = z.union([z.bigint(), z.string()]).transform(val => val.toString());

export const OptionalTsidSchema = TsidInputSchema.nullish();
