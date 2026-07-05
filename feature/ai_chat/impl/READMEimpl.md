# Module: `:feature:ai_chat:impl`

Implementation of the **AI Chat** feature: UI composables and Navigation 3 entry registration.

---

## Development rules

1. **Documentation** — With every change, update this README when screens, navigation entries, or DI setup change. Update the [root README](../../../README.md) only when the ai_chat feature’s module relationships change.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Gradle

```kotlin
plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.hooshmand.feature.impl)
}
namespace = "ir.hrka.hooshmand.ai_chat.impl"
```

### Dependencies

| Dependency | Source |
|------------|--------|
| `:feature:ai_chat:api` | Explicit `implementation` |
| `:core:navigation` | Via `hooshmand.feature.impl` convention |
| Compose, Material 3, Navigation3, lifecycle | Via convention |
| Hilt + KSP | Via `hrka.android.hilt` |

---

## Directory structure

```
feature/ai_chat/impl/src/main/java/ir/hrka/hooshmand/ai_chat/impl/
└── AiChatEntryProvider.kt    # aiChatEntry() extension on EntryProviderScope
```

Future expected layout as the feature grows:

```
ai_chat/impl/
├── AiChatEntryProvider.kt
├── ui/                     # Composables
├── navigation/             # Sub-route keys (if not in api)
└── di/                     # Hilt modules
```

---

## Public API (for `app`)

### `aiChatEntry`

```kotlin
fun EntryProviderScope<NavKey>.aiChatEntry(navigator: Navigator)
```

Registers the composable destination for `AiChatNavKey`. Called from `HooshmandApp`:

```kotlin
entryProvider {
    aiChatEntry(navigator)
}
```

`navigator` enables forward/back navigation to future ai_chat sub-screens or other features.

---

## Current UI

Placeholder: `Text("AI Chat Screen")` inside `entry<AiChatNavKey>`.

---

## Relationship to `:feature:ai_chat:api`

| Concern | Module |
|---------|--------|
| `AiChatNavKey` | `api` |
| Screen UI, entry provider | `impl` |
| Shared types needed by other features | `api` |
| ViewModels, repositories, Hilt | `impl` (or future `core` modules) |

---

## Adding ai_chat sub-routes

1. Define new `@Serializable NavKey` in `api` (if other modules need it) or in `impl` (ai_chat-only).
2. Add `entry<SubNavKey>` blocks inside `aiChatEntry` or a dedicated extension.
3. Use `navigator.navigate(SubNavKey)` from ai_chat UI.
4. Update this README and `READMEapi.md` if public keys change.

---

*Last updated: stub ai_chat screen with Navigation 3 entry provider.*
