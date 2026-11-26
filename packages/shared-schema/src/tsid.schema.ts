import { z } from 'zod';

const MAX_BIGINT = 9223372036854775807n;

// Базовая валидация строки TSID (используется на границе сети/JSON)
const TsidStringSchema = z
  .string()
  .min(1)
  .max(19)
  .refine(
    val => {
      if (val.charCodeAt(0) < 48 || val.charCodeAt(0) > 57) return false;
      try {
        const id = BigInt(val);
        return id > 0n && id <= MAX_BIGINT;
      } catch {
        return false;
      }
    },
    { message: 'Invalid TSID' }
  );

// Кодек: в JSON/URL это string, в TypeScript/UseCase это bigint
export const TsidCodec = z.codec(TsidStringSchema, z.bigint(), {
  decode: val => BigInt(val),
  encode: val => val.toString(),
});

// Для входящих данных (params, query, body)
export const TsidInputSchema = TsidCodec;
export const OptionalTsidSchema = TsidCodec.nullish();

// Для исходящих данных (response) — принимает bigint или string из UseCase и отдаёт string в JSON
export const TsidOutputSchema = z.codec(TsidStringSchema, z.union([z.bigint(), z.string()]), {
  decode: val => val,
  encode: val => val.toString(),
});
