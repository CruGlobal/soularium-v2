package org.cru.soularium.ui.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cru.soularium.domain.ContactInfo
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_save
import org.cru.soularium.generated.resources.action_skip_for_now
import org.cru.soularium.generated.resources.contact_email_hint
import org.cru.soularium.generated.resources.contact_first_name
import org.cru.soularium.generated.resources.contact_invalid_phone
import org.cru.soularium.generated.resources.contact_last_name
import org.cru.soularium.generated.resources.contact_notes_label
import org.cru.soularium.generated.resources.contact_phone_hint
import org.cru.soularium.generated.resources.contact_title
import org.jetbrains.compose.resources.stringResource

/**
 * Returns true if [phone] is a plausible phone number, or is blank/empty (field is optional).
 *
 * Strips spaces, dashes, parentheses, and a leading '+', then checks that the
 * remaining digits are either absent (blank entry) or in the range 7..15 digits,
 * which covers every real-world national number format without pulling in a
 * platform-specific library.
 */
fun isPhoneValid(phone: String): Boolean {
    val digits = phone.replace(Regex("[\\s\\-().+]"), "")
    return digits.isEmpty() || digits.length in 7..15
}

/**
 * Contact-collection form shown after a participant completes all five questions.
 * Lets the facilitator optionally record the participant's details for follow-up.
 */
@Composable
fun ContactCollectionLayout(state: ConversationPresenter.UiState.CollectingContact, modifier: Modifier = Modifier) {
    var firstName by remember { mutableStateOf(state.participantName) }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val phoneError = phone.isNotEmpty() && !isPhoneValid(phone)

    val scrollState = rememberScrollState()

    val saveLabel = stringResource(Res.string.action_save)
    val skipLabel = stringResource(Res.string.action_skip_for_now)
    val invalidPhoneMessage = stringResource(Res.string.contact_invalid_phone)

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
                    .verticalScroll(scrollState)
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.contact_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text(stringResource(Res.string.contact_first_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text(stringResource(Res.string.contact_last_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(Res.string.contact_email_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(stringResource(Res.string.contact_phone_hint)) },
                    singleLine = true,
                    isError = phoneError,
                    supportingText = if (phoneError) {
                        { Text(invalidPhoneMessage, color = MaterialTheme.colorScheme.error) }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(Res.string.contact_notes_label)) },
                    minLines = 3,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }

            Button(
                enabled = !phoneError,
                onClick = {
                    state.eventSink(
                        ConversationPresenter.UiEvent.CollectingContact.Save(
                            ContactInfo(
                                name = firstName,
                                surname = lastName.ifBlank { null },
                                email = email.ifBlank { null },
                                phone = phone.ifBlank { null },
                                notes = notes.ifBlank { null },
                            ),
                        ),
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = saveLabel },
            ) {
                Text(
                    text = saveLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = { state.eventSink(ConversationPresenter.UiEvent.CollectingContact.Skip) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = skipLabel },
            ) {
                Text(
                    text = skipLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
