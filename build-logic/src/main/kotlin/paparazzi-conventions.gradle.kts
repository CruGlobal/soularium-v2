plugins {
    id("app.cash.paparazzi")
}

// HACK: Paparazzi (layoutlib) and Robolectric can't share a JVM, so Paparazzi tests are
//       excluded from testAndroidHostTest unless -Ppaparazzi is passed. See cashapp/paparazzi#1979.
val paparazziEnabled = project.hasProperty("paparazzi")
val paparazziCategory = "org.cru.soularium.ui.test.BasePaparazziTest"

tasks.withType<Test> {
    if (name != "testAndroidHostTest") return@withType
    useJUnit {
        if (paparazziEnabled) includeCategories(paparazziCategory) else excludeCategories(paparazziCategory)
    }
}

// HACK: cleanRecordPaparazzi's delete tasks resolve a provider tied to KSP-generated
//       host-test sources, which fails if queried before kspAndroidHostTest runs.
pluginManager.withPlugin("com.google.devtools.ksp") {
    val deleteSnapshotsTask = Regex("^delete.*PaparazziSnapshots$")
    tasks.matching { it.name.matches(deleteSnapshotsTask) }.configureEach {
        mustRunAfter("kspAndroidHostTest")
    }
}
