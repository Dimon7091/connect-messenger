import closeWithGrace from 'close-with-grace';
import { buildApp } from './app.js';

async function init() {
  const app = await buildApp();

  closeWithGrace(
    { delay: Number(app.config.FASTIFY_CLOSE_GRACE_DELAY ?? 500) },
    async ({ err }) => {
      if (err != null) app.log.error(err);
      await app.close();
    }
  );

  await app.ready();

  try {
    await app.listen({ port: app.config.API_PORT, host: app.config.API_HOST });
    console.log(`🚀 Server running, mode: ${app.config.NODE_ENV}!`);
  } catch (err) {
    app.log.error(err);
    process.exit(1);
  }
}

init();
