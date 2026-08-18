package dev.vayou.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.vayou.core.model.PlayerPreferences
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object PlayerPreferencesSerializer : Serializer<PlayerPreferences> {

    /** Unknown keys ignored, because an installed copy has fields on disk this build has not
     *  brought back yet, and losing every other setting over one of them would be the worse trade. */
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    override val defaultValue: PlayerPreferences get() = PlayerPreferences()

    override suspend fun readFrom(input: InputStream): PlayerPreferences = try {
        jsonFormat.decodeFromString(PlayerPreferences.serializer(), input.readBytes().decodeToString())
    } catch (exception: SerializationException) {
        throw CorruptionException("Cannot read datastore", exception)
    }

    override suspend fun writeTo(t: PlayerPreferences, output: OutputStream) {
        output.write(jsonFormat.encodeToString(PlayerPreferences.serializer(), t).encodeToByteArray())
    }
}
