package org.cru.soularium.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.slack.circuit.codegen.annotations.CircuitInject
import dev.zacsweers.metro.AppScope
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.settings_language
import org.cru.soularium.generated.resources.settings_title
import org.cru.soularium.ui.settings.SettingsPresenter.UiState
import org.cru.soularium.ui.util.getDisplayName
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@CircuitInject(SettingsScreen::class, AppScope::class)
@Composable
fun SettingsLayout(state: UiState, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(Res.string.settings_title))
                },
                navigationIcon = {
                    IconButton(onClick = { state.eventSink(SettingsPresenter.UiEvent.Back) }) {
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
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
        ) {
            LanguageSection(
                state,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun LanguageSection(state: UiState, modifier: Modifier = Modifier) = Column(modifier = modifier) {
    Text(
        text = stringResource(Res.string.settings_language),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )

    state.supportedLanguages.forEach { locale ->
        key(locale) {
            ListItem(
                selected = locale == state.selectedLanguage,
                onClick = { state.eventSink(SettingsPresenter.UiEvent.SelectLanguage(locale)) },
            ) {
                Text(text = locale.getDisplayName(locale))
            }
        }
    }
}
