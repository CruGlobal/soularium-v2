package org.cru.soularium.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cru.soularium.domain.SessionId
import org.cru.soularium.domain.SessionKind
import org.cru.soularium.generated.resources.Res
import org.cru.soularium.generated.resources.action_back
import org.cru.soularium.generated.resources.action_cancel
import org.cru.soularium.generated.resources.action_delete
import org.cru.soularium.generated.resources.past_delete_confirm_body
import org.cru.soularium.generated.resources.past_delete_confirm_title
import org.cru.soularium.generated.resources.past_empty_bookmarked
import org.cru.soularium.generated.resources.past_empty_completed
import org.cru.soularium.generated.resources.past_kind_group
import org.cru.soularium.generated.resources.past_kind_solo
import org.cru.soularium.generated.resources.past_tab_bookmarked
import org.cru.soularium.generated.resources.past_tab_completed
import org.cru.soularium.generated.resources.past_title
import org.cru.soularium.ui.past.PastConversationItem
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless Past Conversations screen.
 *
 * Shows two tabs — Completed and Bookmarked — each containing a scrollable list
 * of session rows. A trailing delete icon on each row triggers a confirmation
 * [AlertDialog] before invoking [onDelete].
 *
 * @param completed  items to display on the Completed tab.
 * @param bookmarked items to display on the Bookmarked tab.
 * @param onOpen     called when the user taps a session row.
 * @param onDelete   called when the user confirms deletion of a session.
 * @param onBack     called when the user taps the navigation back button.
 * @param modifier   optional [Modifier] applied to the root [Scaffold].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastConversationsScreen(
    completed: List<PastConversationItem>,
    bookmarked: List<PastConversationItem>,
    onOpen: (SessionId) -> Unit,
    onDelete: (SessionId) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(Res.string.action_back)

    // Tab state: 0 = Completed, 1 = Bookmarked
    var selectedTabIndex by remember { mutableStateOf(0) }

    // Delete-confirmation dialog state — holds the id pending deletion, or null when closed.
    var pendingDeleteId by remember { mutableStateOf<SessionId?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.past_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = backLabel,
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(stringResource(Res.string.past_tab_completed))
                    },
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(stringResource(Res.string.past_tab_bookmarked))
                    },
                )
            }

            when (selectedTabIndex) {
                0 -> SessionList(
                    items = completed,
                    emptyText = stringResource(Res.string.past_empty_completed),
                    onOpen = onOpen,
                    onDeleteRequest = { pendingDeleteId = it },
                )
                else -> SessionList(
                    items = bookmarked,
                    emptyText = stringResource(Res.string.past_empty_bookmarked),
                    onOpen = onOpen,
                    onDeleteRequest = { pendingDeleteId = it },
                )
            }
        }

        // Delete-confirmation dialog
        pendingDeleteId?.let { idToDelete ->
            AlertDialog(
                onDismissRequest = { pendingDeleteId = null },
                title = {
                    Text(stringResource(Res.string.past_delete_confirm_title))
                },
                text = {
                    Text(stringResource(Res.string.past_delete_confirm_body))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDelete(idToDelete)
                            pendingDeleteId = null
                        },
                    ) {
                        Text(stringResource(Res.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteId = null }) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun SessionList(
    items: List<PastConversationItem>,
    emptyText: String,
    onOpen: (SessionId) -> Unit,
    onDeleteRequest: (SessionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = emptyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items, key = { it.sessionId.value }) { item ->
                SessionRow(
                    item = item,
                    onOpen = { onOpen(item.sessionId) },
                    onDeleteRequest = { onDeleteRequest(item.sessionId) },
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SessionRow(
    item: PastConversationItem,
    onOpen: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val soloLabel = stringResource(Res.string.past_kind_solo)
    val groupLabel = stringResource(Res.string.past_kind_group)
    val deleteLabel = stringResource(Res.string.action_delete)

    val kindLabel = if (item.kind == SessionKind.SOLO) soloLabel else groupLabel
    val kindIcon = if (item.kind == SessionKind.SOLO) Icons.Default.Person else Icons.Default.Group

    val participantsText = item.participantNames.joinToString(", ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = kindIcon,
            contentDescription = kindLabel,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.formattedDate,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (participantsText.isNotEmpty()) {
                Text(
                    text = participantsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        IconButton(onClick = onDeleteRequest) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = deleteLabel,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}
