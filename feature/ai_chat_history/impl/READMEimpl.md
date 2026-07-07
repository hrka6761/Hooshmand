# Module: `:feature:ai_chat_history:impl`

Implementation for the **AI Chat History** feature.

---

## Development rules

1. **Documentation** — With every change, update this README.
2. **Documentation comments** — Every file, class, function, and public element must have KDoc.
3. **Code quality** — Follow Clean Code and SOLID principles.

---

## Gradle

```kotlin
plugins {
    alias(libs.plugins.hrka.android.library)
    alias(libs.plugins.hrka.android.lib.compose)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.hrka.android.hilt)
    alias(libs.plugins.hooshmand.feature.impl)
}
namespace = "ir.hrka.hooshmand.ai_chat_history.impl"

dependencies {
    implementation(projects.feature.aiChatHistory.api)
}
```
