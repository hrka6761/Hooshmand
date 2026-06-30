# Module: `:feature:home:api`

Public contract for the **Home** feature. Other modules depend on this artifact to reference home navigation keys and (future) shared types — without pulling in UI or implementation details.

---

## Development rules

1. **Documentation** — With every change, update this README when public APIs change. Update the [root README](../../../README.md) only when the home feature’s place in the module graph changes.
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
namespace = "ir.hrka.hooshmand.home.api"
```

### Transitive dependencies (via convention)

- `androidx.navigation3:navigation3-ui`
- `kotlinx-serialization-core`

No Compose UI, Hilt, or `:core:navigation` — keeps the API module minimal.

---

## Directory structure

```
feature/home/api/src/main/java/ir/hrka/hooshmand/home/api/
└── HomeNavKey.kt
```

---

## Public API

### `HomeNavKey`

```kotlin
@Serializable
object HomeNavKey : NavKey
```

- Serializable navigation key for Navigation 3 (process death / saveable stacks).
- Used as **start destination** in `MainActivity`.
- Registered in top-level destination set alongside future tab keys.

---

## Consumers

| Module | Usage |
|--------|-------|
| `:app` | Start destination in `rememberNavigationState` |
| `:feature:home:impl` | `entry<HomeNavKey>` in `homeEntry` |

---

## Extension guidelines

Add to `api` only when another module needs the symbol:

- Additional `@Serializable` `NavKey` types for home sub-routes
- Public interfaces / data classes shared across features
- Navigation-related constants

Keep Composables, ViewModels, and Hilt modules in `:feature:home:impl`.

---

*Last updated: HomeNavKey as sole public type.*
