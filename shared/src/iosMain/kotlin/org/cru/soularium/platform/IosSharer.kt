package org.cru.soularium.platform

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.cru.soularium.domain.ports.ShareResult
import org.cru.soularium.domain.ports.Sharer
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController
import kotlin.coroutines.resume

/**
 * iOS [Sharer] backed by [UIActivityViewController].
 *
 * The controller is presented from the topmost view controller and its
 * completion handler maps to [ShareResult]: a completed activity is
 * [ShareResult.Succeeded], a dismissal is [ShareResult.Cancelled]. If no
 * window/controller is available the share is reported as
 * [ShareResult.NoAppAvailable].
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class IosSharer : Sharer {
    override suspend fun share(
        text: String,
        subject: String?,
    ): ShareResult = withContext(Dispatchers.Main) {
        val presenter = topmostViewController()
        if (presenter == null) {
            ShareResult.NoAppAvailable
        } else {
            suspendCancellableCoroutine<ShareResult> { continuation ->
                val controller =
                    UIActivityViewController(
                        activityItems = listOf(text),
                        applicationActivities = null,
                    )
                controller.setCompletionWithItemsHandler { _, completed, _, _ ->
                    continuation.resume(
                        if (completed) ShareResult.Succeeded else ShareResult.Cancelled,
                    )
                }
                presenter.presentViewController(controller, animated = true, completion = null)
            }
        }
    }
}

/** Walks the presented-controller chain from the key window's root. */
@Suppress("DEPRECATION")
private fun topmostViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}
