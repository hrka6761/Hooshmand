import com.android.build.api.dsl.LibraryExtension
import ir.hrka.hooshmand.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<LibraryExtension> {
                dependencies {
                    "implementation"(libs.findLibrary("androidx-compose-material3").get())
                    "implementation"(libs.findLibrary("androidx-compose-ui").get())
                    "implementation"(libs.findLibrary("androidx-core-ktx").get())
                    "implementation"(libs.findLibrary("androidx-lifecycle-runtime-ktx").get())
                    "implementation"(libs.findLibrary("androidx-navigation3-ui").get())
                    "implementation"(libs.findLibrary("androidx.navigation3.runtime").get())
                    "implementation"(libs.findLibrary("androidx-compose-material-icons-extended").get())
                    "implementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
                    "implementation"(libs.findLibrary("hilt-navigation-compose").get())
                    "implementation"(project(":core:navigation"))
                }
            }
        }
    }
}
