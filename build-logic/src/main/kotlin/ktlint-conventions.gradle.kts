plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

ktlint {
    dependencies.add("ktlintRuleset", versionCatalog.findBundle("ktlint-rulesets").get())

    // KSP (Room) and Compose Resources register generated sources as
    // Kotlin source dirs; ktlint must not lint machine-generated code.
    // Compare paths instead of materializing the build directory as a
    // FileTree — walking it races with KSP tasks writing generated
    // sources in the same build ("Could not read path .../generated/ksp/...").
    filter {
        val buildDir = layout.buildDirectory.get().asFile.toPath()
        exclude { it.file.toPath().startsWith(buildDir) }
    }
}
