# 🚀 Быстрый старт

## Запуск за 2 минуты

### Вариант 1: Docker Compose (Самый простой)

```bash
# 1. Клонировать и перейти в директорию
cd croco-online

# 2. Запустить всё одной командой
docker-compose up --build

# 3. Открыть браузер
open http://localhost:8080
```

Готово! 🎉

### Вариант 2: Локальная разработка

```bash
# 1. Запустить только PostgreSQL в Docker
docker-compose up postgres

# 2. В новом терминале запустить приложение
mvn spring-boot:run

# 3. Открыть браузер
open http://localhost:8080
```

## Первые шаги

### 1. Создайте комнату

1. Откройте http://localhost:8080
2. Выберите тему (например, "Животные")
3. Нажмите "Создать комнату"
4. Скопируйте ссылку на комнату

### 2. Присоединитесь к игре

1. Откройте ссылку (или откройте в новой вкладке/incognito)
2. Введите своё имя
3. Нажмите "Присоединиться"

### 3. Играйте!

**Если вы ведущий:**
- Нажмите "Сгенерировать новое слово"
- Покажите слово жестами
- Или назначьте победителя вручную

**Если вы игрок:**
- Смотрите на ведущего
- Вводите свои варианты ответа
- Первый угадавший становится ведущим!

## Тестовые сценарии

### Сценарий 1: Игра с автоматическим угадыванием

```bash
# Терминал 1: Создать комнату и получить код
curl -X POST http://localhost:8080/api/rooms \
  -H "Content-Type: application/json" \
  -d '{"theme":"Животные","wordProviderType":"database"}'

# Получите roomCode из ответа, например "ABC123"

# Терминал 2: Присоединиться как Player 1 (будет ведущим)
curl -X POST http://localhost:8080/api/rooms/ABC123/join \
  -H "Content-Type: application/json" \
  -c cookies1.txt \
  -d '{"playerName":"Player1"}'

# Терминал 3: Присоединиться как Player 2
curl -X POST http://localhost:8080/api/rooms/ABC123/join \
  -H "Content-Type: application/json" \
  -c cookies2.txt \
  -d '{"playerName":"Player2"}'

# Терминал 2: Сгенерировать слово (Player 1 - ведущий)
curl -X POST http://localhost:8080/api/rooms/ABC123/new-word \
  -b cookies1.txt

# Получите слово из ответа, например "Кошка"

# Терминал 3: Player 2 угадывает слово
curl -X POST http://localhost:8080/api/rooms/ABC123/guess \
  -H "Content-Type: application/json" \
  -b cookies2.txt \
  -d '{"guess":"Кошка"}'

# Player 2 теперь ведущий!

# Проверить состояние комнаты
curl http://localhost:8080/api/rooms/ABC123/state -b cookies2.txt
```

### Сценарий 2: Ручное назначение победителя

```bash
# После присоединения двух игроков...

# Ведущий назначает победителя вручную
curl -X POST http://localhost:8080/api/rooms/ABC123/assign-winner \
  -H "Content-Type: application/json" \
  -b cookies1.txt \
  -d '{"winnerId":2}'  # ID второго игрока
```

## Проверка работоспособности

### Health Check

```bash
# Проверить, что приложение запущено
curl http://localhost:8080/

# Должен вернуть HTML главной страницы
```

### Проверить доступные темы

```bash
curl http://localhost:8080/api/rooms/themes

# Ответ:
# ["Животные","Профессии","Предметы быта","Фильмы и сериалы","Еда и напитки","Спорт","Города и страны"]
```

### Проверить базу данных

```bash
# Подключиться к PostgreSQL
docker exec -it crocodile-postgres psql -U crocodile_user -d crocodile_db

# Проверить таблицы
\dt

# Проверить количество слов
SELECT theme, COUNT(*) FROM words GROUP BY theme;

# Выход
\q
```

## Режим разработки

### Hot Reload для Java

```bash
# Установить Spring Boot DevTools (уже в pom.xml)
# Запустить с профилем dev
mvn spring-boot:run

# При изменении .java файлов - автоматическая перекомпиляция
```

### Изменение frontend без перезапуска

1. Измените файлы в `src/main/resources/templates/` или `static/`
2. В браузере нажмите Ctrl+Shift+R (hard refresh)
3. Изменения сразу видны (thymeleaf.cache=false в dev режиме)

### Просмотр SQL запросов

```yaml
# В application.yml установить:
spring:
  jpa:
    show-sql: true
```

Или через environment variable:
```bash
export SHOW_SQL=true
mvn spring-boot:run
```

### Debug режим

```bash
# Запустить с debug портом
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# В IntelliJ IDEA: Run → Attach to Process → выбрать порт 5005
```

## Тестирование

### Запустить все тесты

```bash
mvn test
```

### Запустить конкретный тест

```bash
mvn test -Dtest=StringSimilarityTest
mvn test -Dtest=RoomServiceTest
```

### Запустить с покрытием

```bash
mvn test jacoco:report

# Отчёт будет в target/site/jacoco/index.html
open target/site/jacoco/index.html
```

## Частые проблемы

### Порт 8080 занят

```bash
# Изменить порт
export SERVER_PORT=8081
mvn spring-boot:run

# Или в application.yml
server:
  port: 8081
```

### Ошибка подключения к PostgreSQL

```bash
# Проверить, что PostgreSQL запущен
docker ps | grep postgres

# Если нет - запустить
docker-compose up postgres

# Проверить логи
docker logs crocodile-postgres
```

### База данных не создаётся

```bash
# Пересоздать базу с нуля
docker-compose down -v  # Удалить volumes
docker-compose up postgres

# Liquibase создаст все таблицы при старте приложения
```

### Тесты падают

```bash
# Убедиться, что Docker запущен (для Testcontainers)
docker ps

# Очистить и пересобрать
mvn clean install -DskipTests
mvn test
```

## Полезные команды

### Maven

```bash
mvn clean                    # Очистить target/
mvn compile                  # Скомпилировать
mvn package                  # Собрать JAR
mvn spring-boot:run          # Запустить приложение
mvn test                     # Запустить тесты
mvn dependency:tree          # Показать зависимости
```

### Docker

```bash
docker-compose up            # Запустить все сервисы
docker-compose up -d         # Запустить в фоне
docker-compose down          # Остановить все сервисы
docker-compose down -v       # Остановить и удалить volumes
docker-compose logs -f app   # Логи приложения
docker-compose ps            # Статус сервисов
```

### PostgreSQL

```bash
# Подключение к БД
docker exec -it crocodile-postgres psql -U crocodile_user -d crocodile_db

# Backup
docker exec crocodile-postgres pg_dump -U crocodile_user crocodile_db > backup.sql

# Restore
docker exec -i crocodile-postgres psql -U crocodile_user -d crocodile_db < backup.sql
```

## Следующие шаги

1. 📖 Прочитайте [ARCHITECTURE.md](ARCHITECTURE.md) для понимания структуры
2. ⚙️ Изучите [CONFIGURATION.md](CONFIGURATION.md) для настройки
3. 🔧 Начните разработку новых фичей
4. 🧪 Добавьте больше тестов
5. 🚀 Задеплойте в production

## Получить помощь

- Проверьте логи: `docker-compose logs -f`
- Посмотрите существующие issue
- Создайте новый issue с описанием проблемы

Удачи! 🐊

