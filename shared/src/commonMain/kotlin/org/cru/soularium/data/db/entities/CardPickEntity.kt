package org.cru.soularium.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "card_picks",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversation_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("conversation_id"),
        Index(value = ["conversation_id", "question_number"]),
    ],
)
data class CardPickEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "conversation_id") val conversationId: String,
    @ColumnInfo(name = "question_number") val questionNumber: Int,
    @ColumnInfo(name = "card_id") val cardId: Int,
    @ColumnInfo(name = "pick_order") val pickOrder: Int,
    @ColumnInfo(name = "is_final") val isFinal: Boolean,
)
