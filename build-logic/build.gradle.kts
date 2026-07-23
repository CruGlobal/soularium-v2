plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
}

dependencies {
    implementation(libs.android.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.kotlin.serialization.gradle)
    implementation(libs.kover.gradle)
    implementation(libs.ktlint.gradle)
    implementation(libs.metro.gradle)
    implementation(libs.paparazzi.gradle)
}

ktlint {
    filter {
        val buildDir = layout.buildDirectory.asFileTree
        exclude { it.file in buildDir }
    }
}
