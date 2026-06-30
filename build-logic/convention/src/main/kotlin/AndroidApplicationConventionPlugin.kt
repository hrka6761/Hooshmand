import com.android.build.api.dsl.ApplicationExtension
import ir.hrka.hooshmand.convention.BuildType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.apply

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {

        apply(plugin = "com.android.application")

        extensions.configure<ApplicationExtension> {
            namespace = "ir.hrka.hooshmand"
            compileSdk = 37

            defaultConfig {
                applicationId = "ir.hrka.hooshmand"
                minSdk = 33
                targetSdk = 37
                versionCode = 1
                versionName = "1.0"

                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            buildTypes {
                debug {
                    applicationIdSuffix = BuildType.DEBUG.applicationIdSuffix
                }
                release {
                    isMinifyEnabled = providers
                        .gradleProperty("minifyWithR8")
                        .map(String::toBooleanStrict)
                        .getOrElse(true)

                    isShrinkResources = providers
                        .gradleProperty("minifyWithR8")
                        .map(String::toBooleanStrict)
                        .getOrElse(true)

                    applicationIdSuffix = BuildType.RELEASE.applicationIdSuffix
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
        }
    }
}
