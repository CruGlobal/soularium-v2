plugins {
    id("soularium-kmp.module-conventions")
    id("serialization-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.model"
    }

    sourceSets {
        commonTest {
            dependencies {
                implementation(libs.kotest.assertions)
            }
        }
    }
}
