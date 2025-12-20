# Архитектура приложения "Крокодил"

## Общая архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                         Browser                              │
│  ┌────────────┐  ┌────────────┐  ┌────────────────────┐    │
│  │  index.js  │  │  room.js   │  │  style.css         │    │
│  └─────┬──────┘  └─────┬──────┘  └────────────────────┘    │
└────────┼────────────────┼───────────────────────────────────┘
         │                │
         │ REST API       │ Polling (every 2s)
         ▼                ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Application                    │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Controllers Layer                        │  │
│  │  ┌───────────────┐  ┌──────────────┐  ┌──────────┐  │  │
│  │  │ViewController │  │RoomController│  │GameCtrl  │  │  │
│  │  └───────────────┘  └──────────────┘  └──────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               Service Layer                           │  │
│  │  ┌──────────────┐  ┌──────────┐  ┌───────────────┐  │  │
│  │  │RoomCoordinator│ │GameRound │  │LeadershipSvc  │  │  │
│  │  └──────────────┘  │Service   │  └───────────────┘  │  │
│  │  ┌───────────┐     └──────────┘  ┌─────────────┐   │  │
│  │  │RoomService│  ┌──────────┐     │PlayerService│   │  │
│  │  └───────────┘  │SessionSvc│     └─────────────┘   │  │
│  │                 └──────────┘                         │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          WordProvider Strategy Pattern                │  │
│  │  ┌─────────────────┐ ┌──────────────┐ ┌──────────┐  │  │
│  │  │DatabaseProvider │ │AiWordProvider│ │ThemeProvider│ │
│  │  └─────────────────┘ └──────┬───────┘ └──────────┘  │  │
│  │                              │                        │  │
│  │  ┌──────────────────────────┴────────────────────┐  │  │
│  │  │          LLM Integration Layer                 │  │  │
│  │  │  ┌──────────┐  ┌───────────┐  ┌───────────┐  │  │  │
│  │  │  │WordPool  │  │WordPool   │  │LlmAdapter │  │  │  │
│  │  │  │          │  │Refiller   │  │Factory    │  │  │  │
│  │  │  └──────────┘  └─────┬─────┘  └─────┬─────┘  │  │  │
│  │  │                      │ @Async        │         │  │  │
│  │  │         ┌────────────┴───────────────┘         │  │  │
│  │  │         │                                       │  │  │
│  │  │  ┌──────▼────────┐        ┌────────────────┐  │  │  │
│  │  │  │YandexGPT      │        │LM Studio       │  │  │  │
│  │  │  │(JWT Auth)     │        │Adapter         │  │  │  │
│  │  │  └───────────────┘        └────────────────┘  │  │  │
│  │  └───────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │            Repository Layer (Spring Data JPA)         │  │
│  │  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌─────────┐ │  │
│  │  │RoomRepo  │ │PlayerRepo│ │WordRepo  │ │HistoryRepo│ │
│  │  └──────────┘ └──────────┘ └──────────┘ └─────────┘ │  │
│  └──────────────────────────────────────────────────────┘  │
└────────────────────────┬─────────────────────────────────────┘
                         │
                         │ JDBC
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                     PostgreSQL Database                      │
│  ┌────────┐  ┌────────┐  ┌────────┐  ┌──────────────┐     │
│  │ rooms  │  │players │  │ words  │  │game_history  │     │
│  └────────┘  └────────┘  └────────┘  └──────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Слои приложения

### 1. Presentation Layer (Frontend)

**Технологии**: Thymeleaf, HTML5, CSS3, Vanilla JavaScript

**Компоненты**:
- `index.html` - главная страница создания комнат
- `room.html` - игровая комната
- `index.js` - логика создания комнат
- `room.js` - логика игрового процесса, polling
- `style.css` - адаптивные стили

**Особенности**:
- Polling каждые 2 секунды для обновления состояния
- Динамическое переключение между view ведущего и игрока
- Адаптивный дизайн для мобильных устройств

### 2. Controller Layer

**Контроллеры**:

1. **ViewController** - Server-side rendering
   - `GET /` - главная страница
   - `GET /room/{code}` - страница комнаты

2. **RoomController** - REST API для комнат
   - `POST /api/rooms` - создание комнаты
   - `GET /api/rooms/{code}/state` - получение состояния
   - `GET /api/rooms/themes` - список тем

3. **GameController** - REST API для игровых действий
   - `POST /api/rooms/{code}/join` - присоединение
   - `POST /api/rooms/{code}/guess` - отправка ответа
   - `POST /api/rooms/{code}/assign-winner` - назначение победителя
   - `POST /api/rooms/{code}/new-word` - генерация слова
   - `POST /api/rooms/{code}/leave` - выход

