# Module: `:core:navigation`

Shared **Navigation 3** infrastructure: back-stack state, navigation commands, and conversion to `NavEntry` lists for `NavDisplay`.

Used by `app` and injected into feature `impl` modules via the `hooshmand.feature.impl` convention plugin.

---

## Development rules

1. **Documentation** — With every change, update this README when public APIs or navigation behavior change. Update the [root README](../../README.md) only when this module’s role in the project graph changes.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Gradle

```kotlin
plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
}
namespace = "ir.hrka.hooshmand.navigation"
```

### Dependencies

| Library | Purpose |
|---------|---------|
| `navigation3-runtime` | `NavKey`, `NavBackStack`, `NavEntry`, decorators |
| `savedstate-compose` | Saveable state across config changes |
| `lifecycle-viewmodel-navigation3` | ViewModel store per nav entry |

---

## Directory structure

```
core/navigation/src/main/java/ir/hrka/hooshmand/navigation/
├── NavigationState.kt    # State holder + rememberNavigationState + toEntries()
└── Navigator.kt          # navigate() / goBack() commands
```

---

## Public API

### `rememberNavigationState`

```kotlin
@Composable
fun rememberNavigationState(
    startDestination: NavKey,
    topLevelDestinations: Set<NavKey>,
): NavigationState
```

Creates persisted navigation state:

- **`topLevelStack`** — ordered top-level tabs/sections.
- **`subStacks`** — one back stack per top-level destination for nested routes.

Survives configuration changes and process death via Navigation 3 saveable back stacks.

### `NavigationState`

| Property | Description |
|----------|-------------|
| `startDestination` | Root route; back from here exits the app |
| `currentTopLevelDestination` | Top of `topLevelStack` |
| `currentDestination` | Top of the active sub-stack |
| `topLevelKeys` | All registered top-level keys |

### `Navigator`

Single entry point for navigation side effects:

| Method | Behavior |
|--------|----------|
| `navigate(destination)` | Top-level key → switch tab (clears sub-stack if re-selected); sub-route → push on current sub-stack |
| `goBack()` | Pop sub-stack, or pop top-level stack, or error at start destination |

Feature entry providers receive `Navigator` to trigger navigation from UI.

### `NavigationState.toEntries`

```kotlin
@Composable
fun NavigationState.toEntries(
    entryProvider: (NavKey) -> NavEntry<NavKey>,
): SnapshotStateList<NavEntry<NavKey>>
```

Flattens top-level + sub-stacks into entries for `NavDisplay`, with saveable state and ViewModel decorators per stack.

---

## Usage pattern (from `app`)

```kotlin
val navigationState = rememberNavigationState(HomeNavKey, setOf(HomeNavKey))
val navigator = remember { Navigator(navigationState) }

NavDisplay(
    entries = navigationState.toEntries(entryProvider),
    onBack = { navigator.goBack() }
)
```

Feature impl modules define entries:

```kotlin
fun EntryProviderScope<NavKey>.homeEntry(navigator: Navigator) {
    entry<HomeNavKey> { /* composable */ }
}
```

---

## Design constraints

- Must remain **feature-agnostic** — no imports from `feature:*` or `app`.
- Only generic Navigation 3 abstractions (`NavKey`, stacks, navigator).
- New cross-cutting nav behavior (deep links, typed routes) belongs here before spreading into features.

---

*Last updated: NavigationState, Navigator, and toEntries for Navigation 3 multi-stack pattern.*
