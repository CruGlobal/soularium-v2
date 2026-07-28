plugins {
    id("soularium-kmp.test-fixtures-conventions")
}

kotlin {
    android {
        namespace = "org.cru.soularium.db.test.fixtures"
    }

    sourceSets {
        commonMain {
            dependencies {
                api(projects.module.db)
            }
        }
    }
}
