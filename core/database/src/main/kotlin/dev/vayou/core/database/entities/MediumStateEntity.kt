package dev.vayou.core.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_state",
    indices = [
        Index(value = ["uri"], unique = true),
    ],
)
data class MediumStateEntity(
    @PrimaryKey
    @ColumnInfo(name = "uri")
    val uriString: String,
    @ColumnInfo(name = "playback_position")
    val playbackPosition: Long = 0,
    @ColumnInfo(name = "audio_track_index")
    val audioTrackIndex: Int? = null,
    @ColumnInfo(name = "subtitle_track_index")
    val subtitleTrackIndex: Int? = null,
    @ColumnInfo(name = "playback_speed")
    val playbackSpeed: Float? = null,
    /**
     * How long the thing is, written down beside where it was left.
     *
     * Kept here and not looked up, because for a film on a share there is nowhere to look it up:
     * MediaStore never saw the file, and asking the server means opening it over the network. The
     * player knows the length while it is playing and costs nothing to say so.
     *
     * Zero where it was never recorded -- an entry written before this column existed. A bar cannot
     * be drawn from a position alone, so those show none until the film is opened again.
     */
    @ColumnInfo(name = "duration")
    val durationMillis: Long = 0,
    @ColumnInfo(name = "last_played_time")
    val lastPlayedTime: Long? = null,
    @ColumnInfo(name = "external_subs")
    val externalSubs: String = "",
    @ColumnInfo(name = "video_scale")
    val videoScale: Float = 1f,
    @ColumnInfo(name = "subtitle_delay")
    val subtitleDelayMilliseconds: Long = 0,
    @ColumnInfo(name = "subtitle_speed")
    val subtitleSpeed: Float = 1f,
)
