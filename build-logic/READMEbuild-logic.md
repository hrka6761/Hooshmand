# Module: `build-logic`

Included Gradle build (`includeBuild("build-logic")`) that publishes **convention plugins** for all Hooshmand modules. Eliminates duplicated Android/Compose/Hilt/flavor configuration.

---

## Development rules

1. **Documentation** — With every change, update this README when plugin behavior, IDs, or shared build config change. Update the [root README](../README.md) when new plugins are added or module-wide conventions shift.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc (Kotlin) or JavaDoc where applicable.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Structure

```
build-logic/
├── settings.gradle.kts          # Includes :convention; imports libs.versions.toml
└── convention/
    ├── build.gradle.kts         # Registers all Gradle plugins
    └── src/main/kotlin/
        ├── Android*ConventionPlugin.kt   # Plugin entry points
        ├── HiltConventionPlugin.kt
        └── ir/hrka/hooshmand/convention/
            ├── AndroidCompose.kt        # Shared Compose BOM setup
            ├── BuildType.kt               # DEBUG / RELEASE suffixes
            ├── Flavor.kt                  # beta / prod flavors + BuildConfig URLs
            └── ProjectExtensions.kt       # `Project.libs` version catalog accessor
```

---

## Registered plugins

| Plugin ID | Class | Applied to | Configures |
|-----------|-------|------------|------------|
| `hrka.android.application` | `AndroidApplicationConventionPlugin` | `:app` | Android application, SDK 37, minSdk 33, Java 17, build types, ProGuard |
| `hrka.android.application.compose` | `AndroidApplicationComposeConventionPlugin` | `:app` | Compose compiler + BOM |
| `hrka.android.flavors` | `AndroidApplicationFlavorsConventionPlugin` | `:app` | `beta` / `prod` flavors, `BuildConfig` URL fields |
| `hrka.android.library` | `AndroidLibraryConventionPlugin` | Library modules | Android library defaults |
| `hrka.android.library.compose` | `AndroidLibraryComposeConventionPlugin` | Compose libraries | Compose on library modules |
| `hrka.android.hilt` | `HiltConventionPlugin` | Feature impl (etc.) | KSP + Hilt dependencies |
| `hooshmand.feature.api` | `AndroidFeatureApiConventionPlugin` | `feature:*:api` | Navigation3 UI + kotlinx-serialization |
| `hooshmand.feature.impl` | `AndroidFeatureImplConventionPlugin` | `feature:*:impl` | Compose, lifecycle, nav, `:core:navigation` |

Plugin IDs are declared in `gradle/libs.versions.toml` under `[plugins]`.

---

## Shared configuration details

### Application defaults (`hrka.android.application`)

- `namespace` / default `applicationId`: `ir.hrka.hooshmand`
- Debug: `applicationIdSuffix = ".debug"`
- Release: R8 minify/shrink (toggle via `minifyWithR8` in `gradle.properties`)

### Flavors (`hrka.android.flavors`)

| Flavor | ID suffix | Version suffix | `BASE_URL` |
|--------|-----------|----------------|------------|
| `beta` | `.beta` | `-beta` | `https://beta.hooshmand.com/api/v1/` |
| `prod` | — | — | `https://app.hooshmand.com/api/v1/` |

Both expose `BuildConfig.FILES_BASE_URL` for file storage base URL.

### Feature API convention

Adds to `feature:*:api`:

- `androidx.navigation3:navigation3-ui`
- `kotlinx-serialization-core`

### Feature impl convention

Adds to `feature:*:impl`:

- Compose Material 3, UI, core-ktx, lifecycle-runtime
- Navigation3 runtime + UI
- `project(":core:navigation")`

---

## Version catalog

`build-logic` reads `gradle/libs.versions.toml` via:

```kotlin
// build-logic/settings.gradle.kts
versionCatalogs {
    create("libs") {
        from(files("../gradle/libs.versions.toml"))
    }
}
```

Convention code accesses libraries through `Project.libs` (`ProjectExtensions.kt`).

---

## Adding a new convention plugin

1. Create plugin class in `convention/src/main/kotlin/`.
2. Register in `convention/build.gradle.kts` → `gradlePlugin { plugins { register(...) } }`.
3. Add alias in `gradle/libs.versions.toml`.
4. Document the plugin in this README and the root README plugin table.

---

*Last updated: eight convention plugins for app, library, compose, hilt, and feature modules.*
