package org.cru.soularium.db.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import org.cru.soularium.db.room.entities.ConversationEntity

@Dao
interface ConversationDao {
    // @Upsert, not @Insert REPLACE — REPLACE would delete-and-reinsert the row,
    // cascading away child card_picks.
    @Upsert
    suspend fun upsert(conversation: ConversationEntity)

    @Upsert
    suspend fun upsertAll(conversations: List<ConversationEntity>)

    @Query("SELECT * FROM conversations WHERE session_id = :sessionId ORDER BY display_order")
    suspend fun forSession(sessionId: String): List<ConversationEntity>

    @Query("SELECT * FROM conversations WHERE session_id = :sessionId ORDER BY display_order")
    fun observeForSession(sessionId: String): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun byId(id: String): ConversationEntity?

    @Query("DELETE FROM conversations WHERE session_id = :sessionId")
    suspend fun deleteForSession(sessionId: String)

    // Drops conversations whose display_order is at or past [count] — used to
    // prune participant rows left behind when the participant list shrinks.
    // FK ON DELETE CASCADE removes each pruned conversation's card_picks too.
    @Query("DELETE FROM conversations WHERE session_id = :sessionId AND display_order >= :count")
    suspend fun deleteFrom(sessionId: String, count: Int)
}
