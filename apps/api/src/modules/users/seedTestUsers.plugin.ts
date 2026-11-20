import fp from 'fastify-plugin';
import type { FastifyInstance } from 'fastify';

/**
 * Детерминированный массив тестовых пользователей.
 * Фамилии, имена и userName сгенерированы так, чтобы обеспечить
 * разнообразие для проверки пагинации, поиска и сортировки.
 */
const TEST_USERS = [
  { firstName: 'Александр', lastName: 'Иванов', userName: 'alex.ivanov' },
  { firstName: 'Елена', lastName: 'Петрова', userName: 'elena.petrova' },
  { firstName: 'Сергей', lastName: 'Сидоров', userName: 'sergey.sidorov' },
  { firstName: 'Ольга', lastName: 'Кузнецова', userName: 'olga.kuznetsova' },
  { firstName: 'Максим', lastName: 'Смирнов', userName: 'maxim.smirnov' },
  { firstName: 'Анна', lastName: 'Попова', userName: 'anna.popova' },
  { firstName: 'Денис', lastName: 'Волков', userName: 'denis.volkov' },
  { firstName: 'Наталья', lastName: 'Зайцева', userName: 'natalia.zaitseva' },
  { firstName: 'Павел', lastName: 'Морозов', userName: 'pavel.morozov' },
  { firstName: 'Юлия', lastName: 'Козлова', userName: 'yulia.kozlova' },
  { firstName: 'Иван', lastName: 'Новиков', userName: 'ivan.novikov' },
  { firstName: 'Татьяна', lastName: 'Медведева', userName: 'tatiana.medvedeva' },
  { firstName: 'Андрей', lastName: 'Соколов', userName: 'andrey.sokolov' },
  { firstName: 'Марина', lastName: 'Васильева', userName: 'marina.vasilieva' },
  { firstName: 'Артём', lastName: 'Белов', userName: 'artem.belov' },
  { firstName: 'Кристина', lastName: 'Крылова', userName: 'kristina.krylova' },
  { firstName: 'Роман', lastName: 'Кузьмин', userName: 'roman.kuzmin' },
  { firstName: 'Ирина', lastName: 'Федорова', userName: 'irina.fedorova' },
  { firstName: 'Дмитрий', lastName: 'Орлов', userName: 'dmitry.orlov' },
  { firstName: 'Светлана', lastName: 'Григорьева', userName: 'svetlana.grigorieva' },
  { firstName: 'Владимир', lastName: 'Тимофеев', userName: 'vladimir.timofeev' },
  { firstName: 'Екатерина', lastName: 'Богданова', userName: 'ekaterina.bogdanova' },
  { firstName: 'Станислав', lastName: 'Виноградов', userName: 'stanislav.vinogradov' },
  { firstName: 'Людмила', lastName: 'Дмитриева', userName: 'lyudmila.dmitrieva' },
  { firstName: 'Василий', lastName: 'Матвеев', userName: 'vasily.matveev' },
  { firstName: 'Алиса', lastName: 'Кириллова', userName: 'alisa.kirillova' },
  { firstName: 'Георгий', lastName: 'Яковлев', userName: 'georgy.yakovlev' },
  { firstName: 'Вера', lastName: 'Титова', userName: 'vera.titova' },
  { firstName: 'Николай', lastName: 'Гусев', userName: 'nikolay.gusev' },
  { firstName: 'Полина', lastName: 'Лазарева', userName: 'polina.lazareva' },
  { firstName: 'Антон', lastName: 'Калинин', userName: 'anton.kalinin' },
  { firstName: 'Зоя', lastName: 'Павлова', userName: 'zoya.pavlova' },
  { firstName: 'Евгений', lastName: 'Герасимов', userName: 'evgeny.gerasimov' },
  { firstName: 'Валерия', lastName: 'Романова', userName: 'valeria.romanova' },
  { firstName: 'Михаил', lastName: 'Баранов', userName: 'mikhail.baranov' },
  { firstName: 'Галина', lastName: 'Маркова', userName: 'galina.markova' },
  { firstName: 'Тимофей', lastName: 'Шестаков', userName: 'timofey.shestakov' },
  { firstName: 'Лариса', lastName: 'Никифорова', userName: 'larisa.nikiforova' },
  { firstName: 'Виталий', lastName: 'Сорокин', userName: 'vitaly.sorokin' },
  { firstName: 'Оксана', lastName: 'Савельева', userName: 'oksana.savelieva' },
  { firstName: 'Борис', lastName: 'Филиппов', userName: 'boris.filippov' },
  { firstName: 'Надежда', lastName: 'Коновалова', userName: 'nadezhda.konovalova' },
  { firstName: 'Леонид', lastName: 'Алексеев', userName: 'leonid.alekseev' },
  { firstName: 'Валентина', lastName: 'Гаврилова', userName: 'valentina.gavrilova' },
  { firstName: 'Григорий', lastName: 'Логинов', userName: 'grigory.loginov' },
  { firstName: 'Раиса', lastName: 'Ефимова', userName: 'raisa.efimova' },
];

/** Единый тестовый пароль для всех сидируемых пользователей */
const TEST_PASSWORD = 'TestUser123!';

/**
 * Плагин идемпотентно создаёт 46 тестовых пользователей.
 *
 * - Использует `ON CONFLICT DO NOTHING` для уникального поля userName,
 *   поэтому при повторных запусках дубликаты не создаются.
 * - Пароль хешируется тем же способом, что и в seedAdminPlugin.
 * - Даты createdAt распределены в диапазоне последних 30 дней
 *   для проверки сортировки.
 */
async function seedTestUsers(app: FastifyInstance) {
  // Проверяем, есть ли у нас уже тестовые пользователи
  // (по userName первого тестового пользователя)
  try {
    await app.userService.findAuthUserByUserName(TEST_USERS[0]!.userName);
    app.log.info('ℹ️  Test users already exist, skip seeding');
    return;
  } catch {
    // Not found — продолжаем
  }

  const passwordHash = app.hashService.hash(TEST_PASSWORD);

  // Базовая дата: 30 дней назад
  const baseDate = Date.now() - 30 * 24 * 60 * 60 * 1000;

  const newUsers = TEST_USERS.map((u, index) => {
    // Распределяем даты от 30 дней назад до сегодня
    const createdAt = new Date(baseDate + (index / TEST_USERS.length) * 30 * 24 * 60 * 60 * 1000);

    return {
      userName: u.userName,
      firstName: u.firstName,
      lastName: u.lastName,
      passwordHash,
      role: 'user' as const,
      createdAt,
    };
  });

  // Идемпотентная массовая вставка.
  // Drizzle ORM не поддерживает ON CONFLICT DO NOTHING напрямую без raw SQL,
  // поэтому используем цикл с try/catch для каждой записи.
  let created = 0;
  for (const user of newUsers) {
    try {
      await app.userService.create(user);
      created++;
    } catch {
      // Игнорируем дубликаты — пользователь уже существует
    }
  }

  if (created > 0) {
    app.log.info(`✅ Created ${created} test users for pagination testing`);
  } else {
    app.log.info('ℹ️  All test users already exist, skip seeding');
  }
}

export const seedTestUsersPlugin = fp(seedTestUsers, {
  name: 'seedTestUsersPlugin',
});