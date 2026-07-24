package org.cru.soularium.db.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlin.time.Instant
import org.cru.soularium.db.room.converter.SessionStateConverters
import org.cru.soularium.model.Session
import org.cru.soularium.model.game.SessionState

@Entity(tableName = "sessions")
@TypeConverters(SessionStateConverters::class)
data class SessionEntity(
    @PrimaryKey val id: Session.Id,
    @ColumnInfo(name = "kind") val kind: Session.Kind,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "ended_at") val endedAt: Instant?,
    @ColumnInfo(name = "bookmarked_at") val bookmarkedAt: Instant?,
    @ColumnInfo(name = "selection_instructions_shown") val selectionInstructionsShown: Boolean,
    @ColumnInfo(name = "state_snapshot_json") val state: SessionState,
) {
    constructor(session: Session, state: SessionState) : this(
        id = session.id,
        kind = session.kind,
        startedAt = session.startedAt,
        endedAt = session.endedAt,
        bookmarkedAt = session.bookmarkedAt,
        selectionInstructionsShown = session.selectionInstructionsShown,
        state = state,
    )

    fun toModel() = Session(
        id = id,
        kind = kind,
        startedAt = startedAt,
        endedAt = endedAt,
        bookmarkedAt = bookmarkedAt,
        selectionInstructionsShown = selectionInstructionsShown,
    )
}
