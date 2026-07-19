# `:core:database`

Room persistence module for Hooshmand. Hosts the app SQLite database, entities, and DAOs.

## Contents

| Type | Visibility | Role |
|------|------------|------|
| [HooshmandDatabase] | `internal` | Room database (`hooshmand-database`) |
| [PlaceholderEntity] | public | Temporary bootstrap entity (Room requires ≥1 entity) |
| [PlaceholderDao] | public | Temporary DAO for the placeholder table |
| [DatabaseModule] / [DaosModule] | `internal` | Hilt providers |

## Dependencies

- Room (via `hrka.android.room`)
- Hilt (via `hrka.android.hilt`)

Schemas are exported under `schemas/` for AutoMigration support.

## Usage

```kotlin
implementation(projects.core.database)
```

Inject DAOs (not [HooshmandDatabase]) from Hilt. Replace the placeholder table with chat-history entities in a later step.

[HooshmandDatabase]: src/main/java/ir/hrka/database/HooshmandDatabase.kt
[PlaceholderEntity]: src/main/java/ir/hrka/database/model/PlaceholderEntity.kt
[PlaceholderDao]: src/main/java/ir/hrka/database/dao/PlaceholderDao.kt
[DatabaseModule]: src/main/java/ir/hrka/database/di/DatabaseModule.kt
