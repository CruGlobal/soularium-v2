plugins {
    id("soularium-kmp.test-fixtures-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.game.test.fixtures"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.game)
            }
        }
    }
}
