# `:core:data`

Data layer for Hooshmand. Owns repositories that turn network responses into shared domain models from `:core:model`.

## Contents

| Type | Role |
|------|------|
| [ChangelogRepository] | Public API to load the remote changelog |
| [DefaultChangelogRepository] | Base64-decode GitHub content → parse JSON → map to [Changelog] |
| [NetworkChangelog] | Dec for decoded `changelog.json` payload |
| [ChangelogMapper] | `NetworkChangelog` → `Changelog` |

## Dependencies

- `:core:model`
- `:core:network`
- Hilt
- kotlinx-serialization-json

## Usage

```kotlin
implementation(projects.core.data)
```

Inject [ChangelogRepository] and call `getChangelog()`.

[ChangelogRepository]: src/main/java/ir/hrka/hooshmand/data/repository/ChangelogRepository.kt
[DefaultChangelogRepository]: src/main/java/ir/hrka/hooshmand/data/repository/DefaultChangelogRepository.kt
[NetworkChangelog]: src/main/java/ir/hrka/hooshmand/data/model/NetworkChangelog.kt
[ChangelogMapper]: src/main/java/ir/hrka/hooshmand/data/model/ChangelogMapper.kt
[Changelog]: ../model/src/main/java/ir/hrka/hooshmand/model/Changelog.kt
