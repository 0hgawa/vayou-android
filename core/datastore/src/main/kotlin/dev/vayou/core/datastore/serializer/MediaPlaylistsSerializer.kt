package dev.vayou.core.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import dev.vayou.core.model.MediaPlaylists
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

object MediaPlaylistsSerializer : Serializer<MediaPlaylists> {

    /** Unknown keys ignored, so a document written by a build with a field this one has not
     *  brought back yet still opens, rather than taking every playlist with it. */
    private val jsonFormat = Json { ignoreUnknownKeys = true }

    override val defaultValue: MediaPlaylists get() = MediaPlaylists()

    override suspend fun readFrom(input: InputStream): MediaPlaylists = try {
        jsonFormat.decodeFromString(MediaPlaylists.serializer(), input.readBytes().decodeToString())
    } catch (exception: SerializationException) {
        throw CorruptionException("Cannot read datastore", exception)
    }

    override suspend fun writeTo(t: MediaPlaylists, output: OutputStream) {
        output.write(jsonFormat.encodeToString(MediaPlaylists.serializer(), t).encodeToByteArray())
    }
}
