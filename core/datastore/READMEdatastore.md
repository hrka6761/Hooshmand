# `:core:datastore`

Preferences DataStore module for typed primitive key-value storage.

## Modules

| Module | Role |
|--------|------|
| `:core:datastore` | Public [PrimitiveDataStore] API, Hilt wiring, implementation |

## Usage

1. Add the dependency:

```kotlin
implementation(projects.core.datastore)
```

2. Apply Hilt in the host module (`hrka.android.hilt`) and use `@HiltAndroidApp` in the Application class.

3. Inject [PrimitiveDataStore]:

```kotlin
class ExampleRepository @Inject constructor(
    private val primitiveDataStore: PrimitiveDataStore,
) {
    val counter = primitiveDataStore.intFlow("counter")

    suspend fun increment() {
        val current = primitiveDataStore.getInt("counter")
        primitiveDataStore.putInt("counter", current + 1)
    }
}
```

## Supported types

- `Int`, `Long`, `Float`, `Double`, `Boolean`, `String`

`Double` values are stored as raw `Long` bits under a namespaced key.

## Testing

Unit tests use an in-memory `DataStore` inside this module's `src/test` source set.

[PrimitiveDataStore]: src/main/java/ir/hrka/datastore/api/PrimitiveDataStore.kt
[PrimitivePreferencesDataSource]: src/main/java/ir/hrka/datastore/PrimitivePreferencesDataSource.kt
[DataStoreModule]: src/main/java/ir/hrka/datastore/di/DataStoreModule.kt
