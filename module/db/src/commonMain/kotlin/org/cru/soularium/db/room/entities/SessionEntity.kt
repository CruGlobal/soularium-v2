package org.cru.soularium.db.room.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "started_at") val startedAt: Long,
    @ColumnInfo(name = "ended_at") val endedAt: Long?,
    @ColumnInfo(name = "bookmarked_at") val bookmarkedAt: Long?,
    @ColumnInfo(name = "state_snapshot_json") val stateSnapshotJson: String,
    @ColumnInfo(name = "selection_instructions_shown") val selectionInstructionsShown: Boolean,
)
