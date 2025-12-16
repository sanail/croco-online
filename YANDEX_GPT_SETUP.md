# Настройка Yandex GPT с JWT аутентификацией

## Обзор

Приложение использует JWT-based аутентификацию для Yandex Cloud Foundation Models API. IAM-токены генерируются автоматически и обновляются каждые 12 часов.

## Шаги настройки

### 1. Создание сервисного аккаунта

1. Откройте [Yandex Cloud Console](https://console.cloud.yandex.ru/)
2. Выберите ваш каталог (folder)
3. Перейдите в раздел **"Сервисные аккаунты"**
4. Нажмите **"Создать сервисный аккаунт"**
5. Укажите имя (например, `crocodile-game-sa`)
6. Нажмите **"Создать"**

### 2. Назначение роли

1. Откройте созданный сервисный аккаунт
2. Перейдите на вкладку **"Права доступа"**
3. Нажмите **"Назначить роли"**
4. Добавьте роль **`ai.languageModels.user`**
5. Сохраните изменения

### 3. Создание авторизованного ключа

1. В настройках сервисного аккаунта перейдите на вкладку **"Авторизованные ключи"**
2. Нажмите **"Создать новый ключ"**
3. Выберите **"Создать авторизованный ключ"**
4. Нажмите **"Создать"**
5. Скачайте JSON-файл с ключом (он будет выглядеть примерно так):

```json
{
   "id": "ajexxxxxxxxx",
   "service_account_id": "ajeyyyyyyy",
   "created_at": "2024-12-16T10:00:00Z",
   "key_algorithm": "RSA_2048",
   "public_key": "-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----",
   "private_key": "PLEASE DO NOT REMOVE THIS LINE! Yandex.Cloud SA Key ID <ajexxxxxxxxx>\n-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
}
```

⚠️ **ВАЖНО**: 
- Сохраните этот файл в безопасном месте. Приватный ключ нельзя будет получить повторно!
- Обратите внимание, что `private_key` содержит комментарий Yandex Cloud в начале - это нормально, парсер обрабатывает его автоматически

### 4. Настройка переменных окружения

Установите следующие переменные окружения:

```bash
# Путь к JSON-файлу с авторизованным ключом
export YANDEX_GPT_AUTH_KEY_PATH=/path/to/authorized_key.json

# ID каталога (folder) в Yandex Cloud
export YANDEX_GPT_FOLDER_ID=b1xxxxxxxxxxxxxx

# Включить Yandex GPT адаптер
export YANDEX_GPT_ENABLED=true

# Выбрать Yandex GPT как активный провайдер
export LLM_ACTIVE_PROVIDER=yandex-gpt
```

### 5. Получение Folder ID

Folder ID можно найти:
1. В Yandex Cloud Console, в URL страницы вашего каталога
2. Или через CLI: `yc resource-manager folder get <folder-name> --format json | grep "^id:"`

### 6. Проверка настройки

Запустите приложение:

```bash
mvn spring-boot:run
```

В логах вы должны увидеть:

```
INFO  c.c.s.w.l.YandexIamTokenProvider : Loading Yandex Cloud authorized key from: /path/to/authorized_key.json
INFO  c.c.s.w.l.YandexIamTokenProvider : Authorized key loaded successfully for service account: ajeyyyyyyy
INFO  c.c.s.w.l.YandexIamTokenProvider : IAM token refreshed successfully. Expires at: 2024-12-16T22:00:00Z
```

## Безопасность

### Рекомендации по хранению ключа

1. **Не коммитьте** файл с ключом в git
2. Добавьте путь к ключу в `.gitignore`:
   ```
   **/authorized_key*.json
   ```
3. Храните ключ в безопасном месте с ограниченными правами доступа:
   ```bash
   chmod 600 /path/to/authorized_key.json
   ```
4. В production используйте secrets management (например, Yandex Lockbox, HashiCorp Vault)

## Troubleshooting

### Ошибка "Authorized key file not found"

**Проблема**: Файл с ключом не найден по указанному пути.

**Решение**: 
- Проверьте правильность пути в `YANDEX_GPT_AUTH_KEY_PATH`
- Убедитесь, что файл существует и доступен для чтения

### Ошибка "Failed to load authorized key"

**Проблема**: Ошибка парсинга JSON-файла.

**Решение**:
- Проверьте, что файл содержит валидный JSON
- Убедитесь, что файл не поврежден

### Ошибка "401 Unauthorized"

**Проблема**: Недействительный IAM-токен или отсутствие прав доступа.

**Решение**:
- Проверьте, что сервисному аккаунту назначена роль `ai.languageModels.user`
- Убедитесь, что `YANDEX_GPT_FOLDER_ID` соответствует каталогу, к которому привязан сервисный аккаунт
- Попробуйте пересоздать авторизованный ключ

### Ошибка "invalid model_uri"

**Проблема**: Неправильный format URI модели.

**Решение**:
- Проверьте правильность `YANDEX_GPT_FOLDER_ID`
- Убедитесь, что используете правильное имя модели (по умолчанию `yandexgpt-lite`)

## Дополнительные настройки

### Выбор модели

Вы можете выбрать другую модель:

```bash
export YANDEX_GPT_MODEL=yandexgpt  # Полная версия (более мощная, но дороже)
# или
export YANDEX_GPT_MODEL=yandexgpt-lite  # Lite версия (по умолчанию)
```

### Настройка температуры

Контролируйте "креативность" генерации:

```bash
export YANDEX_GPT_TEMPERATURE=0.7  # От 0.0 (детерминированно) до 1.0 (креативно)
```

### Максимальное количество токенов

```bash
export YANDEX_GPT_MAX_TOKENS=1000  # Максимум токенов в ответе
```

## Архитектура

```
YandexGptLlmAdapter
    └── YandexIamTokenProvider (автоматическое обновление токенов)
        ├── YandexJwtGenerator (генерация JWT)
        └── RestTemplate (обмен JWT на IAM-токен)
```

**Процесс аутентификации:**
1. Загрузка авторизованного ключа из JSON-файла
2. Генерация JWT с подписью приватным ключом (**PS256** - RSA-PSS with SHA-256)
3. Обмен JWT на IAM-токен через Yandex IAM API
4. Кэширование IAM-токена (обновление за 10 минут до истечения)
5. Использование токена в запросах к Yandex GPT API

⚠️ **ВАЖНО**: Yandex Cloud требует использование алгоритма подписи **PS256** (RSA-PSS), а не RS256!

## Полезные ссылки

- [Yandex Cloud Console](https://console.cloud.yandex.ru/)
- [Документация Yandex Foundation Models](https://cloud.yandex.ru/docs/foundation-models/)
- [Создание IAM-токенов для сервисного аккаунта](https://cloud.yandex.ru/docs/iam/operations/iam-token/create-for-sa)

