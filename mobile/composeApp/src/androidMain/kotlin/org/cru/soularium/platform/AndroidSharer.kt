package org.cru.soularium.platform

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import org.cru.soularium.domain.ports.ShareResult
import org.cru.soularium.domain.ports.Sharer

/**
 * Android [Sharer] backed by [Intent.ACTION_SEND] and a system chooser.
 *
 * The conversation summary is plain text, so [Intent.EXTRA_TEXT] carries the
 * share URL. The chooser is launched from the application context, which
 * requires [Intent.FLAG_ACTIVITY_NEW_TASK]. Android's chooser does not report
 * back whether the user completed the share, so a successful launch is
 * reported as [ShareResult.Succeeded].
 */
class AndroidSharer(
    private val context: Context,
) : Sharer {
    override suspend fun share(
        text: String,
        subject: String?,
    ): ShareResult =
        try {
            val sendIntent =
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                    if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
                }
            val chooser =
                Intent.createChooser(sendIntent, subject).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(chooser)
            ShareResult.Succeeded
        } catch (_: ActivityNotFoundException) {
            ShareResult.NoAppAvailable
        }
}
