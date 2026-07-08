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
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    android {
        namespace = "org.cru.soularium"
        compileSdk = libs.versions.android.sdk.compile.get().toInt()
        minSdk = libs.versions.android.sdk.min.get().toInt()
        // Opt in to AGP's KMP Android-resources pipeline so the Compose
        // Multiplatform resources plugin can wire its
        // CopyResourcesToAndroidAssetsTask output into the published Android
        // artifact. Without this, composeResources don't ship in the APK on
        // com.android.kotlin.multiplatform.library and stringResource(...)
        // throws MissingResourceException at runtime. See CMP-9547.
        androidResources.enable = true
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            // Teach kotlin-parcelize to recognize the multiplatform-safe
            // @Parcelize annotation declared in commonMain (provided by
            // gto-support-parcelize) so Circuit Screens declared there get
            // parcelable codegen on Android.
            freeCompilerArgs.addAll(
                "-P",
                "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=" +
                    "org.ccci.gto.android.common.parcelize.Parcelize",
            )
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
            api(libs.circuit.codegen.annotations)
            implementation(libs.circuit.foundation)
            implementation(libs.circuit.runtime.presenter)
            implementation(libs.circuit.runtime.ui)
            implementation(libs.coil.compose)
            implementation(libs.gtoSupport.parcelize)
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
            implementation(libs.circuit.test)
            implementation(libs.gtoSupport.androidx.test.junit)
            implementation(libs.kotest.assertions)
            implementation(libs.turbine)
            implementation(libs.coroutines.test)
        }
        // Circuit presenter tests run the Compose Runtime on the JVM host. The
        // Compose Android artifact's error path touches android.util.Log, so we
        // need Robolectric to provide real Android stubs.
        named("androidHostTest").configure {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.robolectric)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "org.cru.soularium.generated.resources"
}

metro {
    enableCircuitCodegen.set(true)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