### 3. Service Layer

**Основные сервисы**:

1. **RoomCoordinator** - координатор игровых комнат
   - Оркестрирует взаимодействие между сервисами
   - Управляет жизненным циклом комнаты

2. **GameRoundService** - сервис игровых раундов
   - Генерация новых слов через WordProvider
   - Проверка угадывания с учетом опечаток
   - Сохранение истории раундов

3. **LeadershipService** - управление ведущим
   - Смена ведущего при угадывании
   - Назначение нового ведущего при выходе текущего
   - Логика выбора следующего ведущего

4. **RoomService** - управление комнатами
   - Создание комнат с уникальными кодами
   - Жизненный цикл комнат
   - Scheduled задача для очистки неактивных комнат

5. **PlayerService** - управление игроками
   - Присоединение/выход игроков
   - Управление очками
   - Управление статусом активности

6. **SessionService** - управление сессиями
   - Генерация и хранение session ID в cookie
   - Идентификация игроков между запросами

### 4. Strategy Pattern - WordProvider

**Интерфейс WordProvider**:
```java
public interface WordProvider {
    String generateWord(String theme);
    List<String> getSupportedThemes();
    String getType();
}
```

**Реализации**:
1. **DatabaseWordProvider** ✅ - получение слов из БД (105 предзаполненных слов)
2. **AiWordProvider** ✅ - AI генерация через LLM с использованием WordPool для оптимизации

**LLM Adapters** (используются через AiWordProvider):
1. **YandexGptLlmAdapter** ✅ - интеграция с Yandex Cloud Foundation Models
   - JWT-based аутентификация через сервисный аккаунт
   - Автоматическое обновление IAM токенов
   - Batch генерация слов
2. **LmStudioLlmAdapter** ✅ - интеграция с локальной LLM через LM Studio
   - REST API интеграция
   - Batch генерация слов
   - Настраиваемые промпты

**WordProviderFactory** - фабрика для выбора провайдера на основе конфигурации

**WordPool** - пул предгенерированных слов для оптимизации
- Thread-safe хранилище слов по темам
- Instant response (без ожидания LLM)
- Снижение LLM API вызовов на ~95%

**WordPoolRefiller** - асинхронное пополнение пула
- @Async методы для background генерации
- Self-injection pattern для корректной работы Spring AOP
- Предотвращение дублирующих refill операций
- Batch генерация (по умолчанию 20 слов за раз)

### 5. Repository Layer

**JPA Repositories**:
- `RoomRepository` - CRUD для комнат
- `PlayerRepository` - CRUD для игроков
- `WordRepository` - работа со словарём
- `GameHistoryRepository` - история игр

### 6. Model Layer

**Entities**:

1. **Room** - игровая комната
   - code, theme, status, currentWord, currentLeaderId
   - wordProviderType, createdAt, lastActivity

2. **Player** - игрок
   - roomId, sessionId, name, score
   - isLeader, isActive, joinedAt

3. **Word** - слово из словаря
   - word, theme, locale

4. **GameHistory** - история раундов
   - roomId, word, leaderId, winnerId
   - startTime, endTime

### 7. Data Layer

**PostgreSQL Database**:
- Управляется через Liquibase migrations
- 4 основные таблицы + индексы
- Предзаполнена словарём (105 слов в 7 темах)

### 8. Configuration Layer

**AsyncConfig** - конфигурация асинхронного выполнения:
- ThreadPoolTaskExecutor для WordPoolRefiller
- Core pool size: 2, Max: 5, Queue: 100
- Custom thread naming: "word-pool-refill-"

**RestTemplateConfig** - настройка HTTP клиента:
- Connection timeout: 5 секунд
- Read timeout: 30 секунд (для LLM запросов)

## Потоки данных

### Создание комнаты

```
User → POST /api/rooms
  → RoomController.createRoom()
    → GameService.createRoom()
      → RoomService.createRoom()
        → WordProviderFactory.getProvider()
        → RoomRepository.save()
      ← Room with unique code
    ← CreateRoomResponse
  ← JSON response with room code and URL
```

### Присоединение к игре

```
User → POST /api/rooms/{code}/join
  → GameController.joinRoom()
    → SessionService.getOrCreateSessionId()
    → GameService.joinRoom()
      → RoomService.getRoomByCode()
      → PlayerService.joinRoom()
        → PlayerRepository.save()
      → Set leader if first player
      ← Player
    ← JoinRoomResponse
  ← JSON with player info + Set-Cookie
```

