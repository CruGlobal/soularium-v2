package org.cru.soularium.db.room.converter

import androidx.room.TypeConverter
import kotlinx.serialization.json.Json
import org.cru.soularium.model.game.SessionState

internal class SessionStateConverters {
    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    @TypeConverter
    fun fromSessionState(state: SessionState): String = json.encodeToString(state)

    @TypeConverter
    fun toSessionState(value: String): SessionState = json.decodeFromString(value)
}
