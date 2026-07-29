plugins {
    id("soularium-kmp.module-conventions")
    id("metro-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.game"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.model)
                api(libs.coroutines.core)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.coroutines.test)
                implementation(libs.turbine)
            }
        }
    }
}
