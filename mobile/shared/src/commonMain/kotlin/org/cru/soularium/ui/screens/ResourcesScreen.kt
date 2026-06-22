package org.cru.soularium.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.resource_cru_header
import org.cru.soularium.generated.resources.resource_cru_label
import org.cru.soularium.generated.resources.resource_cru_url
import org.cru.soularium.generated.resources.resource_feedback_email
import org.cru.soularium.generated.resources.resource_feedback_header
import org.cru.soularium.generated.resources.resource_feedback_label
import org.cru.soularium.generated.resources.resource_feedback_subject
import org.cru.soularium.generated.resources.resource_mysoularium_header
import org.cru.soularium.generated.resources.resource_mysoularium_label
import org.cru.soularium.generated.resources.resource_mysoularium_url
import org.cru.soularium.generated.resources.resource_privacy_header
import org.cru.soularium.generated.resources.resource_privacy_label
import org.cru.soularium.generated.resources.resource_privacy_url
import org.cru.soularium.generated.resources.resource_terms_header
import org.cru.soularium.generated.resources.resource_terms_label
import org.cru.soularium.generated.resources.resources_title
import org.jetbrains.compose.resources.stringResource

/**
 * Sealed type representing the action triggered when a resource row is tapped.
 *
 * [OpenUrl]     — open an external web URL via [LocalUriHandler].
 * [SendEmail]   — compose an email via `mailto:` URI using [LocalUriHandler].
 * [InAppAction] — invoke an in-app callback (e.g. navigate to Terms of Use).
 */
sealed interface ResourceAction {
    data class OpenUrl(val url: String) : ResourceAction
    data class SendEmail(val address: String, val subject: String) : ResourceAction
    data class InAppAction(val invoke: () -> Unit) : ResourceAction
}

/**
 * A single row in the Resources list.
 *
 * @param header Displayed in all-caps label style above the description.
 * @param label  Short descriptive line shown below the header.
 * @param action What happens when the row is tapped.
 */
data class ResourceLink(
    val header: String,
    val label: String,
    val action: ResourceAction,
)

/**
 * Resources screen — a [LazyColumn] of [ResourceLink] rows. Each row either
 * opens an external URI (web or mailto) via [LocalUriHandler] or triggers an
 * in-app callback.
 *
 * No ViewModel. No Koin. Plain callbacks only.
 *
 * @param onBack      Called when the user taps the top-app-bar back button.
 * @param onOpenTerms Called when the user taps the Terms of Use row.
 * @param modifier    Optional modifier forwarded to the root [Scaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourcesScreen(
    onBack: () -> Unit,
    onOpenTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current

    // Resolve all string resources here so ResourceLink data class stays pure.
    val mySoulariumHeader = stringResource(Res.string.resource_mysoularium_header)
    val mySoulariumLabel = stringResource(Res.string.resource_mysoularium_label)
    val mySoulariumUrl = stringResource(Res.string.resource_mysoularium_url)

    val cruHeader = stringResource(Res.string.resource_cru_header)
    val cruLabel = stringResource(Res.string.resource_cru_label)
    val cruUrl = stringResource(Res.string.resource_cru_url)

    val feedbackHeader = stringResource(Res.string.resource_feedback_header)
    val feedbackLabel = stringResource(Res.string.resource_feedback_label)
    val feedbackEmail = stringResource(Res.string.resource_feedback_email)
    val feedbackSubject = stringResource(Res.string.resource_feedback_subject)

    val termsHeader = stringResource(Res.string.resource_terms_header)
    val termsLabel = stringResource(Res.string.resource_terms_label)

    val privacyHeader = stringResource(Res.string.resource_privacy_header)
    val privacyLabel = stringResource(Res.string.resource_privacy_label)
    val privacyUrl = stringResource(Res.string.resource_privacy_url)

    val backLabel = stringResource(Res.string.action_back)
    val screenTitle = stringResource(Res.string.resources_title)

    val resources = remember(
        mySoulariumUrl,
        cruUrl,
        feedbackEmail,
        feedbackSubject,
        privacyUrl,
        onOpenTerms,
    ) {
        listOf(
            ResourceLink(
                header = mySoulariumHeader,
                label = mySoulariumLabel,
                action = ResourceAction.OpenUrl(mySoulariumUrl),
            ),
            ResourceLink(
                header = cruHeader,
                label = cruLabel,
                action = ResourceAction.OpenUrl(cruUrl),
            ),
            ResourceLink(
                header = feedbackHeader,
                label = feedbackLabel,
                action = ResourceAction.SendEmail(
                    address = feedbackEmail,
                    subject = feedbackSubject,
                ),
            ),
            ResourceLink(
                header = termsHeader,
                label = termsLabel,
                action = ResourceAction.InAppAction(invoke = onOpenTerms),
            ),
            ResourceLink(
                header = privacyHeader,
                label = privacyLabel,
                action = ResourceAction.OpenUrl(privacyUrl),
            ),
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenTitle,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = backLabel },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                items(
                    items = resources,
                    key = { it.header },
                ) { resource ->
                    ResourceRow(
                        resource = resource,
                        onTap = {
                            when (val action = resource.action) {
                                is ResourceAction.OpenUrl -> uriHandler.openUri(action.url)
                                is ResourceAction.SendEmail -> {
                                    val mailtoUri =
                                        "mailto:${action.address}?subject=${encodeMailtoParam(action.subject)}"
                                    uriHandler.openUri(mailtoUri)
                                }
                                is ResourceAction.InAppAction -> action.invoke()
                            }
                        },
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceRow(
    resource: ResourceLink,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                onClickLabel = resource.header,
                onClick = onTap,
            )
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = resource.header,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = resource.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Minimal percent-encoding for mailto query parameter values.
 * Encodes space, +, &, =, ?, # which would otherwise break mailto URI parsing.
 */
private fun encodeMailtoParam(value: String): String =
    value
        .replace("%", "%25")
        .replace("+", "%2B")
        .replace("&", "%26")
        .replace("=", "%3D")
        .replace("?", "%3F")
        .replace("#", "%23")
        .replace(" ", "%20")
