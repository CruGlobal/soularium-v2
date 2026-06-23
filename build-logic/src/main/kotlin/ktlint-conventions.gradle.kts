plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    dependencies.add("ktlintRuleset", versionCatalog.findBundle("ktlint-rulesets").get())

    // KSP (Room) and Compose Resources register generated sources as
    // Kotlin source dirs; ktlint must not lint machine-generated code.
    filter {
        val buildDir = layout.buildDirectory.asFileTree
        exclude { it.file in buildDir }
    }
}
