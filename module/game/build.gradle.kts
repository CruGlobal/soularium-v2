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

                implementation(projects.module.analytics)
                implementation(projects.module.db)
            }
        }

        commonTest {
            dependencies {
                implementation(projects.module.db.testFixtures)

                implementation(libs.coroutines.test)
            }
        }
    }
}
