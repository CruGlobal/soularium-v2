package org.cru.soularium.db.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import org.cru.soularium.db.room.entities.CardPickEntity

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
    suspend fun deleteForRound(conversationId: String, questionNumber: Int, isFinal: Boolean)

    /**
     * Replaces one round's picks atomically. The delete and the re-insert run
     * inside a single transaction, so a cancelled or failed write can never
     * leave the round with its old picks deleted and nothing reinserted.
     */
    @Transaction
    suspend fun replaceRound(
        conversationId: String,
        questionNumber: Int,
        isFinal: Boolean,
        picks: List<CardPickEntity>,
    ) {
        deleteForRound(conversationId, questionNumber, isFinal)
        upsertAll(picks)
    }
}
