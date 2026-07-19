# Hooshmand

Android application for **local execution of AI models**. The first planned feature is **offline AI chat**, where models are **downloaded by the user after installation** (not bundled in the APK).

Architecture is inspired by [Now in Android](https://github.com/android/nowinandroid): modular Gradle project, feature `api`/`impl` split, convention plugins, Jetpack Compose, and Navigation 3.

---

## Development rules

1. **Documentation** — With every change, update the **module README** when that module’s structure, APIs, or behavior change. Update this **root README** only when project-wide structure, module relationships, or cross-cutting conventions change.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Navigation | Navigation 3 (`androidx.navigation3`) |
| DI | Hilt (convention plugin ready; wired in feature `impl` modules) |
| Serialization | Kotlinx Serialization (nav keys) |
| Build | Gradle 9.6, AGP 9.2, compileSdk 37, minSdk 33 |
| JDK | 17 |

---

## Project layout

```
Hooshmand/
├── app/                          # Application shell: Activity, theme, nav wiring
├── build-logic/                  # Included build — Gradle convention plugins
│   └── convention/
├── core/
│   ├── navigation/               # Shared Navigation 3 state & Navigator
│   ├── model/                    # Shared domain models (changelog, etc.)
│   ├── network/                  # HTTP clients & network DTOs
│   └── data/                     # Repositories mapping network → domain
├── feature/
│   └── home/
│       ├── api/                  # Home feature public contract (NavKey)
│       └── impl/                 # Home feature UI & entry provider
├── gradle/
│   └── libs.versions.toml        # Version catalog (dependencies & plugins)
├── settings.gradle.kts           # Module registration
└── README.md                     # This file
```

---

## Modules

| Gradle path | Purpose | Module README |
|-------------|---------|---------------|
| `:app` | APK entry point, `MainActivity`, global theme, composes feature entry providers | [READMEapp.md](app/READMEapp.md) |
| `:build-logic:convention` | Shared Gradle plugins (Android, Compose, Hilt, flavors, feature conventions) | [READMEbuild-logic.md](build-logic/READMEbuild-logic.md) |
| `:core:navigation` | `NavigationState`, `Navigator`, back-stack helpers for Navigation 3 | [READMEnavigation.md](core/navigation/READMEnavigation.md) |
| `:core:model` | Shared domain models (e.g. changelog / version info) | [READMEmodel.md](core/model/READMEmodel.md) |
| `:core:network` | HTTP clients and network DTOs (GitHub Contents API) | [READMEnetwork.md](core/network/READMEnetwork.md) |
| `:core:data` | Repositories mapping network responses to domain models | [READMEdata.md](core/data/READMEdata.md) |
| `:feature:home:api` | Home feature navigation key (`HomeNavKey`) | [READMEapi.md](feature/home/api/READMEapi.md) |
| `:feature:home:impl` | Home screen UI and `homeEntry` registration | [READMEimpl.md](feature/home/impl/READMEimpl.md) |

---

## Module dependency graph

```
                    ┌─────────────────┐
                    │      :app       │
                    └────────┬────────┘
         ┌───────────────────┼───────────────────┐
         ▼                   ▼                   ▼
┌────────────────┐  ┌───────────────┐  ┌──────────────────┐
│:core:navigation│  │:feature:home  │  │:feature:home:impl│
└────────────────┘  │     :api      │  └────────┬─────────┘
                    └───────────────┘           │
                              ▲                 │
                              └─────────────────┘
```

- **`app`** depends on all runtime modules and wires navigation entry providers.
- **`feature:*:impl`** depends on its `api` module and (via convention plugin) `:core:navigation`.
- **`feature:*:api`** stays lightweight: nav keys and public interfaces only.
- **`core:*`** modules must not depend on `feature:*` or `app`.

---

## Architecture patterns

### Feature modules (`api` + `impl`)

Each feature is split into two Gradle modules:

| Layer | Responsibility | Typical contents |
|-------|----------------|------------------|
| `api` | Stable public contract | `@Serializable` `NavKey`, navigation interfaces, domain models exposed to other modules |
| `impl` | Implementation | Composables, ViewModels, Hilt modules, `EntryProviderScope` extensions |

Other features and `app` depend only on `api`. Only `app` (or a dedicated aggregator) pulls in `impl` modules.

### Navigation 3

1. **`MainActivity`** creates `NavigationState` (start destination + top-level keys) and a `Navigator`.
2. **`HooshmandApp`** builds an `entryProvider` block that registers each feature’s entries (e.g. `homeEntry`).
3. **`NavDisplay`** renders the flattened entry list from `NavigationState.toEntries()`.

Top-level destinations use a top-level back stack; each top-level key has its own sub-stack for nested destinations.

### Build conventions

Modules apply project-specific plugins from `build-logic` instead of repeating Gradle config. See [READMEbuild-logic.md](build-logic/READMEbuild-logic.md) for plugin IDs and what each applies.

### Product flavors

| Flavor | Application ID suffix | Purpose |
|--------|----------------------|---------|
| `beta` | `.beta` | Beta environment (`BuildConfig.BASE_URL`, `FILES_BASE_URL`) |
| `prod` | — | Production environment |

Build types add `.debug` suffix on debug builds. Full matrix example: `betaDebug`, `prodRelease`.

---

## Runtime flow (current)

```
MainActivity
  └─ HooshmandTheme
       └─ rememberNavigationState(HomeNavKey, …)
       └─ Navigator(navigationState)
       └─ HooshmandApp
            └─ entryProvider { homeEntry(navigator) }
            └─ NavDisplay(entries, onBack)
                 └─ Home screen (feature/home/impl)
```

---

## Adding a new feature (checklist)

1. Create `feature/<name>/api` and `feature/<name>/impl` Gradle modules.
2. Register both in `settings.gradle.kts`.
3. Apply `hooshmand.feature.api` / `hooshmand.feature.impl` convention plugins.
4. Define `@Serializable` `NavKey`(s) in `api`.
5. Implement `EntryProviderScope` extension and UI in `impl`.
6. Add `implementation(projects.feature.<name>.api)` and `.impl` to `app/build.gradle.kts`.
7. Register the entry provider in `HooshmandApp.kt`.
8. Add the nav key to top-level destinations in `MainActivity` if it is a top-level tab/section.
9. Create/update module READMEs.

---

## Planned direction: offline AI chat

Future work will add modules (likely under `core/` for model runtime/download and `feature/chat/` for UI) for:

- Model download & storage management
- On-device inference engine integration
- Chat UI and conversation state

Keep new AI-related code in dedicated modules; avoid coupling inference logic into `app` or unrelated features.

---

## Build & run

```bash
# Debug (beta flavor example)
./gradlew :app:assembleBetaDebug

# Install on connected device
./gradlew :app:installBetaDebug
```

Requires Android SDK 37 and JDK 17. Configure `local.properties` with `sdk.dir`.

---

## Key configuration files

| File | Role |
|------|------|
| `gradle/libs.versions.toml` | Dependency and plugin versions |
| `settings.gradle.kts` | Included modules and `build-logic` |
| `gradle.properties` | Gradle JVM args, configuration cache, R8 minify flag |
| `build-logic/convention/` | Convention plugin implementations |

---

*Last updated: project bootstrap — app shell, navigation core, home feature stub.*
