plugins {
    id("soularium-kmp.module-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.db"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.model)
                api(libs.coroutines.core)
            }
        }
    }
}
