import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi

plugins {
    id("soularium-kmp.module-conventions")
    id("dev.zacsweers.metro")
}

metro {
    @OptIn(ExperimentalMetroGradleApi::class)
    generateContributionProviders.set(true)
}
