import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
    alias(libs.plugins.metro)
    alias(libs.plugins.room)
    id("ktlint-conventions")
    id("paparazzi-conventions")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "org.cru.soularium"

        compileSdk = libs.versions.android.sdk.compile.get().toInt()
        minSdk = libs.versions.android.sdk.min.get().toInt()

        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=" +
                    "org.ccci.gto.android.common.parcelize.Parcelize",
            )
        }
        withHostTest {}

        compilations.withType(KotlinMultiplatformAndroidHostTestCompilation::class.java) {
            isIncludeAndroidResources = true
        }
    }

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.circuit.codegen.annotations)
                api(libs.kotlinx.serialization.json)
                api(libs.room.runtime)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.runtime)
                implementation(libs.circuit.foundation)
                implementation(libs.circuit.runtime.presenter)
                implementation(libs.circuit.runtime.ui)
                implementation(libs.coil.compose)
                implementation(libs.coroutines.core)
                implementation(libs.datastore.preferences.core)
                implementation(libs.gtoSupport.compose)
                implementation(libs.gtoSupport.parcelize)
                implementation(libs.kotlinx.datetime)
                implementation(libs.markdown.renderer)
                implementation(libs.markdown.renderer.m3)
                implementation(libs.okio)
                implementation(libs.sqlite.bundled)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.circuit.test)
                implementation(libs.coroutines.test)
                implementation(libs.gtoSupport.androidx.test.junit)
                implementation(libs.kotest.assertions)
                implementation(libs.turbine)
            }
        }

        named("androidHostTest").configure {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.paparazzi)
                implementation(libs.robolectric)
                implementation(libs.testparameterinjector)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "org.cru.soularium.generated.resources"
}

metro {
    @OptIn(ExperimentalMetroGradleApi::class)
    enableCircuitCodegen.set(true)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    kspAndroid(libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
