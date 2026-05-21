package org.cru.soularium.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.cru.soularium.data.db.entities.CardPickEntity

@Dao
interface CardPickDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(picks: List<CardPickEntity>)

    @Query(
        "SELECT * FROM card_picks WHERE conversation_id = :conversationId " +
            "ORDER BY question_number, pick_order",
    )
    suspend fun forConversation(conversationId: String): List<CardPickEntity>

    @Query(
        "DELETE FROM card_picks WHERE conversation_id = :conversationId " +
            "AND question_number = :questionNumber AND is_final = :isFinal",
    )
    suspend fun deleteForRound(conversationId: String, questionNumber: Int, isFinal: Boolean)
}
