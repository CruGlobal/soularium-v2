plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.ktlint.gradle)
}

ktlint {
    filter {
        val buildDir = layout.buildDirectory.asFileTree
        exclude { it.file in buildDir }
    }
}
