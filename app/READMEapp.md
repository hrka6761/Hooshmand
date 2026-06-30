# Module: `:app`

Application module — produces the installable APK and hosts the single-activity Compose shell.

---

## Development rules

1. **Documentation** — With every change, update this README when this module’s structure, APIs, or behavior change. Update the [root README](../README.md) only when project-wide structure or cross-module relationships change.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Role

- Declares `Application`/`Activity` entry points in the manifest.
- Applies global Material 3 theme (`HooshmandTheme`).
- Owns top-level navigation setup: creates `NavigationState` and `Navigator`, registers feature entry providers.
- Aggregates feature `impl` modules (currently `:feature:home:impl`).

The app module should stay thin: no feature business logic; delegate to feature modules.

---

## Gradle plugins

```kotlin
alias(libs.plugins.hrka.android.application)
alias(libs.plugins.hrka.android.app.compose)
alias(libs.plugins.hrka.android.flavors)
```

| Setting | Value |
|---------|-------|
| Namespace / applicationId | `ir.hrka.hooshmand` |
| compileSdk | 37 |
| minSdk | 33 |
| Flavors | `beta`, `prod` |
| Build types | `debug` (`.debug` suffix), `release` (R8 minify) |

---

## Dependencies

| Module / library | Purpose |
|------------------|---------|
| `:core:navigation` | Navigation state and navigator |
| `:feature:home:api` | Home nav key (for start destination) |
| `:feature:home:impl` | Home screen entry provider |
| `androidx.navigation3:navigation3-ui` | `NavDisplay`, `entryProvider` |
| Compose Material 3, Activity Compose | UI framework |

---

## Directory structure

```
app/src/main/
├── AndroidManifest.xml
├── java/ir/hrka/hooshmand/
│   ├── MainActivity.kt          # Activity entry; nav state bootstrap
│   ├── HooshmandApp.kt          # Root composable; NavDisplay + entry providers
│   ├── core/
│   │   └── Utils.kt             # App-wide CompositionLocals (e.g. Snackbar)
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt             # HooshmandTheme
│       └── Type.kt
├── res/                         # Launcher icons, themes, strings
└── keepRules/rules.keep         # ProGuard keep rules
```

---

## Key classes

| Class / function | File | Responsibility |
|------------------|------|----------------|
| `MainActivity` | `MainActivity.kt` | Edge-to-edge Activity; initializes nav with `HomeNavKey` |
| `HooshmandApp` | `HooshmandApp.kt` | Scaffold + `NavDisplay`; wires `homeEntry(navigator)` |
| `HooshmandTheme` | `ui/theme/Theme.kt` | Material 3 theme (dynamic color on API 31+) |
| `LocalSnackbarHostState` | `core/Utils.kt` | CompositionLocal for global snackbars |

---

## Navigation wiring

```kotlin
// MainActivity — start destination and top-level keys
rememberNavigationState(HomeNavKey, setOf(HomeNavKey))

// HooshmandApp — register feature entries
entryProvider {
    homeEntry(navigator)
    // future: chatEntry(navigator), …
}
```

When adding a top-level feature, update both `MainActivity` (top-level set) and `HooshmandApp` (entry provider).

---

## What does not belong here

- Feature screens and ViewModels → `feature:<name>:impl`
- Nav keys and public feature APIs → `feature:<name>:api`
- Shared navigation logic → `:core:navigation`
- AI model runtime / download → future dedicated `core` modules

---

*Last updated: initial shell with home feature wired.*
