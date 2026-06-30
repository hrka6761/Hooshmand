package ir.hrka.hooshmand.convention

import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

enum class FlavorDimension {
    Environment,
}

enum class Flavor(
    val dimension: FlavorDimension,
    val applicationIdSuffix: String? = null,
    val versionNameSuffix: String? = null,
    val baseUrl: String,
    val filesBaseUrl: String,
) {
    Beta(
        dimension = FlavorDimension.Environment,
        applicationIdSuffix = ".beta",
        versionNameSuffix = "-beta",
        baseUrl = "https://beta.hooshmand.com/api/v1/",
        filesBaseUrl = "https://beta.hooshmand.com/storage/",
    ),
    Prod(
        dimension = FlavorDimension.Environment,
        baseUrl = "https://app.hooshmand.com/api/v1/",
        filesBaseUrl = "https://app.hooshmand.com/storage/",
    ),
}

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: Flavor) -> Unit = {}
) {
    commonExtension.apply {
        FlavorDimension.entries.forEach { dimension ->
            flavorDimensions += dimension.name
        }

        productFlavors {
            Flavor.entries.forEach { flavor ->
                create(flavor.name.lowercase()) {
                    dimension = flavor.dimension.name
                    flavorConfigurationBlock(this, flavor)
                    if (this is ApplicationProductFlavor) {
                        if (flavor.applicationIdSuffix != null) {
                            applicationIdSuffix = flavor.applicationIdSuffix
                        }
                        if (flavor.versionNameSuffix != null) {
                            versionNameSuffix = flavor.versionNameSuffix
                        }
                    }

                    buildConfigField("String", "BASE_URL", "\"${flavor.baseUrl}\"")
                    buildConfigField("String", "FILES_BASE_URL", "\"${flavor.filesBaseUrl}\"")
                }
            }
        }
    }
}
