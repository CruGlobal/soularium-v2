plugins {
    id("soularium-kmp.module-conventions")
    id("metro-conventions")
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    compilerOptions {
        // Room KMP's @ConstructedBy generates an actual for an expect object.
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        namespace = "org.cru.soularium.db"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.model)

                api(libs.coroutines.core)
                api(libs.room.runtime)
                implementation(libs.gtoSupport.androidx.room)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
                implementation(libs.turbine)
            }
        }

        iosMain {
            dependencies {
                implementation(libs.sqlite.bundled)
            }
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    kspAndroid(libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

// TODO: temporary workaround — AGP's androidHostTest lint tasks (LintModelWriterTask,
//       AndroidLintAnalysisTask) consume the KSP-generated androidHostTest sources without
//       declaring a dependency on the task that produces them, failing Gradle's
//       implicit-dependency validation whenever both end up in the same task graph. Remove
//       once KSP supports the KMP library plugin (https://github.com/google/ksp/issues/2476).
//       Reproducer: ./gradlew :module:db:lintAnalyzeAndroidHostTest :module:db:kspAndroidHostTest
tasks.matching { it.name == "generateAndroidHostTestLintModel" || it.name == "lintAnalyzeAndroidHostTest" }
    .configureEach { dependsOn("kspAndroidHostTest") }
