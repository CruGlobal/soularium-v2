plugins {
    id("soularium-kmp.module-conventions")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "org.cru.soularium.model"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.serialization.json)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }
    }
}
