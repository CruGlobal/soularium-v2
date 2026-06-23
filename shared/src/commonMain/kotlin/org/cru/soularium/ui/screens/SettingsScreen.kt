package org.cru.soularium.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import kotlinx.coroutines.launch
import org.cru.soularium.domain.DeviceState
import org.cru.soularium.domain.ports.DeviceStateRepository
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.locale_en
import org.cru.soularium.generated.resources.locale_es
import org.cru.soularium.generated.resources.locale_fr
import org.cru.soularium.generated.resources.locale_pl
import org.cru.soularium.generated.resources.locale_zh_hans
import org.cru.soularium.generated.resources.settings_language
import org.cru.soularium.generated.resources.settings_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Supported app locales. Each entry carries its IETF [code] (persisted in
 * device state) and maps to its display-name string resource.
 *
 * The chosen locale is persisted via the device-state store. Runtime
 * application of a non-system locale still depends on a Compose-resources
 * locale-override API (CMP 1.8+); until then the picker records the
 * preference and reflects it in the UI.
 */
enum class AppLocale(
    val code: String,
) {
    EN("en"),
    ES("es"),
    FR("fr"),
    PL("pl"),
    ZH_HANS("zh-Hans"),
    ;

    val labelRes: StringResource
        get() =
            when (this) {
                EN -> Res.string.locale_en
                ES -> Res.string.locale_es
                FR -> Res.string.locale_fr
                PL -> Res.string.locale_pl
                ZH_HANS -> Res.string.locale_zh_hans
            }

    companion object {
        /** Maps a stored locale code back to an [AppLocale], defaulting to [EN]. */
        fun fromCode(code: String?): AppLocale = entries.firstOrNull { it.code == code } ?: EN
    }
}

class SettingsPresenter(
    private val navigator: Navigator,
    private val deviceStateRepo: DeviceStateRepository,
) : Presenter<SettingsPresenter.UiState> {

    data class UiState(
        val selectedLocale: AppLocale,
        val eventSink: (UiEvent) -> Unit,
    ) : CircuitUiState

    sealed interface UiEvent : CircuitUiEvent {
        data object Back : UiEvent
        data class SelectLocale(val locale: AppLocale) : UiEvent
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val deviceState by remember { deviceStateRepo.deviceState }.collectAsState(DeviceState())
        return UiState(
            selectedLocale = AppLocale.fromCode(deviceState.locale),
        ) { event ->
            when (event) {
                UiEvent.Back -> navigator.pop()
                is UiEvent.SelectLocale -> scope.launch { deviceStateRepo.setLocale(event.locale.code) }
            }
        }
    }
}

/**
 * Settings screen. Currently contains a single Language section that lets the
 * user select among the supported locales via a radio group.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsLayout(
    state: SettingsPresenter.UiState,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(Res.string.action_back)

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { state.eventSink(SettingsPresenter.UiEvent.Back) },
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backLabel,
                        )
                    }
                },
                colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding),
        ) {
            LanguageSection(
                selectedLocale = state.selectedLocale,
                onLocaleSelect = { state.eventSink(SettingsPresenter.UiEvent.SelectLocale(it)) },
            )
        }
    }
}

@Composable
private fun LanguageSection(
    selectedLocale: AppLocale,
    onLocaleSelect: (AppLocale) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.settings_language),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Column(modifier = Modifier.selectableGroup()) {
            AppLocale.entries.forEach { locale ->
                LocaleRow(
                    locale = locale,
                    isSelected = locale == selectedLocale,
                    onSelect = { onLocaleSelect(locale) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun LocaleRow(
    locale: AppLocale,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(locale.labelRes)

    Row(
        modifier =
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}
