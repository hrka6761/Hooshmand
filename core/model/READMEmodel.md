# `:core:model`

Shared domain models for the Hooshmand app. Pure Kotlin types with no Android framework or networking dependencies beyond what the Android library plugin provides.

## Contents

| Type | Role |
|------|------|
| [Changelog] | Remote changelog root (`latest_version`, `minimum_version`, `versions`) |
| [VersionInfo] | Single published version entry (`version_code`, `version_name`, `changelog`) |
| [Conversation] | Persisted AI chat conversation metadata |
| [ChatMessage] | Persisted chat message |
| [ChatMessageRole] | `USER` / `MODEL` / `ERROR` |

## Usage

```kotlin
implementation(projects.core.model)
```

Other modules (`:core:data`, `:core:domain`, features) depend on this module for shared types. Do not put Retrofit/OkHttp DTOs, Room entities, or UI state here.

[Changelog]: src/main/java/ir/hrka/hooshmand/model/Changelog.kt
[VersionInfo]: src/main/java/ir/hrka/hooshmand/model/VersionInfo.kt
[Conversation]: src/main/java/ir/hrka/hooshmand/model/Conversation.kt
[ChatMessage]: src/main/java/ir/hrka/hooshmand/model/ChatMessage.kt
[ChatMessageRole]: src/main/java/ir/hrka/hooshmand/model/ChatMessageRole.kt
