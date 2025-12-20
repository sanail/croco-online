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

# LLM Configuration
LLM_ACTIVE_PROVIDER=database  # Options: database, lm-studio, yandex-gpt

# LM Studio Configuration (if using AI)
LM_STUDIO_ENABLED=false
LM_STUDIO_URL=http://localhost:1234
LM_STUDIO_MODEL=openai/gpt-oss-20b
LM_STUDIO_TEMPERATURE=0.7
LM_STUDIO_MAX_TOKENS=1000
LM_STUDIO_TIMEOUT=10

# Yandex GPT Configuration (if using AI)
YANDEX_GPT_ENABLED=false
YANDEX_GPT_AUTH_KEY_PATH=/path/to/authorized_key.json
YANDEX_GPT_FOLDER_ID=your-folder-id
YANDEX_GPT_MODEL=yandexgpt-lite
YANDEX_GPT_TEMPERATURE=0.7
YANDEX_GPT_MAX_TOKENS=1000

# Word Pool Configuration (for AI providers)
LLM_BATCH_SIZE=20          # Words per batch request
LLM_MIN_THRESHOLD=5        # Trigger refill threshold
LLM_INITIAL_SIZE=10        # Initial pool size
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
  llm:
    active-provider: database         # Провайдер слов: database / lm-studio / yandex-gpt
    word-pool:
      batch-size: 20                  # Слов в одном batch запросе
      min-threshold: 5                # Порог для async refill
      initial-size: 10                # Начальный размер пула
```

## Выбор провайдера слов

Приложение поддерживает три источника слов:

### 1. Database (по умолчанию)

Использует предзаполненную базу данных с 105 словами в 7 темах.

```yaml
game:
  llm:
    active-provider: database
```

**Плюсы:**
- Мгновенная генерация
- Не требует внешних сервисов
- Бесплатно

**Минусы:**
- Ограниченный набор слов
- Нужно вручную добавлять новые слова

### 2. LM Studio (локальные LLM модели)

Использует локально запущенную LLM через LM Studio.

```yaml
game:
  llm:
    active-provider: lm-studio
    lm-studio:
      enabled: true
      url: http://localhost:1234
      model: openai/gpt-oss-20b
      temperature: 0.7
```

**Плюсы:**
- Неограниченная генерация слов
- Полный контроль (локально)
- Бесплатно (после скачивания модели)
- Приватность данных

**Минусы:**
- Требует мощное железо (GPU рекомендуется)
- Нужно скачать и запустить LM Studio
- Медленнее облачных решений

**Настройка:**
1. Скачайте [LM Studio](https://lmstudio.ai/)
2. Загрузите модель (например, GPT-OSS-20B)
3. Запустите локальный сервер в LM Studio
4. Установите `LM_STUDIO_ENABLED=true`

### 3. Yandex GPT (облачная LLM)

Использует Yandex Cloud Foundation Models API.

```yaml
game:
  llm:
    active-provider: yandex-gpt
    yandex-gpt:
      enabled: true
      authorized-key-path: /path/to/authorized_key.json
      folder-id: your-folder-id
      model: yandexgpt-lite
```

**Плюсы:**
- Быстрая генерация
- Не требует локального железа
- Высокое качество слов

**Минусы:**
- Платный сервис (но есть бесплатный tier)
- Требует настройки Yandex Cloud
- Зависимость от внешнего API

**Настройка:**
См. [YANDEX_GPT_SETUP.md](YANDEX_GPT_SETUP.md) для детальных инструкций.

## Настройки Word Pool (для AI провайдеров)

WordPool оптимизирует работу с LLM, снижая количество API вызовов на ~95%.

```yaml
game:
  llm:
    word-pool:
      batch-size: 20          # Сколько слов генерировать за один запрос
      min-threshold: 5        # При каком размере пула начинать async refill
      initial-size: 10        # Сколько слов генерировать при первом запросе
```

### Примеры конфигураций

**Консервативная (меньше API вызовов, возможны задержки):**
```yaml
batch-size: 30
min-threshold: 3
initial-size: 5
```

**Агрессивная (больше API вызовов, быстрый response):**
```yaml
batch-size: 10
min-threshold: 8
initial-size: 10
```

**Сбалансированная (рекомендуется):**
```yaml
batch-size: 20
min-threshold: 5
initial-size: 10
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
5. Настройте thread pool для async операций:

```yaml
# В AsyncConfig можно настроить:
executor.setCorePoolSize(2);      # Минимум потоков
executor.setMaxPoolSize(5);       # Максимум потоков
executor.setQueueCapacity(100);   # Размер очереди
```

## HTTP Client Configuration

Настройки для запросов к LLM API:

```yaml
http:
  client:
    connect-timeout-seconds: 5    # Timeout для установки соединения
    read-timeout-seconds: 30      # Timeout для ожидания ответа (важно для LLM)
```

Для медленных LLM моделей можно увеличить `read-timeout-seconds`.

## Deployment Scenarios

### Сценарий 1: Database-only (простейший)

```bash
export LLM_ACTIVE_PROVIDER=database
docker compose up
```

Подходит для: тестирования, небольших групп, когда достаточно предзаполненных слов.

### Сценарий 2: С LM Studio (мощное железо)

```bash
# Запустить LM Studio на порту 1234
export LLM_ACTIVE_PROVIDER=lm-studio
export LM_STUDIO_ENABLED=true
export LM_STUDIO_URL=http://localhost:1234
docker compose up
```

Подходит для: локального запуска, полный контроль, приватность данных.

### Сценарий 3: С Yandex GPT (production)

```bash
export LLM_ACTIVE_PROVIDER=yandex-gpt
export YANDEX_GPT_ENABLED=true
export YANDEX_GPT_AUTH_KEY_PATH=/secrets/authorized_key.json
export YANDEX_GPT_FOLDER_ID=b1xxxxxxxxxxxxxx
docker compose up
```

Подходит для: production deployment, масштабируемость, профессиональное использование.

### Сценарий 4: Гибридный (Database + AI fallback)

Можно настроить приоритет провайдеров в коде `WordProviderFactory`, чтобы использовать Database как fallback при недоступности AI.

## Troubleshooting

### LM Studio не отвечает

```bash
# Проверить доступность
curl http://localhost:1234/v1/models

# Увеличить timeout
export LM_STUDIO_TIMEOUT=30
```

### Yandex GPT ошибки аутентификации

```bash
# Проверить путь к ключу
ls -la $YANDEX_GPT_AUTH_KEY_PATH

# Проверить права доступа
chmod 600 $YANDEX_GPT_AUTH_KEY_PATH

# Проверить логи
docker compose logs app | grep "Yandex"
```

### WordPool не пополняется

Проверьте логи на наличие async выполнения:
```bash
docker compose logs app | grep "word-pool-refill"
```

Должны видеть логи из потоков с префиксом `[word-pool-refill-X]`.

