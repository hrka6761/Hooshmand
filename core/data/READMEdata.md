# `:core:data`

Data layer for Hooshmand. Owns repositories that map network and local sources into shared domain models from `:core:model`.

## Contents

| Type | Role |
|------|------|
| [ChangelogRepository] | Public API to load the remote changelog |
| [DefaultChangelogRepository] | Base64-decode GitHub content → parse JSON → map to [Changelog] |
| [NetworkChangelog] | DTO for decoded `changelog.json` payload |
| [ChangelogMapper] | `NetworkChangelog` → `Changelog` |
| [ChatHistoryRepository] | Public API for local chat conversations and messages |
| [DefaultChatHistoryRepository] | Room DAOs → domain [Conversation] / [ChatMessage] |
| [ConversationMapper] / [ChatMessageMapper] | Entity ↔ domain mappers |

## Dependencies

- `:core:model`
- `:core:network`
- `:core:database`
- Hilt
- kotlinx-serialization-json
- kotlinx-coroutines

## Usage

```kotlin
implementation(projects.core.data)
```

Inject [ChangelogRepository] or [ChatHistoryRepository]. Features should consume domain models, not Room entities.

[ChangelogRepository]: src/main/java/ir/hrka/hooshmand/data/repository/ChangelogRepository.kt
[DefaultChangelogRepository]: src/main/java/ir/hrka/hooshmand/data/repository/DefaultChangelogRepository.kt
[NetworkChangelog]: src/main/java/ir/hrka/hooshmand/data/model/NetworkChangelog.kt
[ChangelogMapper]: src/main/java/ir/hrka/hooshmand/data/model/ChangelogMapper.kt
[ChatHistoryRepository]: src/main/java/ir/hrka/hooshmand/data/repository/ChatHistoryRepository.kt
[DefaultChatHistoryRepository]: src/main/java/ir/hrka/hooshmand/data/repository/DefaultChatHistoryRepository.kt
[ConversationMapper]: src/main/java/ir/hrka/hooshmand/data/model/ConversationMapper.kt
[ChatMessageMapper]: src/main/java/ir/hrka/hooshmand/data/model/ChatMessageMapper.kt
[Changelog]: ../model/src/main/java/ir/hrka/hooshmand/model/Changelog.kt
[Conversation]: ../model/src/main/java/ir/hrka/hooshmand/model/Conversation.kt
[ChatMessage]: ../model/src/main/java/ir/hrka/hooshmand/model/ChatMessage.kt
