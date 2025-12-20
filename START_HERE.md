# 🐊 Игра "Крокодил" - ГОТОВО К ЗАПУСКУ! 🎉

## ✅ Проект полностью реализован и готов к использованию

### 🚀 Быстрый запуск (Выберите один вариант)

#### Вариант 1: Docker Compose (Рекомендуется)

```bash
# Запустить всё одной командой
docker compose up --build

# Открыть в браузере
http://localhost:8080
```

По умолчанию используются слова из базы данных.

#### Вариант 2: Только Java + PostgreSQL в Docker

```bash
# 1. Запустить PostgreSQL
docker compose up postgres

# 2. В новом терминале запустить приложение
java -jar target/crocodile-game-1.0.0.jar

# 3. Открыть в браузере
http://localhost:8080
```

#### Вариант 3: С AI генерацией (YandexGPT)

```bash
# Настроить переменные окружения
export LLM_ACTIVE_PROVIDER=yandex-gpt
export YANDEX_GPT_ENABLED=true
export YANDEX_GPT_AUTH_KEY_PATH=/path/to/authorized_key.json
export YANDEX_GPT_FOLDER_ID=your-folder-id

# Запустить
docker compose up --build
```

См. [YANDEX_GPT_SETUP.md](YANDEX_GPT_SETUP.md) для детальной настройки.

#### Вариант 4: С AI генерацией (LM Studio)

```bash
# 1. Запустить LM Studio на порту 1234
# 2. Настроить переменные
export LLM_ACTIVE_PROVIDER=lm-studio
export LM_STUDIO_ENABLED=true

# 3. Запустить
docker compose up --build
```

## 🎮 Как играть

1. **Создать комнату**: Главная страница → Выбрать тему → Создать комнату
2. **Пригласить друзей**: Поделиться ссылкой
3. **Присоединиться**: Ввести имя
4. **Играть**:
   - Ведущий генерирует слово (из БД или через AI) и показывает жестами
   - Игроки угадывают и вводят ответы
   - Угадавший становится новым ведущим (+10 очков)
   - С AI генерацией слова создаются бесконечно и уникально!

## 🏗️ Архитектура

```
Browser (HTML/CSS/JS)
    ↓ REST API
Spring Boot (Controllers)
    ↓
Services (RoomCoordinator, GameRoundService, LeadershipService)
    ↓
WordProvider (Database / AI with WordPool)
    ↓
LLM Adapters (YandexGPT with JWT / LM Studio)
    ↓
Repositories (JPA)
    ↓
PostgreSQL Database
```

## 🔧 Технологии

- **Backend**: Java 21, Spring Boot 3.5.6, Spring Data JPA, Spring Async
- **Frontend**: Thymeleaf, HTML5, CSS3, Vanilla JavaScript
- **Database**: PostgreSQL 15
- **AI/LLM**: YandexGPT (JWT auth), LM Studio, WordPool optimization
- **Build**: Maven
- **Deploy**: Docker, Docker Compose
- **Migrations**: Liquibase

## 📁 Структура проекта

```
croco-online/
├── src/main/java/com/crocodile/
│   ├── controller/       # REST + View контроллеры
│   ├── service/          # Бизнес-логика
│   │   ├── wordprovider/ # Strategy для генерации слов
│   │   │   ├── llm/      # LLM адаптеры (YandexGPT, LM Studio)
│   │   │   ├── WordPool.java
│   │   │   └── WordPoolRefiller.java
│   │   └── ...
│   ├── config/           # AsyncConfig, RestTemplateConfig
│   ├── repository/       # JPA repositories
│   ├── model/            # Entities
│   ├── dto/              # Data transfer objects
│   ├── exception/        # Exception handling
│   └── util/             # Утилиты
├── src/main/resources/
│   ├── templates/        # HTML (Thymeleaf)
│   ├── static/           # CSS, JS
│   └── db/changelog/     # Liquibase migrations
├── deploy-package/       # Production deployment
│   ├── docker-compose.prod.yml
│   ├── scripts/          # deploy, backup, healthcheck
│   └── secrets/          # authorized_key.json
├── docker-compose.yml    # Docker setup
├── Dockerfile           # Application image
└── README.md            # Документация
```

## 🎯 Основные возможности

- ✅ Создание комнат с уникальными кодами
- ✅ 7 тем: Животные, Профессии, Предметы быта, Фильмы, Еда, Спорт, Города
- ✅ **AI генерация слов через YandexGPT или LM Studio**
- ✅ **Batch optimization (~95% снижение LLM API вызовов)**
- ✅ **Асинхронное пополнение пула слов**
- ✅ Генерация из базы данных (105 предзаполненных слов)
- ✅ Автоматическая смена ведущего
- ✅ Ручное назначение победителя
- ✅ Система очков
- ✅ Проверка с учётом опечаток (Levenshtein)
- ✅ Real-time обновления (polling)
- ✅ Адаптивный дизайн для мобильных
- ✅ 2 режима: Оффлайн и Онлайн

## 🚧 Легко добавить

- WebSocket (вместо polling)
- Таймер раундов
- Видеосвязь
- Новые темы (просто добавить в БД)

## 🆘 Помощь

### Порт занят?
```bash
export SERVER_PORT=8081
mvn spring-boot:run
```

### PostgreSQL не стартует?
```bash
docker compose down -v
docker compose up postgres
```

### Нужны логи?
```bash
docker compose logs -f app
```

## 📚 Полная документация

- 📖 [README.md](README.md) - Подробное описание
- 🏛️ [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура
- ⚡ [QUICKSTART.md](QUICKSTART.md) - Быстрый старт с примерами
- ⚙️ [CONFIGURATION.md](CONFIGURATION.md) - Конфигурация
- ✅ [IMPLEMENTATION_SUMMARY.md](IMPLEMENTATION_SUMMARY.md) - Что реализовано
- 🤖 [YANDEX_GPT_SETUP.md](YANDEX_GPT_SETUP.md) - Настройка YandexGPT
- 📦 [LLM_BATCH_OPTIMIZATION.md](LLM_BATCH_OPTIMIZATION.md) - Batch оптимизация
- 🔄 [ASYNC_FIX_EXPLANATION.md](ASYNC_FIX_EXPLANATION.md) - Async исправления

## 🎉 Готово к использованию!

### Базовый запуск (слова из БД):
```bash
docker compose up
```

### С AI генерацией (YandexGPT):
```bash
export LLM_ACTIVE_PROVIDER=yandex-gpt
export YANDEX_GPT_ENABLED=true
export YANDEX_GPT_AUTH_KEY_PATH=/path/to/authorized_key.json
export YANDEX_GPT_FOLDER_ID=your-folder-id
docker compose up
```

### С AI генерацией (LM Studio):
```bash
# Запустить LM Studio локально
export LLM_ACTIVE_PROVIDER=lm-studio
export LM_STUDIO_ENABLED=true
docker compose up
```

И откройте: **http://localhost:8080**

**Приятной игры с AI генерацией! 🐊🎮🤖**

---

*Если возникли вопросы - смотрите документацию выше или создайте issue*

