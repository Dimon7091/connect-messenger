import fp from 'fastify-plugin';
import type { FastifyInstance } from 'fastify';

async function seedAdmin(app: FastifyInstance) {
  const adminUserName = app.config.INIT_ADMIN_USERNAME.trim().toLowerCase();
  const adminFirstName = app.config.INIT_ADMIN_FIRSTNAME;
  const adminPassword = app.config.INIT_ADMIN_PASSWORD;

  const adminAlreadyExists = await app.userService.hasAdmin();

  if (adminAlreadyExists) {
    app.log.info(`Administrator is already exists`);
    return;
  }

  const passwordHash = app.hashService.hash(adminPassword);

  await app.userService.create({
    userName: adminUserName,
    profile: {
      firstName: adminFirstName,
    },
    passwordHash: passwordHash,
    role: 'admin',
  });

  app.log.info(`✅ Administrator: ${adminUserName}, successfully initialized`);
}

export const seedAdminPlugin = fp(seedAdmin, {
  name: 'seedAdminPlugin',
});
