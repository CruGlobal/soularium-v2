import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    id("ktlint-conventions")
}

android {
    namespace = "org.cru.soularium.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.cru.soularium"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "2.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildTypes {
        debug { applicationIdSuffix = ".dev" }
        release {
            isMinifyEnabled = false
        }
    }
}

// Workaround: Compose Multiplatform 1.11.x does not auto-wire its prepared
// composeResources into the Android assets pipeline when :shared uses AGP 9's
// com.android.kotlin.multiplatform.library plugin. Stage the prepared resources
// into a local directory and feed it to the Android variant's assets.
abstract class StageComposeAssets : Sync() {
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        into(outputDir)
    }
}

val stageComposeAssets = tasks.register<StageComposeAssets>("stageSharedComposeAssets") {
    dependsOn(":shared:prepareComposeResourcesTaskForCommonMain")
    // The runtime resource reader looks them up under
    // composeResources/<packageOfResClass>/..., so re-root the prepared
    // commonMain composeResources/ tree into that namespaced directory.
    from(
        project(":shared").layout.buildDirectory.dir(
            "generated/compose/resourceGenerator/preparedResources/commonMain/composeResources",
        ),
    ) {
        into("composeResources/org.cru.soularium.generated.resources")
    }
    outputDir.set(layout.buildDirectory.dir("generated/composeResources"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            stageComposeAssets,
            StageComposeAssets::outputDir,
        )
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
}
