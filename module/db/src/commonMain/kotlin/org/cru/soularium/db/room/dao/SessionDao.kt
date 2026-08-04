package org.cru.soularium.db.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.cru.soularium.db.room.entity.SessionEntity
import org.cru.soularium.model.Session

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun findSession(id: Session.Id): SessionEntity?
    @Query("SELECT * FROM sessions WHERE id = :id")
    fun findSessionFlow(id: Session.Id): Flow<SessionEntity?>

    @Upsert
    suspend fun upsert(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE ended_at IS NOT NULL ORDER BY ended_at DESC")
    fun observeCompleted(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE bookmarked_at IS NOT NULL AND ended_at IS NULL ORDER BY bookmarked_at DESC")
    fun observeBookmarked(): Flow<List<SessionEntity>>

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: Session.Id)
}
