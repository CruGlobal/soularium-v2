package org.cru.soularium.ui.resources

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.resource_cru_header
import org.cru.soularium.generated.resources.resource_cru_label
import org.cru.soularium.generated.resources.resource_feedback_email
import org.cru.soularium.generated.resources.resource_feedback_header
import org.cru.soularium.generated.resources.resource_feedback_label
import org.cru.soularium.generated.resources.resource_feedback_subject
import org.cru.soularium.generated.resources.resource_mysoularium_header
import org.cru.soularium.generated.resources.resource_mysoularium_label
import org.cru.soularium.generated.resources.resource_privacy_header
import org.cru.soularium.generated.resources.resource_privacy_label
import org.cru.soularium.generated.resources.resource_terms_header
import org.cru.soularium.generated.resources.resource_terms_label
import org.cru.soularium.generated.resources.resources_title
import org.jetbrains.compose.resources.stringResource

private const val URL_MYSOULARIUM = "https://mysoularium.com"
private const val URL_CRU_SOULARIUM =
    "https://www.cru.org/us/en/train-and-grow/share-the-gospel/evangelism-principles/soularium.html"
private const val URL_PRIVACY_POLICY = "https://www.cru.org/about/privacy.html"

/**
 * Resources screen — a scrolling [Column] of tappable rows. Each row either
 * opens an external URI (web or mailto) via [LocalUriHandler] or triggers an
 * in-app callback (e.g. navigate to Terms of Use).
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(ResourcesScreen::class, AppScope::class)
@Composable
fun ResourcesLayout(state: ResourcesPresenter.UiState, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current

    val feedbackEmail = stringResource(Res.string.resource_feedback_email)
    val feedbackSubject = stringResource(Res.string.resource_feedback_subject)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.resources_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(ResourcesPresenter.UiEvent.Back) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            ResourceRow(
                header = stringResource(Res.string.resource_mysoularium_header),
                label = stringResource(Res.string.resource_mysoularium_label),
                onTap = { uriHandler.openUri(URL_MYSOULARIUM) },
            )
            ResourceDivider()
            ResourceRow(
                header = stringResource(Res.string.resource_cru_header),
                label = stringResource(Res.string.resource_cru_label),
                onTap = { uriHandler.openUri(URL_CRU_SOULARIUM) },
            )
            ResourceDivider()
            ResourceRow(
                header = stringResource(Res.string.resource_feedback_header),
                label = stringResource(Res.string.resource_feedback_label),
                onTap = {
                    uriHandler.openUri(
                        "mailto:$feedbackEmail?subject=${encodeMailtoParam(feedbackSubject)}",
                    )
                },
            )
            ResourceDivider()
            ResourceRow(
                header = stringResource(Res.string.resource_terms_header),
                label = stringResource(Res.string.resource_terms_label),
                onTap = { state.eventSink(ResourcesPresenter.UiEvent.OpenTerms) },
            )
            ResourceDivider()
            ResourceRow(
                header = stringResource(Res.string.resource_privacy_header),
                label = stringResource(Res.string.resource_privacy_label),
                onTap = { uriHandler.openUri(URL_PRIVACY_POLICY) },
            )
            ResourceDivider()
        }
    }
}

@Composable
private fun ResourceRow(header: String, label: String, onTap: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(
                onClickLabel = header,
                onClick = onTap,
            )
            .semantics { role = Role.Button }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ResourceDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/**
 * Minimal percent-encoding for mailto query parameter values.
 * Encodes space, +, &, =, ?, # which would otherwise break mailto URI parsing.
 */
private fun encodeMailtoParam(value: String): String = value
    .replace("%", "%25")
    .replace("+", "%2B")
    .replace("&", "%26")
    .replace("=", "%3D")
    .replace("?", "%3F")
    .replace("#", "%23")
    .replace(" ", "%20")