### Угадывание слова

```
User → POST /api/rooms/{code}/guess
  → GameController.submitGuess()
    → SessionService.getSessionId()
    → GameRoundService.submitGuess()
      → StringSimilarity.isCorrectGuess()
      → If correct:
        → GameHistoryRepository.save()
        → PlayerService.addWinScore()
        → LeadershipService.changeLeader()
        → RoomService.updateRoom()
      ← GuessResponse
    ← JSON with result
```

### Генерация слова через AI (новый flow)

```
Leader → POST /api/rooms/{code}/new-word
  → GameController.generateNewWord()
    → GameRoundService.generateNewWord()
      → WordProviderFactory.getProvider("ai")
      → AiWordProvider.generateWord(theme)
        → WordPool.pollWord(theme)
        → If pool has words:
          ← word (instant response)
          → Check if pool.size < threshold
          → If yes: WordPoolRefiller.triggerAsyncRefill(theme)
            → @Async WordPoolRefiller.refillPoolAsync(theme)
              → LlmAdapter.generateWords(theme, 20) [background]
              → WordPool.addWords(generatedWords)
        → If pool empty:
          → LlmAdapter.generateWords(theme, 10) [synchronous]
          → Take first word, add rest to pool
          → Trigger async refill for next time
      ← Generated word
    ← NewWordResponse
  ← JSON with new word

Note: Pool refill happens asynchronously in background thread,
      reducing LLM API calls by ~95%
```

### JWT Authentication Flow (Yandex Cloud)

```
App Startup → YandexIamTokenProvider.init()
  → Load authorized_key.json
  → Parse service_account_id and private_key
  
First LLM Request → YandexGptLlmAdapter.generateWords()
  → YandexIamTokenProvider.getIamToken()
    → Check if token expired or missing
    → If expired:
      → YandexJwtGenerator.generateJwt(serviceAccountId, privateKey)
        → Create JWT with PS256 signature (RSA-PSS)
        → Set expiration: now + 1 hour
      → Exchange JWT for IAM token via Yandex IAM API
      → Cache IAM token (valid for 12 hours)
    ← Valid IAM token
  → Make request to Yandex GPT with IAM token
  ← Generated words

Background: IAM token auto-refreshes 10 minutes before expiration
```

### Обновление состояния (Polling)

```
Browser (every 2s) → GET /api/rooms/{code}/state
  → RoomController.getRoomState()
    → GameService.getRoomState()
      → RoomService.getRoomByCode()
      → PlayerService.getActivePlayers()
      → Build RoomStateResponse
      ← RoomStateResponse
    ← JSON with current state
  → JavaScript updates UI
```

## Паттерны проектирования

1. **Strategy Pattern** - WordProvider для различных источников слов (Database, AI)
2. **Factory Pattern** - WordProviderFactory и LlmAdapterFactory для создания провайдеров
3. **DTO Pattern** - для передачи данных между слоями
4. **Repository Pattern** - Spring Data JPA
5. **Service Layer Pattern** - изоляция бизнес-логики
6. **MVC Pattern** - Spring MVC
7. **Self-Injection Pattern** - для корректной работы @Async в WordPoolRefiller
8. **Object Pool Pattern** - WordPool для переиспользования предгенерированных слов

## Обработка Edge Cases

1. **Ведущий покидает комнату**
   - Автоматически назначается новый лидер из оставшихся игроков
   - Если игроков нет - комната помечается как неактивная

2. **Все игроки покинули комнату**
   - Комната помечается как INACTIVE
   - Scheduled задача удалит её через час

3. **Переподключение игрока**
   - SessionId в cookie позволяет восстановить состояние
   - Игрок сохраняет свои очки и статус

4. **Проверка угадывания с опечатками**
   - Используется алгоритм Levenshtein Distance
   - Допускается 1 символ отличия для слов >4 символов

5. **Генерация уникальных кодов комнат**
   - Проверка на существование в цикле
   - Исключены путающие символы (I, O, 0, 1)

## Масштабируемость

### Текущие ограничения

- Polling создаёт нагрузку на сервер (запрос каждые 2 сек)
- Состояние комнаты хранится в БД (каждый запрос - SELECT)
- Single instance приложения

### Реализованные оптимизации

1. **WordPool с асинхронным refill**
   - Снижение LLM API вызовов на ~95%
   - Instant response для пользователей
   - Background пополнение пула в отдельных потоках

