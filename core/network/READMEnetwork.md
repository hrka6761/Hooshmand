# `:core:network`

Remote networking layer for Hooshmand. Hosts HTTP clients and network DTOs. Does not expose domain models from `:core:model`.

## Contents

| Type | Role |
|------|------|
| [GitHubFileContent] | GitHub Contents API file envelope (`content` is Base64) |

## Dependencies

- OkHttp
- kotlinx-serialization-json

No GitHub token is required for the public `Hooshmand_App_Files` repository.

## Usage

```kotlin
implementation(projects.core.network)
```

Deserialize Contents API responses with unknown keys ignored:

```kotlin
val json = Json { ignoreUnknownKeys = true }
val file = json.decodeFromString<GitHubFileContent>(responseBody)
```

[GitHubFileContent]: src/main/java/ir/hrka/hooshmand/network/model/GitHubFileContent.kt
