import z from 'zod';

// Базовая схема UUID
export const UuidSchema = z.uuid('Некорректный формат UUID');
