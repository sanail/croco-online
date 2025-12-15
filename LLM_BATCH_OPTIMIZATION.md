# LLM Batch Generation Optimization - Implementation Summary

## Overview

Successfully implemented batch generation optimization to reduce the number of LLM API calls by using a word pool system with asynchronous refill capabilities.

## Key Changes

### 1. Extended LlmAdapter Interface

**File:** `src/main/java/com/crocodile/service/wordprovider/llm/LlmAdapter.java`

- Added `List<String> generateWords(String theme, int count)` method for batch generation
- Made `generateWord(String theme)` a default method that delegates to `generateWords(theme, 1)`
- Maintains backward compatibility

### 2. Implemented Batch Generation in LM Studio Adapter

**File:** `src/main/java/com/crocodile/service/wordprovider/llm/LmStudioLlmAdapter.java`

- Implemented `generateWords(String theme, int count)` method
- Dynamic prompt generation for batch requests
- Smart parsing of LLM responses (supports newline and comma-separated formats)
- Automatic max_tokens adjustment based on word count
- Robust error handling and logging

### 3. Implemented Batch Generation in Yandex GPT Adapter

**File:** `src/main/java/com/crocodile/service/wordprovider/llm/YandexGptLlmAdapter.java`

- Implemented `generateWords(String theme, int count)` method stub
- Documented implementation requirements for future development
- Ready for actual Yandex GPT API integration

### 4. Created WordPool Component

**File:** `src/main/java/com/crocodile/service/wordprovider/WordPool.java`

- Thread-safe word pool management using `ConcurrentHashMap` and `ConcurrentLinkedQueue`
- Separate pools per theme
- Methods for adding, retrieving, and monitoring pool state
- Configurable threshold for refill detection
- Full logging and error handling

### 5. Created AsyncConfig

**File:** `src/main/java/com/crocodile/config/AsyncConfig.java`

- Enabled Spring's `@Async` support
- Configured `ThreadPoolTaskExecutor` with:
  - Core pool size: 2 threads
  - Max pool size: 5 threads
  - Queue capacity: 100 tasks
  - Custom rejection policy (caller-runs)

### 6. Refactored AiWordProvider

**File:** `src/main/java/com/crocodile/service/wordprovider/AiWordProvider.java`

**New Architecture:**
- Tries to retrieve words from the pool first (instant response)
- Falls back to synchronous batch generation if pool is empty
- Triggers asynchronous refill when pool size drops below threshold
- Prevents duplicate refill operations using `ConcurrentHashMap<String, AtomicBoolean>`

**Key Features:**
- `generateWord(String theme)` - Main entry point, retrieves from pool
- `refillPoolAsync(String theme)` - Async method for background refill
- `triggerAsyncRefill(String theme)` - Orchestrates refill with duplicate prevention

### 7. Updated Configuration

**File:** `src/main/resources/application.yml`

Added new configuration section:
```yaml
game:
  llm:
    word-pool:
      batch-size: ${LLM_BATCH_SIZE:20}        # Words per batch request
      min-threshold: ${LLM_MIN_THRESHOLD:5}   # Trigger refill threshold
      initial-size: ${LLM_INITIAL_SIZE:10}    # Initial pool size
```

Updated system prompts to support batch generation (added instructions for list formatting).

### 8. Comprehensive Unit Tests

Created two test suites:

**WordPoolTest.java** (24 tests)
- Basic operations (add, poll, size)
- Edge cases (empty, null, blank)
- Refill detection logic
- Thread-safety tests with concurrent operations
- FIFO ordering verification

**AiWordProviderBatchTest.java** (15 tests)
- Pool retrieval scenarios
- Synchronous generation fallback
- Refill triggering logic
- Error handling (empty results, exceptions)
- Multi-theme independence
- Async refill behavior

**All 72 tests pass** (including existing tests).

## Performance Improvements

### Before Optimization
- **1 LLM API call** per word generated
- **Synchronous blocking** on every word request
- **No caching** mechanism

