pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Hooshmand"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

include(":core:navigation")
include(":core:download_manager")
include(":core:datastore")
include(":core:llm_runtime")
include(":core:model")
include(":core:network")
include(":core:data")

include(":feature:home:api")
include(":feature:home:impl")
include(":feature:ai_chat_history:api")
include(":feature:ai_chat_history:impl")
include(":feature:ai_chat:api")
include(":feature:ai_chat:impl")