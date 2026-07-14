package org.cru.soularium.ui.resources.about

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.about_50_5
import org.cru.soularium.generated.resources.about_how_to_body
import org.cru.soularium.generated.resources.about_how_to_heading
import org.cru.soularium.generated.resources.about_meaningful_body
import org.cru.soularium.generated.resources.about_sunlight
import org.cru.soularium.generated.resources.about_tip_connections_body
import org.cru.soularium.generated.resources.about_tip_connections_title
import org.cru.soularium.generated.resources.about_tip_listening_body
import org.cru.soularium.generated.resources.about_tip_listening_title
import org.cru.soularium.generated.resources.about_tips_heading
import org.cru.soularium.generated.resources.about_title
import org.cru.soularium.generated.resources.about_what_is_body
import org.cru.soularium.generated.resources.about_what_is_heading
import org.cru.soularium.generated.resources.action_back
import org.jetbrains.compose.resources.stringResource

/**
 * Informational screen describing Soularium — what it is, how to start a
 * conversation, and helpful tips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(AboutScreen::class, AppScope::class)
@Composable
fun AboutLayout(state: AboutPresenter.UiState, modifier: Modifier = Modifier) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.about_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { state.eventSink(AboutPresenter.UiEvent.Back) },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            SectionHeading(text = stringResource(Res.string.about_what_is_heading))
            Spacer(modifier = Modifier.height(8.dp))
            SectionBody(text = stringResource(Res.string.about_what_is_body))
            Spacer(modifier = Modifier.height(16.dp))
            SectionBody(text = stringResource(Res.string.about_meaningful_body))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.about_50_5),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeading(text = stringResource(Res.string.about_how_to_heading))
            Spacer(modifier = Modifier.height(8.dp))
            SectionBody(text = stringResource(Res.string.about_how_to_body))

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeading(text = stringResource(Res.string.about_tips_heading))
            Spacer(modifier = Modifier.height(12.dp))

            TipItem(
                title = stringResource(Res.string.about_tip_listening_title),
                body = stringResource(Res.string.about_tip_listening_body),
            )

            Spacer(modifier = Modifier.height(16.dp))

            TipItem(
                title = stringResource(Res.string.about_tip_connections_title),
                body = stringResource(Res.string.about_tip_connections_body),
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(Res.string.about_sunlight),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionBody(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun TipItem(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
