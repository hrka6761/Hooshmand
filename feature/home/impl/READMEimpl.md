# Module: `:feature:home:impl`

Implementation of the **Home** feature: UI composables and Navigation 3 entry registration.

---

## Development rules

1. **Documentation** — With every change, update this README when screens, navigation entries, or DI setup change. Update the [root README](../../../README.md) only when the home feature’s module relationships change.
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
namespace = "ir.hrka.hooshmand.home.impl"
```

### Dependencies

| Dependency | Source |
|------------|--------|
| `:feature:home:api` | Explicit `implementation` |
| `:core:navigation` | Via `hooshmand.feature.impl` convention |
| Compose, Material 3, Navigation3, lifecycle | Via convention |
| Hilt + KSP | Via `hrka.android.hilt` |

---

## Directory structure

```
feature/home/impl/src/main/java/ir/hrka/hooshmand/home/impl/
└── HomeEntryProvider.kt    # homeEntry() extension on EntryProviderScope
```

Future expected layout as the feature grows:

```
home/impl/
├── HomeEntryProvider.kt
├── ui/                     # Composables
├── navigation/             # Sub-route keys (if not in api)
└── di/                     # Hilt modules
```

---

## Public API (for `app`)

### `homeEntry`

```kotlin
fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator)
```

Registers the composable destination for `HomeNavKey`. Called from `HooshmandApp`:

```kotlin
entryProvider {
    homeEntry(navigator)
}
```

`navigator` enables forward/back navigation to future home sub-screens or other features.

---

## Current UI

Placeholder: `Text("Home Screen")` inside `entry<HomeNavKey>`. Will evolve into the main landing surface before offline chat is added.

---

## Relationship to `:feature:home:api`

| Concern | Module |
|---------|--------|
| `HomeNavKey` | `api` |
| Screen UI, entry provider | `impl` |
| Shared types needed by other features | `api` |
| ViewModels, repositories, Hilt | `impl` (or future `core` modules) |

---

## Adding home sub-routes

1. Define new `@Serializable NavKey` in `api` (if other modules need it) or in `impl` (home-only).
2. Add `entry<SubNavKey>` blocks inside `homeEntry` or a dedicated extension.
3. Use `navigator.navigate(SubNavKey)` from home UI.
4. Update this README and `READMEapi.md` if public keys change.

---

*Last updated: stub home screen with Navigation 3 entry provider.*
