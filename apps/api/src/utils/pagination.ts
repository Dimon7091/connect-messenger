import type { OffsetPaginationQuery, PaginatedResponse } from '@messenger/shared-schema';

export function getPaginationParams({ page, limit }: OffsetPaginationQuery) {
  return {
    limit,
    offset: (page - 1) * limit,
  };
}

export function createPaginatedResponse<T>(
  items: T[],
  total: number,
  { page, limit }: OffsetPaginationQuery
): PaginatedResponse<T> {
  const totalPages = Math.ceil(total / limit);

  return {
    items,
    meta: {
      total,
      page,
      limit,
      totalPages,
      hasNextPage: page < totalPages,
      hasPrevPage: page > 1,
    },
  };
}

import { asc, desc, type AnyColumn } from '@messenger/database';

export function buildOrderBy<TColumns extends Record<string, AnyColumn>>(
  columnsMap: TColumns,
  sortBy: keyof TColumns | undefined,
  sortOrder: 'asc' | 'desc',
  fallbackColumn: AnyColumn
) {
  const direction = sortOrder === 'asc' ? asc : desc;
  const targetColumn = sortBy ? columnsMap[sortBy] : fallbackColumn;

  // Возвращаем массив для передачи в .orderBy(...orderBy)
  // Вторым параметром всегда идет fallbackColumn (обычно id), чтобы строки не "прыгали"
  return targetColumn === fallbackColumn
    ? [direction(fallbackColumn)]
    : [direction(targetColumn), direction(fallbackColumn)];
}
