package org.cru.soularium.ui.settings

import androidx.compose.ui.text.intl.Locale
import com.slack.circuit.test.FakeNavigator
import com.slack.circuit.test.test
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.ccci.gto.support.androidx.test.junit.runners.AndroidJUnit4
import org.ccci.gto.support.androidx.test.junit.runners.RunOnAndroidWith
import org.cru.soularium.domain.settings.FakeLanguageRepository

@RunOnAndroidWith(AndroidJUnit4::class)
class SettingsPresenterTest {
    private val navigator = FakeNavigator(SettingsScreen)
    private val languageRepo = FakeLanguageRepository()
    private val presenter = SettingsPresenter(navigator, languageRepo)

    @Test
    fun `UiState - selectedLanguage - reflects stored language`() = runTest {
        val locale = Locale("fr")
        languageRepo.setAppLanguage(locale)
        presenter.test {
            assertEquals(locale, awaitItem().selectedLanguage)
        }
    }

    @Test
    fun `UiState - selectedLanguage - null when no stored language`() = runTest {
        presenter.test {
            assertNull(awaitItem().selectedLanguage)
        }
    }

    @Test
    fun `UiEvent - SelectLanguage - persists and updates the selection`() = runTest {
        val locale = Locale("es")
        presenter.test {
            awaitItem().eventSink(SettingsPresenter.UiEvent.SelectLanguage(Locale("es")))
            assertEquals(locale, awaitItem().selectedLanguage)
        }
        assertEquals(locale, languageRepo.appLanguage.value)
    }

    @Test
    fun `UiEvent - Back - pops the navigator`() = runTest {
        presenter.test {
            awaitItem().eventSink(SettingsPresenter.UiEvent.Back)
            navigator.awaitPop()
        }
    }
}
