import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("kover-conventions")
    id("ktlint-conventions")
}

kotlin {
    android {
        compileSdk = versionCatalog.findVersion("android-sdk-compile").get().requiredVersion.toInt()
        minSdk = versionCatalog.findVersion("android-sdk-min").get().requiredVersion.toInt()

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(versionCatalog.findVersion("jvm").get().requiredVersion))
        }
        withHostTest {}
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonTest {
            dependencies {
                implementation(versionCatalog.findBundle("test-framework").get())
            }
        }
    }
}
