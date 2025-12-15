# Исправление проблемы с синхронным выполнением @Async

## Проблема

При вызове метода с аннотацией `@Async` внутри того же класса (self-invocation), Spring AOP-прокси обходится, и метод выполняется **синхронно** вместо асинхронного выполнения.

### Почему это происходит?

Spring использует AOP-прокси для реализации `@Async`. Схема работы:

```
Внешний класс -> Spring Proxy -> @Async метод (выполняется асинхронно) ✅
```

Но при внутреннем вызове:

```
this.asyncMethod() -> @Async метод (выполняется СИНХРОННО!) ❌
```

Прокси обходится, потому что вызов идет напрямую через `this`, а не через Spring bean.

## Решение: Self-Injection

Используем **self-injection** - внедряем bean самого себя через конструктор с аннотацией `@Lazy`.

### Код до исправления

```java
@Component
public class WordPoolRefiller {
    
    public void triggerAsyncRefill(String theme) {
        if (shouldRefill) {
            refillPoolAsync(theme);  // ❌ Self-invocation - работает синхронно!
        }
    }
    
    @Async("wordPoolTaskExecutor")
    public void refillPoolAsync(String theme) {
        // Генерация слов...
    }
}
```

### Код после исправления

```java
@Component
public class WordPoolRefiller {
    
    private final WordPoolRefiller self;  // ✅ Добавили self-injection
    
    public WordPoolRefiller(LlmAdapterFactory llmAdapterFactory, 
                            WordPool wordPool,
                            @Lazy WordPoolRefiller self) {  // ✅ @Lazy важен!
        this.llmAdapterFactory = llmAdapterFactory;
        this.wordPool = wordPool;
        this.self = self;  // ✅ Сохраняем прокси
    }
    
    public void triggerAsyncRefill(String theme) {
        if (shouldRefill) {
            self.refillPoolAsync(theme);  // ✅ Вызов через прокси!
        }
    }
    
    @Async("wordPoolTaskExecutor")
    public void refillPoolAsync(String theme) {
        // Генерация слов...
    }
}
```

## Почему нужен @Lazy?

Аннотация `@Lazy` необходима, чтобы избежать циклической зависимости при создании bean'а:

1. Spring создает `WordPoolRefiller`
2. Для его создания нужен... `WordPoolRefiller` (self)
3. Без `@Lazy` получается deadlock

`@Lazy` говорит Spring: "Внедри прокси, а реальный bean создашь позже, когда понадобится".

## Проверка работы

### В логах теперь видно:

```
[word-pool-refill-1] INFO  WordPoolRefiller -- Async refill started for theme 'животные'
```

Обратите внимание на `[word-pool-refill-1]` - это имя отдельного потока из `wordPoolTaskExecutor`!

### Без исправления было бы:

```
[http-nio-8080-exec-1] INFO  WordPoolRefiller -- Async refill started for theme 'животные'
```

`[http-nio-8080-exec-1]` - это HTTP-поток запроса, что означает синхронное выполнение.

## Альтернативные решения

### 1. Вынести async метод в отдельный класс

```java
@Component
class AsyncExecutor {
    @Async
    public void execute() { }
}

@Component  
class MyService {
    private final AsyncExecutor asyncExecutor;
    
    public void doWork() {
        asyncExecutor.execute();  // ✅ Работает
    }
}
```

**Плюсы:** Более явная архитектура  
**Минусы:** Больше классов, может быть избыточно

### 2. ApplicationEventPublisher

```java
@Component
class MyService {
    private final ApplicationEventPublisher publisher;
    
    public void doWork() {
        publisher.publishEvent(new RefillEvent(theme));
    }
}

@Component
class RefillEventListener {
    @Async
    @EventListener
    public void handleRefill(RefillEvent event) { }
}
```

**Плюсы:** Полное разделение, event-driven архитектура  
**Минусы:** Сложнее для простых случаев

### 3. Self-injection (наш выбор)

**Плюсы:** Просто, минимум изменений, работает надежно  
**Минусы:** Может быть неочевидно для новых разработчиков

## Заключение

Self-injection - простое и эффективное решение проблемы self-invocation с `@Async`. Важно помнить:

1. Всегда используйте `@Lazy` при self-injection
2. Вызывайте async методы через `self`, а не `this`
3. Проверяйте имена потоков в логах, чтобы убедиться, что async работает

## Файлы с изменениями

- `src/main/java/com/crocodile/service/wordprovider/WordPoolRefiller.java` - добавлен self-injection
- `src/test/java/com/crocodile/service/wordprovider/WordPoolRefillerTest.java` - обновлены тесты

