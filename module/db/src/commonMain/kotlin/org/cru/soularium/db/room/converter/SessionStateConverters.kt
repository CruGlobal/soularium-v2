package org.cru.soularium.db.room.converter

import androidx.room.TypeConverter
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.cru.soularium.model.game.SessionState

internal class SessionStateConverters {
    private companion object {
        private val json = Json { ignoreUnknownKeys = true }
    }

    @TypeConverter
    fun fromSessionState(state: SessionState?): String? = state?.let { json.encodeToString(it) }

    // An unreadable snapshot (corruption, or a value written by a different
    // build of the state hierarchy) decodes to null rather than failing every
    // query that materializes the session row.
    @TypeConverter
    fun toSessionState(value: String?): SessionState? = value?.let {
        try {
            json.decodeFromString<SessionState>(it)
        } catch (_: SerializationException) {
            null
        } catch (_: IllegalArgumentException) {
            null
        }
    }
}
