package org.cru.soularium.ui.conversation

import androidx.compose.runtime.mutableStateOf
import app.cash.paparazzi.DeviceConfig
import com.android.resources.NightMode
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import org.cru.soularium.ui.test.BasePaparazziTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class ContactCollectionLayoutPaparazziTest(
    @TestParameter(valuesProvider = DeviceConfigProvider::class) deviceConfig: DeviceConfig,
    @TestParameter nightMode: NightMode,
) : BasePaparazziTest(deviceConfig = deviceConfig, nightMode = nightMode) {
    // Fresh form: only the participant's first name is seeded, every other field
    // is empty and Save is enabled.
    @Test
    fun `ContactCollectionLayout() - fresh form`() = snapshot {
        ContactCollectionLayout(state = collectingContactState())
    }

    // Every field filled in with valid values.
    @Test
    fun `ContactCollectionLayout() - filled form`() = snapshot {
        ContactCollectionLayout(
            state = collectingContactState(
                lastName = "Smith",
                email = "alice@example.com",
                phone = "(408) 555-1234",
                notes = "Met at the campus outreach event.",
            ),
        )
    }

    // Implausible phone number: the field shows its error message and Save is disabled.
    @Test
    fun `ContactCollectionLayout() - invalid phone`() = snapshot {
        ContactCollectionLayout(state = collectingContactState(phone = "1234", phoneError = true))
    }

    // Implausible email: the field shows its error message and Save is disabled.
    @Test
    fun `ContactCollectionLayout() - invalid email`() = snapshot {
        ContactCollectionLayout(
            state = collectingContactState(email = "not-an-email", emailError = true),
        )
    }
}

private fun collectingContactState(
    firstName: String = "Alice",
    lastName: String = "",
    email: String = "",
    phone: String = "",
    notes: String = "",
    emailError: Boolean = false,
    phoneError: Boolean = false,
) = ConversationPresenter.UiState.CollectingContact(
    participantIndex = 0,
    firstName = mutableStateOf(firstName),
    lastName = mutableStateOf(lastName),
    email = mutableStateOf(email),
    phone = mutableStateOf(phone),
    notes = mutableStateOf(notes),
    emailError = emailError,
    phoneError = phoneError,
    showExitDialog = false,
    eventSink = {},
)
