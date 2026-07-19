# `:core:database`

Room persistence module for Hooshmand. Hosts the app SQLite database, entities, and DAOs.

## Contents

| Type | Visibility | Role |
|------|------------|------|
| [HooshmandDatabase] | `internal` | Room database (`hooshmand-database`, version 2) |
| [ConversationEntity] | public | Chat conversation metadata |
| [MessageEntity] | public | Chat message row (FK → conversation, cascade delete) |
| [ConversationDao] | public | Observe / upsert / delete conversations |
| [MessageDao] | public | Observe / upsert / delete messages |
| [DatabaseModule] / [DaosModule] | `internal` | Hilt providers |

## Schema notes

- Message `role` is stored as a string: `USER`, `MODEL`, or `ERROR` (matches `AiChatMessageRole` names).
- Timestamps are epoch millis (`created_at` / `updated_at`).
- Streaming UI state is not persisted.
- Pre-release builds use destructive migration when the schema version changes.

## Dependencies

- Room (via `hrka.android.room`)
- Hilt (via `hrka.android.hilt`)

Schemas are exported under `schemas/` for AutoMigration support.

## Usage

```kotlin
implementation(projects.core.database)
```

Inject DAOs (not [HooshmandDatabase]) from Hilt.

[HooshmandDatabase]: src/main/java/ir/hrka/database/HooshmandDatabase.kt
[ConversationEntity]: src/main/java/ir/hrka/database/model/ConversationEntity.kt
[MessageEntity]: src/main/java/ir/hrka/database/model/MessageEntity.kt
[ConversationDao]: src/main/java/ir/hrka/database/dao/ConversationDao.kt
[MessageDao]: src/main/java/ir/hrka/database/dao/MessageDao.kt
[DatabaseModule]: src/main/java/ir/hrka/database/di/DatabaseModule.kt
