import com.android.build.api.dsl.ApplicationExtension
import ir.hrka.hooshmand.convention.configureFlavors
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationFlavorsConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            extensions.configure<ApplicationExtension> {
                buildFeatures {
                    buildConfig = true
                }

                configureFlavors(this)
            }
        }
    }
}
