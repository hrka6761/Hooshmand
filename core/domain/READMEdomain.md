# `:core:domain`

Domain layer for Hooshmand. Hosts use cases that combine repositories and express app business rules.

## Contents

| Type | Role |
|------|------|
| [AppUpdateStatus] | No / optional / mandatory update decision |
| [CheckAppUpdateUseCase] | Fetch changelog and compare against app `versionCode` |

## Dependencies

- `:core:data`
- `:core:model`

No Hilt `@Module` is required here; use cases are constructor-injected.

## Usage

```kotlin
implementation(projects.core.domain)
```

```kotlin
val status = checkAppUpdateUseCase(BuildConfig.VERSION_CODE)
when (status) {
    AppUpdateStatus.NoUpdate -> Unit
    is AppUpdateStatus.OptionalUpdate -> Unit
    is AppUpdateStatus.MandatoryUpdate -> Unit
}
```

[AppUpdateStatus]: src/main/java/ir/hrka/hooshmand/domain/AppUpdateStatus.kt
[CheckAppUpdateUseCase]: src/main/java/ir/hrka/hooshmand/domain/CheckAppUpdateUseCase.kt
