package org.cru.soularium.ui.conversation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_add
import org.cru.soularium.generated.resources.cta_continue
import org.cru.soularium.generated.resources.participants_empty
import org.cru.soularium.generated.resources.participants_name_hint
import org.cru.soularium.generated.resources.participants_name_placeholder
import org.cru.soularium.generated.resources.participants_remove_action
import org.cru.soularium.generated.resources.participants_title
import org.jetbrains.compose.resources.stringResource

/**
 * First subscreen of the conversation flow. Shown when the presenter exposes
 * [ConversationPresenter.UiState.AddingParticipants].
 *
 * Stateless beyond the transient text-field value held locally.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddParticipantsLayout(state: ConversationPresenter.UiState.AddingParticipants, modifier: Modifier = Modifier) {
    val participantNames = state.participantNames
    var nameInput by remember { mutableStateOf("") }

    val canAdd = nameInput.isNotBlank()
    val canContinue = participantNames.isNotEmpty()

    fun submitName() {
        val trimmed = nameInput.trim()
        if (trimmed.isNotEmpty()) {
            state.eventSink(ConversationPresenter.UiEvent.AddingParticipants.AddParticipant(trimmed))
            nameInput = ""
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.participants_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Name entry row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(stringResource(Res.string.participants_name_hint)) },
                        placeholder = { Text(stringResource(Res.string.participants_name_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )

                    Button(
                        onClick = { submitName() },
                        enabled = canAdd,
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.action_add),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Participant chips or empty hint
                if (participantNames.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.participants_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        participantNames.forEachIndexed { index, name ->
                            val removeDesc = stringResource(Res.string.participants_remove_action, name)
                            FilterChip(
                                selected = false,
                                onClick = {},
                                label = { Text(text = name, style = MaterialTheme.typography.labelLarge) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            state.eventSink(
                                                ConversationPresenter.UiEvent.AddingParticipants.RemoveParticipant(
                                                    index,
                                                ),
                                            )
                                        },
                                        modifier = Modifier.semantics { contentDescription = removeDesc },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }

            Button(
                onClick = { state.eventSink(ConversationPresenter.UiEvent.AddingParticipants.Confirm) },
                enabled = canContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = stringResource(Res.string.cta_continue),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
