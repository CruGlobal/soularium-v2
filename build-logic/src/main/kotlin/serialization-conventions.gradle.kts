plugins {
    id("soularium-kmp.module-conventions")
    kotlin("plugin.serialization")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(versionCatalog.findLibrary("kotlinx-serialization-core").get())
            }
        }
    }
}