2. **Batch генерация слов**
   - Один LLM запрос генерирует 20 слов
   - Конфигурируемый размер batch (default: 20)
   - Адаптивный threshold для refill (default: 5)

3. **Thread Pool для async операций**
   - Отдельный executor для WordPoolRefiller
   - Изолированные потоки для LLM запросов
   - Не блокирует HTTP request threads

### Возможные улучшения

1. **WebSocket вместо Polling**
   - Реальное двустороннее соединение
   - Снижение нагрузки на 90%

2. **Redis для кеширования**
   - Кеширование состояния активных комнат
   - Кеширование IAM токенов для YandexGPT
   - Уменьшение нагрузки на PostgreSQL

3. **Horizontal Scaling**
   - Sticky sessions или shared session storage
   - Load balancer перед несколькими инстансами
   - Shared WordPool через Redis

4. **CDN для статики**
   - Раздача CSS/JS/изображений через CDN
   - Снижение нагрузки на app server

## Безопасность

### Текущая реализация

- Session ID в HttpOnly cookie
- Validation на всех входных данных
- SQL Injection защита через JPA
- XSS защита через Thymeleaf
- JWT с PS256 (RSA-PSS) для Yandex Cloud аутентификации
- Секретные ключи вне репозитория (в environment variables)
- Автоматическое обновление IAM токенов

### Будущие улучшения

- CSRF protection
- Rate limiting для API (особенно для LLM endpoints)
- Input sanitization
- HTTPS в production
- Secrets management (HashiCorp Vault, Yandex Lockbox)

## Асинхронность и многопоточность

### Spring @Async Configuration

**AsyncConfig** настраивает executor для асинхронных операций:

```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean(name = "wordPoolTaskExecutor")
    public Executor wordPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);           // Минимум потоков
        executor.setMaxPoolSize(5);            // Максимум потоков
        executor.setQueueCapacity(100);        // Очередь задач
        executor.setThreadNamePrefix("word-pool-refill-");
        executor.initialize();
        return executor;
    }
}
```

### Self-Injection Pattern

**Проблема**: При вызове @Async метода внутри того же класса (`this.asyncMethod()`), Spring AOP-прокси обходится и метод выполняется синхронно.

**Решение**: Self-injection с @Lazy для избежания циклической зависимости:

```java
@Component
public class WordPoolRefiller {
    private final WordPoolRefiller self;  // Внедрение самого себя
    
    public WordPoolRefiller(/* dependencies */, @Lazy WordPoolRefiller self) {
        this.self = self;
    }
    
    public void triggerAsyncRefill(String theme) {
        self.refillPoolAsync(theme);  // Вызов через proxy!
    }
    
    @Async("wordPoolTaskExecutor")
    public void refillPoolAsync(String theme) {
        // Выполняется в отдельном потоке
    }
}
```

См. [ASYNC_FIX_EXPLANATION.md](ASYNC_FIX_EXPLANATION.md) для деталей.

### Thread Safety

Все конкурентные операции безопасны:
- `ConcurrentHashMap` для theme → pool mapping в WordPool
- `ConcurrentLinkedQueue` для хранения слов
- `AtomicBoolean` для координации refill операций
- Lock-free дизайн без synchronized блоков

## LLM Integration Details

### Yandex Cloud JWT Authentication

1. **Authorized Key Loading**
   - JSON файл с service_account_id и private_key
   - Загружается при старте приложения
   - Путь: `YANDEX_GPT_AUTH_KEY_PATH`

2. **JWT Generation**
   - Алгоритм: PS256 (RSA-PSS with SHA-256)
   - Claims: iss, aud, iat, exp
   - Подпись приватным RSA ключом

3. **IAM Token Exchange**
   - POST запрос к Yandex IAM API
   - Обмен JWT на IAM token
   - Кеширование токена (valid 12 hours)
   - Auto-refresh за 10 минут до истечения

4. **API Requests**
   - Authorization: Bearer ${iamToken}
   - Model URI: gpt://${folderId}/yandexgpt-lite/latest
   - Timeout: 30 секунд

### LM Studio Integration

1. **Local API Connection**
   - HTTP клиент с настраиваемым timeout
   - URL: `http://localhost:1234` (по умолчанию)
   - Model selection через конфигурацию

2. **Request Format**
   - OpenAI-compatible API
   - Temperature control
   - Max tokens configuration
   - Custom system/user prompts

См. [YANDEX_GPT_SETUP.md](YANDEX_GPT_SETUP.md) и [LLM_BATCH_OPTIMIZATION.md](LLM_BATCH_OPTIMIZATION.md) для деталей.

