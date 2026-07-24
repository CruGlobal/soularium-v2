import com.android.build.api.dsl.KotlinMultiplatformAndroidHostTestCompilation
import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
    id("soularium-kmp.module-conventions")
    id("metro-conventions")
    id("serialization-conventions")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("kotlin-parcelize")
    id("paparazzi-conventions")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "org.cru.soularium"

        androidResources.enable = true
        compilerOptions {
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=" +
                    "org.ccci.gto.android.common.parcelize.Parcelize",
            )
        }

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
                implementation(projects.module.db)
                implementation(projects.module.model)

                api(libs.circuit.codegen.annotations)
                implementation(libs.circuit.foundation)
                implementation(libs.circuit.overlay)
                implementation(libs.circuit.runtime.presenter)
                implementation(libs.circuit.runtime.ui)
                implementation(libs.circuitx.navigation)
                implementation(libs.circuitx.overlays)
                implementation(libs.coil.compose)
                implementation(libs.compose.components.resources)
                implementation(libs.compose.components.ui.tooling.preview)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.material3)
                implementation(libs.compose.runtime)
                implementation(libs.coroutines.core)
                implementation(libs.datastore.preferences.core)
                implementation(libs.gtoSupport.compose)
                implementation(libs.gtoSupport.parcelize)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.http)
                implementation(libs.markdown.renderer)
                implementation(libs.markdown.renderer.m3)
                implementation(libs.okio)
            }
        }

        androidMain {
            dependencies {
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.appcompat)
                implementation(libs.gtoSupport.androidx.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.circuit.test)
                implementation(libs.compose.ui.test)
                implementation(libs.coroutines.test)
                implementation(libs.gtoSupport.circuit.test)
                implementation(libs.kotest.assertions)
                implementation(libs.turbine)
            }
        }

        named("androidHostTest").configure {
            dependencies {
                implementation(libs.androidx.compose.ui.test.manifest)
                implementation(libs.paparazzi)
                implementation(libs.testparameterinjector)
            }
        }

        iosTest {
            dependencies {
                implementation(libs.sqlite.bundled)
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
