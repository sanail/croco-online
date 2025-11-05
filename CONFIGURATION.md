# Конфигурация приложения

## Переменные окружения

Создайте `.env` файл в корне проекта со следующими переменными:

```bash
# Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=crocodile_db
DB_USERNAME=crocodile_user
DB_PASSWORD=crocodile_pass

# Server Configuration
SERVER_PORT=8080

# Logging
SHOW_SQL=false

# Spring Profile
SPRING_PROFILES_ACTIVE=prod
```

## Профили Spring

### `default` (dev)
- Используется по умолчанию для локальной разработки
- SQL логи включены
- Thymeleaf кеширование отключено

### `test`
- Используется для тестирования
- Использует Testcontainers для PostgreSQL
- SQL логи включены

### `prod`
- Используется в production окружении
- SQL логи отключены
- Thymeleaf кеширование включено
- Сжатие HTTP ответов включено

## Настройки игры

Можно изменить в `application.yml`:

```yaml
game:
  room:
    code-length: 6                    # Длина кода комнаты (рекомендуется 6-8)
    inactive-timeout-minutes: 60      # Автоочистка неактивных комнат (в минутах)
  session:
    cookie-name: CROCODILE_SESSION    # Имя cookie для сессии
    cookie-max-age: 86400             # Время жизни cookie (секунды)
  score:
    points-per-win: 10                # Очков за правильное угадывание
```

## Безопасность

В production окружении:

1. Используйте сильные пароли для PostgreSQL
2. Настройте HTTPS
3. Настройте CORS правила при необходимости
4. Используйте переменные окружения вместо хардкода
5. Регулярно обновляйте зависимости

## Масштабирование

Для обработки большого количества комнат:

1. Увеличьте `inactive-timeout-minutes` чтобы сохранять комнаты дольше
2. Настройте connection pool для PostgreSQL
3. Рассмотрите переход на WebSocket вместо polling
4. Добавьте Redis для кеширования состояний комнат

