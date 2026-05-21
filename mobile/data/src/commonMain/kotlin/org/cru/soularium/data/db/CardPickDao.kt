package org.cru.soularium.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import org.cru.soularium.data.db.entities.CardPickEntity

@Dao
interface CardPickDao {
    @Upsert
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
    suspend fun deleteForRound(
        conversationId: String,
        questionNumber: Int,
        isFinal: Boolean,
    )
}
