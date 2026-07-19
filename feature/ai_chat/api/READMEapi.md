# Module: `:feature:ai_chat:api`

Public contract for the **AI Chat** feature. Other modules depend on this artifact to reference ai_chat navigation keys and (future) shared types — without pulling in UI or implementation details.

---

## Development rules

1. **Documentation** — With every change, update this README when public APIs change. Update the [root README](../../../README.md) only when the ai_chat feature’s place in the module graph changes.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Gradle

```kotlin
plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hooshmand.feature.api)
}
namespace = "ir.hrka.hooshmand.ai_chat.api"
```

### Transitive dependencies (via convention)

- `androidx.navigation3:navigation3-ui`
- `kotlinx-serialization-core`

No Compose UI, Hilt, or `:core:navigation` — keeps the API module minimal.

---

## Directory structure

```
feature/ai_chat/api/src/main/java/ir/hrka/hooshmand/ai_chat/api/
└── AiChatNavKey.kt
```

---

## Public API

### `AiChatNavKey`

```kotlin
@Serializable
data class AiChatNavKey(val conversationId: String) : NavKey
```

- Serializable navigation key for Navigation 3 (process death / saveable stacks).
- `conversationId` identifies which conversation to open or create.

---

## Consumers

| Module | Usage |
|--------|-------|
| `:feature:ai_chat_history:impl` | Opens chat with a conversation id (list row / New Chat) |
| `:feature:ai_chat:impl` | `entry<AiChatNavKey>` in `aiChatEntry` |

---

## Extension guidelines

Add to `api` only when another module needs the symbol:

- Additional `@Serializable` `NavKey` types for ai_chat sub-routes
- Public interfaces / data classes shared across features
- Navigation-related constants

Keep Composables, ViewModels, and Hilt modules in `:feature:ai_chat:impl`.

---

*Last updated: AiChatNavKey carries conversationId.*