### After Optimization
- **1 LLM API call** generates 20 words (configurable)
- **95% reduction** in API calls (1/20 = 5%)
- **Instant response** from pool (no waiting for LLM)
- **Asynchronous refill** in background threads
- **Per-theme pools** for optimal word relevance

## Configuration Options

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `LLM_BATCH_SIZE` | 20 | Number of words to generate per batch |
| `LLM_MIN_THRESHOLD` | 5 | Pool size that triggers async refill |
| `LLM_INITIAL_SIZE` | 10 | Words to generate on first request |

### Example Configurations

**Conservative (fewer API calls, slower response):**
```yaml
batch-size: 30
min-threshold: 3
initial-size: 5
```

**Aggressive (more API calls, faster response):**
```yaml
batch-size: 10
min-threshold: 8
initial-size: 10
```

**Balanced (default):**
```yaml
batch-size: 20
min-threshold: 5
initial-size: 10
```

## Architecture Diagram

```
┌─────────────────┐
│ GameRoundService│
└────────┬────────┘
         │ generateWord(theme)
         ▼
┌─────────────────┐
│ AiWordProvider  │
└────────┬────────┘
         │
         ├─► pollWord(theme) ─────► ┌──────────────┐
         │                          │   WordPool   │
         │                          │ (per theme)  │
         │                          └──────────────┘
         │
         ├─► needsRefill? ──Yes──► triggerAsyncRefill()
         │                                │
         │                                ▼
         │                    ┌──────────────────────┐
         │                    │  @Async refillPool   │
         │                    │  (background thread) │
         │                    └──────────┬───────────┘
         │                               │
         │◄──────────────────────────────┘
         │ generateWords(theme, 20)
         ▼
┌─────────────────┐
│   LlmAdapter    │
│ (LM Studio /    │
│  Yandex GPT)    │
└────────┬────────┘
         │ HTTP Request
         ▼
┌─────────────────┐
│   LLM Service   │
└─────────────────┘
```

## Flow Scenarios

### Scenario 1: Pool has words (typical case after warmup)
1. Request comes in for theme "животные"
2. AiWordProvider polls word from pool → **instant response** ⚡
3. Check if pool needs refill (size < 5)
4. If yes, trigger async refill in background
5. User continues playing while pool refills

### Scenario 2: Pool is empty (first request or after depletion)
1. Request comes in for theme "профессии"
2. Pool is empty → generate initial batch synchronously
3. Return first word to user
4. Add remaining 9 words to pool
5. Trigger async refill to top up pool

### Scenario 3: Background refill
1. Pool drops below threshold (5 words)
2. Async task starts in background thread
3. Generate batch of 20 words via single LLM call
4. Add all 20 words to pool
5. No impact on current user requests

## Monitoring and Logging

All operations are logged for monitoring:

```
INFO: Pool for theme 'животные' needs refill. Current size: 4, threshold: 5
INFO: Async refill started for theme 'животные'
INFO: LM Studio generated 20 words for theme: животные
INFO: Async refill completed: added 20 words to pool
```

## Thread Safety

All concurrent operations are safe:
- `ConcurrentHashMap` for theme → pool mapping
- `ConcurrentLinkedQueue` for word storage
- `AtomicBoolean` for refill coordination
- No synchronized blocks needed
- Lock-free design

## Backward Compatibility

✅ Existing `generateWord()` method still works
✅ Database word provider unaffected
✅ All existing tests pass
✅ No breaking changes to API

## Future Enhancements

1. **Metrics Collection**: Track pool hit rates, refill frequency
2. **Adaptive Sizing**: Adjust batch size based on usage patterns
3. **Pre-warming**: Populate pools for common themes on startup
4. **Pool Expiration**: Implement TTL for pool entries
5. **Cross-theme Sharing**: Consider sharing pools for related themes

## Testing

```bash
# Run all tests
mvn test

# Run only new tests
mvn test -Dtest=WordPoolTest,AiWordProviderBatchTest

# With coverage
mvn test jacoco:report
```

All 72 tests pass ✅

## Conclusion

The implementation successfully reduces LLM API calls by ~95% while maintaining instant response times for users. The system is thread-safe, well-tested, and fully configurable for different deployment scenarios.

