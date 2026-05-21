package org.cru.soularium.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import soularium.composeapp.generated.resources.Res
import soularium.composeapp.generated.resources.action_agree
import soularium.composeapp.generated.resources.action_back
import soularium.composeapp.generated.resources.terms_confirm_prompt
import soularium.composeapp.generated.resources.terms_review_note
import soularium.composeapp.generated.resources.terms_title

/**
 * Terms of Use gate shown after the intro flow. Displays the terms summary and
 * requires the user to tap Agree before accessing Home.
 *
 * @param onAgree called when the user taps the Agree button. The caller is
 *                responsible for persisting agreement and navigating to Home.
 * @param onBack  called when the user taps the Back button.
 */
@Composable
fun TermsScreen(
    onAgree: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val agreeLabel = stringResource(Res.string.action_agree)
    val backLabel = stringResource(Res.string.action_back)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(top = 48.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.terms_title),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = stringResource(Res.string.terms_confirm_prompt),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.terms_review_note),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Button(
                onClick = onAgree,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = agreeLabel },
            ) {
                Text(
                    text = agreeLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onBack,
                modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .semantics { contentDescription = backLabel },
            ) {
                Text(
                    text = backLabel,
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
