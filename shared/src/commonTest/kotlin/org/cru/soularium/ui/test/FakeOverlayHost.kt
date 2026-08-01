package org.cru.soularium.ui.test

import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayHostData
import com.slack.circuit.overlay.OverlayNavigator
import com.slack.circuit.overlay.ReadOnlyOverlayApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel

/**
 * A fake [OverlayHost] that lets tests await shown overlays and return their result through the paired
 * [OverlayNavigator], without interacting with the overlay UI.
 */
@OptIn(ReadOnlyOverlayApi::class)
internal class FakeOverlayHost : OverlayHost {
    private val pendingOverlays = Channel<Pair<Overlay<*>, OverlayNavigator<Any>>>()

    override val currentOverlayData: OverlayHostData<Any>? = null

    override suspend fun <Result : Any> show(overlay: Overlay<Result>): Result {
        val result = CompletableDeferred<Result>()
        pendingOverlays.send(
            overlay to OverlayNavigator {
                @Suppress("UNCHECKED_CAST")
                result.complete(it as Result)
            },
        )
        return result.await()
    }

    /** Suspend until an overlay has been shown that hasn't been consumed yet, then return it with its navigator. */
    suspend fun awaitOverlayAndNavigator(): Pair<Overlay<*>, OverlayNavigator<Any>> = pendingOverlays.receive()

    /** Suspend until an overlay has been shown that hasn't been consumed yet, then return it. */
    suspend fun awaitOverlay(): Overlay<*> = awaitOverlayAndNavigator().first

    /** Suspend until an overlay has been shown that hasn't been consumed yet, then return its navigator. */
    suspend fun awaitOverlayNavigator(): OverlayNavigator<Any> = awaitOverlayAndNavigator().second
}
