# `:core:network`

Remote networking layer for Hooshmand. Hosts HTTP clients and network DTOs. Does not expose domain models from `:core:model`.

## Contents

| Type | Role |
|------|------|
| [GitHubFileContent] | GitHub Contents API file envelope (`content` is Base64) |
| [ChangelogNetworkDataSource] | Public API to fetch `changelog.json` |
| [GitHubChangelogNetworkDataSource] | OkHttp implementation (no auth token) |

## Dependencies

- OkHttp
- kotlinx-serialization-json
- Hilt

No GitHub token is required for the public `Hooshmand_App_Files` repository.

## Endpoint

```
GET https://api.github.com/repos/hrka6761/Hooshmand_App_Files/contents/changelog.json
Accept: application/vnd.github.v3+json
```

## Usage

```kotlin
implementation(projects.core.network)
```

Inject [ChangelogNetworkDataSource] (Hilt) and call `getChangelogFile()`. The returned [GitHubFileContent.content] is Base64-encoded; decode and map to domain models in `:core:data`.

[GitHubFileContent]: src/main/java/ir/hrka/hooshmand/network/model/GitHubFileContent.kt
[ChangelogNetworkDataSource]: src/main/java/ir/hrka/hooshmand/network/ChangelogNetworkDataSource.kt
[GitHubChangelogNetworkDataSource]: src/main/java/ir/hrka/hooshmand/network/github/GitHubChangelogNetworkDataSource.kt
