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
                implementation(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
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
