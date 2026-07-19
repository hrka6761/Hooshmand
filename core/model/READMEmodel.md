# `:core:model`

Shared domain models for the Hooshmand app. Pure Kotlin types with no Android framework or networking dependencies beyond what the Android library plugin provides.

## Contents

| Type | Role |
|------|------|
| [Changelog] | Remote changelog root (`latest_version`, `minimum_version`, `versions`) |
| [VersionInfo] | Single published version entry (`version_code`, `version_name`, `changelog`) |

## Usage

```kotlin
implementation(projects.core.model)
```

Other modules (`:core:data`, `:core:domain`, features) depend on this module for shared types. Do not put Retrofit/OkHttp DTOs or UI state here.

[Changelog]: src/main/java/ir/hrka/hooshmand/model/Changelog.kt
[VersionInfo]: src/main/java/ir/hrka/hooshmand/model/VersionInfo.kt
