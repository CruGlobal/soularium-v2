package org.cru.soularium.ui.conversation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.slack.circuit.codegen.annotations.CircuitInject
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.Navigator
import com.slack.circuit.runtime.presenter.Presenter
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import org.cru.soularium.game.GameEngine
import org.cru.soularium.game.GameState
import org.cru.soularium.game.SessionEvent
import org.cru.soularium.game.content.Question
import org.cru.soularium.model.ContactInfo
import org.cru.soularium.model.game.SessionState
import org.cru.soularium.model.game.SessionState.InQuestion.QuestionState
import org.cru.soularium.ui.nav.ConversationScreen

@AssistedInject
class ConversationPresenter(
    @Assisted private val navigator: Navigator,
    @Assisted private val screen: ConversationScreen,
    private val gameEngineFactory: GameEngine.Factory,
) : Presenter<ConversationPresenter.UiState> {

    /**
     * One subtype per page the conversation flow can render. Each subtype carries
     * exactly the props the matching screen needs, so [ConversationLayout]'s only
     * branching is the `when` over this sealed hierarchy.
     */
    sealed interface UiState : CircuitUiState {
        val showExitDialog: Boolean
        val eventSink: (UiEvent) -> Unit

        /** Transient placeholder shown while bootstrapping or popping. */
        data class Loading(override val showExitDialog: Boolean, override val eventSink: (UiEvent) -> Unit) : UiState

        data class AddingParticipants(
            val participantNames: List<String>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class QuestionPrompt(
            val questionNumber: Int,
            val totalQuestions: Int,
            val participantName: String,
            val isGroup: Boolean,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Instructions(override val showExitDialog: Boolean, override val eventSink: (UiEvent) -> Unit) :
            UiState

        data class Selection(
            val questionNumber: Int,
            val selectedCardIds: List<Int>,
            val isConfirmEnabled: Boolean,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Finalizing(
            val questionNumber: Int,
            val cardIds: List<Int>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Discussing(
            val questionNumber: Int,
            val participantName: String,
            val cardIds: List<Int>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class Summary(
            val participants: List<ParticipantSummary>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState

        data class CollectingContact(
            val participantIndex: Int,
            val firstName: MutableState<String>,
            val lastName: MutableState<String>,
            val email: MutableState<String>,
            val phone: MutableState<String>,
            val notes: MutableState<String>,
            override val showExitDialog: Boolean,
            override val eventSink: (UiEvent) -> Unit,
        ) : UiState
    }

    /**
     * Sealed event hierarchy. Top-level entries are emittable from any page
     * (the back/exit-dialog affordances). Page-specific events are grouped into
     * nested sealed interfaces named after their owning [UiState] subtype, so
     * each subscreen has its own narrow vocabulary of events.
     */
    sealed interface UiEvent : CircuitUiEvent {
        /** Platform back / explicit exit affordance — open the bookmark/discard dialog. */
        data object RequestExit : UiEvent
        data object DismissExitDialog : UiEvent
        data object BookmarkAndExit : UiEvent
        data object DiscardAndExit : UiEvent

        sealed interface AddingParticipants : UiEvent {
            data class AddParticipant(val name: String) : AddingParticipants
            data class RemoveParticipant(val index: Int) : AddingParticipants
            data object Confirm : AddingParticipants
        }

        sealed interface QuestionPrompt : UiEvent {
            data object BeginSelection : QuestionPrompt
        }

        sealed interface Instructions : UiEvent {
            data object Dismiss : Instructions
        }

        sealed interface Selection : UiEvent {
            /** Tap a card — the presenter decides pick vs. unpick. */
            data class ToggleCard(val cardId: Int) : Selection
            data object Confirm : Selection
        }

        sealed interface Finalizing : UiEvent {
            data object Confirm : Finalizing

            /** Re-open the selection round with the current picks intact. */
            data object ChangeSelection : Finalizing
        }

        sealed interface Discussing : UiEvent {
            data object Done : Discussing
        }

        sealed interface Summary : UiEvent {
            /** Start collecting this participant's contact info. */
            data class StartCollectingContact(val participantIndex: Int) : Summary
            data object Done : Summary
        }

        sealed interface CollectingContact : UiEvent {
            data object Save : CollectingContact
            data object Skip : CollectingContact
        }
    }

    @Composable
    override fun present(): UiState {
        val scope = rememberCoroutineScope()
        val engine = remember(screen.sessionId) { gameEngineFactory.create(screen.sessionId, screen.kind) }
        DisposableEffect(engine) { onDispose { engine.close() } }

        val game by engine.state.collectAsState()
        var summaries by remember { mutableStateOf(emptyList<ParticipantSummary>()) }
        var showExitDialog by remember { mutableStateOf(false) }

        // Keyed on the collecting participant so each contact form starts fresh,
        // seeded with that participant's own name.
        val collectingIndex = (game.session as? SessionState.CollectingContact)?.participantIndex
        val contactFields = remember(collectingIndex) {
            ContactFieldStates(collectingIndex?.let { game.participantNames.getOrElse(it) { "" } }.orEmpty())
        }

        LaunchedEffect(engine) { engine.start() }

        // If we land on Summary (either fresh or via load), populate summaries.
        LaunchedEffect(game.session) {
            if (game.session == SessionState.Summary) {
                summaries = engine.loadSummaries().map {
                    ParticipantSummary(it.participantIndex, it.name, it.picks.toQuestionSelections())
                }
            }
            if (game.session == SessionState.Concluded) {
                navigator.pop()
            }
        }

        val eventSink: (UiEvent) -> Unit = { event ->
            when (event) {
                // Page-specific events
                is UiEvent.AddingParticipants.AddParticipant ->
                    engine.dispatch(SessionEvent.AddParticipant(event.name))
                is UiEvent.AddingParticipants.RemoveParticipant ->
                    engine.dispatch(SessionEvent.RemoveParticipant(event.index))
                UiEvent.AddingParticipants.Confirm ->
                    engine.dispatch(SessionEvent.ConfirmParticipants)

                UiEvent.QuestionPrompt.BeginSelection ->
                    engine.dispatch(SessionEvent.BeginSelection)

                UiEvent.Instructions.Dismiss ->
                    engine.dispatch(SessionEvent.DismissInstructions)

                is UiEvent.Selection.ToggleCard ->
                    engine.dispatch(SessionEvent.ToggleCard(event.cardId))
                UiEvent.Selection.Confirm ->
                    engine.dispatch(SessionEvent.ConfirmSelection)

                UiEvent.Finalizing.Confirm ->
                    engine.dispatch(SessionEvent.ConfirmFinal)
                UiEvent.Finalizing.ChangeSelection ->
                    engine.dispatch(SessionEvent.BeginSelection)

                UiEvent.Discussing.Done ->
                    engine.dispatch(SessionEvent.EndDiscussion)

                is UiEvent.Summary.StartCollectingContact -> {
                    val name = game.participantNames.getOrElse(event.participantIndex) { "" }
                    engine.dispatch(SessionEvent.CollectContact(event.participantIndex, ContactInfo(name)))
                }
                UiEvent.Summary.Done ->
                    engine.dispatch(SessionEvent.Conclude)

                UiEvent.CollectingContact.Save -> {
                    val current = game.session as? SessionState.CollectingContact
                    if (current != null) {
                        engine.dispatch(
                            SessionEvent.CollectContact(current.participantIndex, contactFields.toContactInfo()),
                        )
                    }
                }
                UiEvent.CollectingContact.Skip ->
                    engine.dispatch(SessionEvent.SkipContact)

                // Global events
                UiEvent.RequestExit -> when {
                    game.session == SessionState.Concluded -> Unit

                    // Nothing meaningful entered yet — there is no progress to
                    // bookmark, so back out directly and drop the empty session.
                    game.session == SessionState.AddingParticipants && game.participantNames.isEmpty() ->
                        scope.launch {
                            engine.discard()
                            navigator.pop()
                        }

                    else -> showExitDialog = true
                }
                UiEvent.DismissExitDialog -> showExitDialog = false
                UiEvent.BookmarkAndExit -> {
                    showExitDialog = false
                    scope.launch {
                        engine.bookmark()
                        navigator.pop()
                    }
                }
                UiEvent.DiscardAndExit -> {
                    showExitDialog = false
                    scope.launch {
                        engine.discard()
                        navigator.pop()
                    }
                }
            }
        }

        return buildUiState(game, summaries, contactFields, showExitDialog, eventSink)
    }

    /**
     * Snapshot-backed contact form fields. The Layout writes into these directly
     * as the user types — no event roundtrip — and [toContactInfo] reads them
     * when the form is saved.
     */
    private class ContactFieldStates(participantName: String) {
        val firstName = mutableStateOf(participantName)
        val lastName = mutableStateOf("")
        val email = mutableStateOf("")
        val phone = mutableStateOf("")
        val notes = mutableStateOf("")

        fun toContactInfo() = ContactInfo(
            name = firstName.value,
            surname = lastName.value.ifBlank { null },
            email = email.value.ifBlank { null },
            phone = phone.value.ifBlank { null },
            notes = notes.value.ifBlank { null },
        )
    }

    /**
     * Projects the engine's [GameState] onto the page-specific [UiState]
     * subtype. All the branching that used to live in the Layout (question
     * lookup, participant name resolution, round numbering, selection-count
     * validity) is resolved here.
     */
    private fun buildUiState(
        game: GameState,
        summaries: List<ParticipantSummary>,
        contactFields: ContactFieldStates,
        showExitDialog: Boolean,
        eventSink: (UiEvent) -> Unit,
    ): UiState = when (val sessionState = game.session) {
        SessionState.NotStarted, SessionState.Concluded ->
            UiState.Loading(showExitDialog, eventSink)

        SessionState.AddingParticipants ->
            UiState.AddingParticipants(game.participantNames, showExitDialog, eventSink)

        is SessionState.InQuestion -> {
            val question = Question.forNumber(sessionState.questionNumber)
            val participantName =
                game.participantNames.getOrElse(sessionState.activeParticipantIndex) { "" }
            when (sessionState.activity) {
                QuestionState.ShowingPrompt ->
                    UiState.QuestionPrompt(
                        questionNumber = sessionState.questionNumber,
                        totalQuestions = Question.entries.size,
                        participantName = participantName,
                        isGroup = game.participantNames.size > 1,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionState.ShowingInstructions ->
                    UiState.Instructions(showExitDialog, eventSink)

                QuestionState.Selecting ->
                    UiState.Selection(
                        questionNumber = sessionState.questionNumber,
                        selectedCardIds = game.draftPicks,
                        isConfirmEnabled = game.draftPicks.size == question.requiredImageCount,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionState.Finalizing ->
                    UiState.Finalizing(
                        questionNumber = sessionState.questionNumber,
                        cardIds = game.draftPicks,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )

                QuestionState.Discussing ->
                    UiState.Discussing(
                        questionNumber = sessionState.questionNumber,
                        participantName = participantName,
                        cardIds = game.draftPicks,
                        showExitDialog = showExitDialog,
                        eventSink = eventSink,
                    )
            }
        }

        SessionState.Summary ->
            UiState.Summary(summaries, showExitDialog, eventSink)

        is SessionState.CollectingContact ->
            UiState.CollectingContact(
                participantIndex = sessionState.participantIndex,
                firstName = contactFields.firstName,
                lastName = contactFields.lastName,
                email = contactFields.email,
                phone = contactFields.phone,
                notes = contactFields.notes,
                showExitDialog = showExitDialog,
                eventSink = eventSink,
            )
    }

    @CircuitInject(ConversationScreen::class, AppScope::class)
    @AssistedFactory
    fun interface Factory {
        fun create(navigator: Navigator, screen: ConversationScreen): ConversationPresenter
    }
}
