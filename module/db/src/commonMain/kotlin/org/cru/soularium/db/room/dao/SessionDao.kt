package org.cru.soularium.db.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.cru.soularium.db.room.entities.SessionEntity

@Dao
interface SessionDao {
    // @Upsert (not @Insert REPLACE): REPLACE deletes the existing row first,
    // which cascades to child conversations/card_picks. @Upsert updates in place.
    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun byId(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE ended_at IS NOT NULL ORDER BY ended_at DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE bookmarked_at IS NOT NULL ORDER BY bookmarked_at DESC")
    fun observeBookmarked(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)
}
