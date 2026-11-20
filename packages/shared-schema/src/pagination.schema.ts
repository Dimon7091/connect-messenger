import { z } from 'zod';

const DEFAULT_PAGE_SIZE = 10;
const MAX_PAGE_SIZE = 100;

export const BasePaginationQuerySchema = z.object({
  limit: z.coerce.number().int().min(1).max(MAX_PAGE_SIZE).default(DEFAULT_PAGE_SIZE),
  page: z.coerce.number().int().min(1).default(1),
  sortOrder: z.enum(['asc', 'desc']).default('desc'),
});

export const CursorPaginationQuerySchema = z.object({
  cursor: z.string().optional(), // opaque base64url
  limit: z.coerce.number().int().min(1).max(MAX_PAGE_SIZE).default(20),
});

export const paginationMetaSchema = z.object({
  total: z.number().int().nonnegative(),
  page: z.number().int().positive(),
  limit: z.number().int().positive(),
  totalPages: z.number().int().nonnegative(),
  hasNextPage: z.boolean(),
  hasPrevPage: z.boolean(),
});

// Фабрика схемы для конкретной сущности
export function createOffsetPaginationQuerySchema<T extends readonly [string, ...string[]]>(
  sortableFields: T,
  defaultSortBy?: T[number]
) {
  return BasePaginationQuerySchema.extend({
    sortBy: defaultSortBy
      ? z.enum(sortableFields).default(defaultSortBy)
      : z.enum(sortableFields).optional(),
  });
}

// Фабрика схемы ответа для конкретной сущности
export function createPaginatedResponseSchema<T extends z.ZodType>(itemSchema: T) {
  return z.object({
    items: z.array(itemSchema),
    meta: paginationMetaSchema,
  });
}

// Универсальный TS-тип ответа
export interface PaginatedResponse<T> {
  items: T[];
  meta: PaginationMeta;
}

export type PaginationMeta = z.infer<typeof paginationMetaSchema>;
export type OffsetPaginationQuery = z.infer<typeof BasePaginationQuerySchema>;
