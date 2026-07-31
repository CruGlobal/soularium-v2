plugins {
    id("soularium-kmp.module-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.game"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.model)
            }
        }
    }
}
