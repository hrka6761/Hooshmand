import androidx.room.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import ir.hrka.hooshmand.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Convention plugin for Room persistence modules.
 *
 * Applies the Room Gradle plugin and KSP, enables Kotlin code generation, exports schemas
 * under `schemas/`, and wires Room runtime / KTX / compiler dependencies.
 */
class AndroidRoomConventionPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("androidx.room")
        pluginManager.apply("com.google.devtools.ksp")

        extensions.configure<KspExtension> {
            arg("room.generateKotlin", "true")
        }

        extensions.configure<RoomExtension> {
            // Required for Room auto-migrations.
            schemaDirectory("$projectDir/schemas")
        }

        dependencies {
            "implementation"(libs.findLibrary("room.runtime").get())
            "implementation"(libs.findLibrary("room.ktx").get())
            "ksp"(libs.findLibrary("room.compiler").get())
        }
    }
}
