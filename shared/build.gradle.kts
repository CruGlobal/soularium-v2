import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.ktlint)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "org.cru.soularium"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
        withHostTest {}
    }
    // Compose Multiplatform 1.11.x no longer publishes iosX64 binaries for several
    // artifacts (foundation, components-resources, components-ui-tooling-preview).
    // Intel-Mac iOS simulators are EOL upstream, so :shared only targets the
    // Apple-silicon simulator and device.
    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.coil.compose)
            implementation(libs.markdown.renderer)
            implementation(libs.coroutines.core)
            api(libs.room.runtime)
            implementation(libs.sqlite.bundled)
            implementation(libs.datastore.preferences.core)
            implementation(libs.okio)
            implementation(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotest.assertions)
            implementation(libs.turbine)
            implementation(libs.coroutines.test)
        }
    }
}

compose.resources {
    packageOfResClass = "org.cru.soularium.generated.resources"
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
