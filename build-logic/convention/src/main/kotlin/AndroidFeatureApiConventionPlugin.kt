import com.android.build.api.dsl.LibraryExtension
import ir.hrka.hooshmand.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            extensions.configure<LibraryExtension> {
                dependencies {
                    "implementation"(libs.findLibrary("androidx-navigation3-ui").get())
                    "implementation"(libs.findLibrary("kotlinx-serialization-core").get())
                }
            }
        }
    }
}
