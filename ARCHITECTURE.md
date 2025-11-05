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
│  │  ┌───────────┐  ┌──────────┐  ┌─────────────────┐   │  │
│  │  │GameService│  │RoomService│ │PlayerService    │   │  │
│  │  └───────────┘  └──────────┘  └─────────────────┘   │  │
│  │  ┌───────────────┐  ┌────────────────────────────┐  │  │
│  │  │SessionService │  │WordProviderFactory         │  │  │
│  │  └───────────────┘  └────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                          │                                   │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          WordProvider Strategy Pattern                │  │
│  │  ┌─────────────────┐ ┌──────────────┐ ┌──────────┐  │  │
│  │  │DatabaseProvider │ │YandexGPT(*)  │ │LMStudio(*) │  │
│  │  └─────────────────┘ └──────────────┘ └──────────┘  │  │
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

(*) Заглушки для будущей реализации
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

1. **GameService** - главный сервис игровой логики
   - Координирует взаимодействие между всеми компонентами
   - Управляет игровым процессом (угадывание, смена ведущего)
   - Сохраняет историю игр

2. **RoomService** - управление комнатами
   - Создание комнат с уникальными кодами
   - Жизненный цикл комнат
   - Scheduled задача для очистки неактивных комнат

3. **PlayerService** - управление игроками
   - Присоединение/выход игроков
   - Управление очками
   - Смена ведущего

4. **SessionService** - управление сессиями
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
1. **DatabaseWordProvider** ✅ - получение слов из БД
2. **YandexGptWordProvider** 🚧 - заглушка для YandexGPT
3. **LmStudioWordProvider** 🚧 - заглушка для LM Studio

**WordProviderFactory** - фабрика для выбора провайдера

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
    → GameService.submitGuess()
      → StringSimilarity.isCorrectGuess()
      → If correct:
        → GameHistoryRepository.save()
        → PlayerService.addWinScore()
        → PlayerService.setLeader() (change)
        → RoomService.updateRoom()
      ← GuessResponse
    ← JSON with result
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

1. **Strategy Pattern** - WordProvider для различных источников слов
2. **Factory Pattern** - WordProviderFactory для создания провайдеров
3. **DTO Pattern** - для передачи данных между слоями
4. **Repository Pattern** - Spring Data JPA
5. **Service Layer Pattern** - изоляция бизнес-логики
6. **MVC Pattern** - Spring MVC

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

### Возможные улучшения

1. **WebSocket вместо Polling**
   - Реальное двустороннее соединение
   - Снижение нагрузки на 90%

2. **Redis для кеширования**
   - Кеширование состояния активных комнат
   - Уменьшение нагрузки на PostgreSQL

3. **Horizontal Scaling**
   - Sticky sessions или shared session storage
   - Load balancer перед несколькими инстансами

4. **Асинхронная обработка**
   - Async endpoints для long-running операций
   - Event-driven architecture

## Безопасность

### Текущая реализация

- Session ID в HttpOnly cookie
- Validation на всех входных данных
- SQL Injection защита через JPA
- XSS защита через Thymeleaf

### Будущие улучшения

- CSRF protection
- Rate limiting для API
- Input sanitization
- HTTPS в production

