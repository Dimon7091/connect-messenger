// src/plugins/errorHandler.ts
import fp from 'fastify-plugin';
import type { FastifyError } from 'fastify';
import {
  hasZodFastifySchemaValidationErrors,
  isResponseSerializationError,
} from 'fastify-type-provider-zod';

interface ErrorBody {
  code: string;
  message: string;
  details?: unknown;
}

export const errorHandlerPlugin = fp(async app => {
  app.setErrorHandler((err: FastifyError, request, reply) => {
    app.log.error(
      {
        err,
        request: {
          method: request.method,
          url: request.url,
          query: request.query,
          params: request.params,
        },
      },
      'Unhandled error occurred'
    );

    // Ошибки валидации запроса (Zod) — через провайдер
    if (hasZodFastifySchemaValidationErrors(err)) {
      return reply.code(400).send({
        code: 'VALIDATION_ERROR',
        message: 'Validation failed',
        details: err.validation.map(v => ({
          path: v.instancePath || v.schemaPath,
          message: v.message,
        })),
      } satisfies ErrorBody);
    }

    // Ошибки сериализации ответа (Zod) — ответ не соответствует схеме
    if (isResponseSerializationError(err)) {
      return reply.code(500).send({
        code: 'RESPONSE_SERIALIZATION_ERROR',
        message: 'Response does not match the schema',
        details: {
          method: err.method,
          url: err.url,
          cause: err.cause,
        },
      } satisfies ErrorBody);
    }

    // AppError с полем code
    if ('code' in err && typeof (err as any).code === 'string') {
      const status = err.statusCode ?? 500;
      return reply.code(status).send({
        code: (err as any).code,
        message: status < 500 ? err.message : 'Internal Server Error',
      } satisfies ErrorBody);
    }

    // Все остальные ошибки
    const status = err.statusCode ?? 500;
    return reply.code(status).send({
      code: status < 500 ? 'CLIENT_ERROR' : 'INTERNAL_ERROR',
      message: status < 500 ? err.message : 'Internal Server Error',
    } satisfies ErrorBody);
  });

  app.setNotFoundHandler(
    { preHandler: app.rateLimit({ max: 3, timeWindow: 500 }) },
    (request, reply) => {
      request.log.warn({ method: request.method, url: request.url }, 'Resource not found');
      return reply.code(404).send({
        code: 'NOT_FOUND',
        message: `Route ${request.method} ${request.url} not found`,
      });
    }
  );
});
