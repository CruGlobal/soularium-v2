package org.cru.soularium.di

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Exposes the app-wide [SoulariumAppGraph] to composition. Provided once at the App root so
 * composables can reach graph-scoped dependencies without threading them through every call site.
 * Use [isProvided] to check availability before reading [current] where the graph may be absent
 * (e.g. previews or screenshot tests).
 */
object LocalSoulariumAppGraph {
    private val LocalGraph = staticCompositionLocalOf<SoulariumAppGraph?> { null }

    /** The provided [SoulariumAppGraph]. Throws if none has been provided. */
    val current: SoulariumAppGraph
        @Composable
        get() = LocalGraph.current ?: error("LocalSoulariumAppGraph was not provided")

    /** Whether a [SoulariumAppGraph] has been provided to the current composition. */
    val isProvided: Boolean
        @Composable
        get() = LocalGraph.current != null

    @Composable
    infix fun provides(graph: SoulariumAppGraph) = LocalGraph.provides(graph)
}
