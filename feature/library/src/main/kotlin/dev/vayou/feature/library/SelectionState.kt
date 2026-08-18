package dev.vayou.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Video

/**
 * Which films and folders are marked, while marking is going on.
 *
 * Two sets of addresses and nothing else. Every action a selection leads to -- play, share, delete,
 * add to a list -- takes addresses, and the rows to draw ticked are on screen anyway. Keeping copies
 * of the films here would mean a second version of each that has to be kept in step with the first.
 *
 * Folders are held apart from the films inside them rather than dissolved into them: it is the
 * folder row that has to draw a mark, and a folder ticked because everything in it happens to be
 * ticked is a different fact from a folder the viewer picked.
 *
 * Marking is on exactly while something is marked. There is no separate flag, because the two could
 * then disagree: an empty selection with the bar still up is a screen offering to act on nothing.
 */
@Stable
class SelectionState(marked: Set<String> = emptySet(), markedFolders: Set<String> = emptySet()) {

    var marked: Set<String> by mutableStateOf(marked)
        private set

    var markedFolders: Set<String> by mutableStateOf(markedFolders)
        private set

    val isActive: Boolean get() = marked.isNotEmpty() || markedFolders.isNotEmpty()

    /** Rows picked, not files acted on: a folder counts once, however much is inside it. */
    val count: Int get() = marked.size + markedFolders.size

    fun isMarked(video: Video): Boolean = video.uriString in marked

    fun isMarked(folder: Folder): Boolean = folder.path in markedFolders

    fun toggle(video: Video) {
        marked = if (video.uriString in marked) marked - video.uriString else marked + video.uriString
    }

    fun toggle(folder: Folder) {
        markedFolders = if (folder.path in markedFolders) markedFolders - folder.path else markedFolders + folder.path
    }

    /** All of them, or none if they are already all marked -- one button doing the obvious thing twice. */
    fun toggleAll(videos: List<Video>, folders: List<Folder> = emptyList()) {
        val allVideos = videos.mapTo(mutableSetOf()) { it.uriString }
        val allFolders = folders.mapTo(mutableSetOf()) { it.path }
        if (marked.containsAll(allVideos) && markedFolders.containsAll(allFolders)) {
            clear()
        } else {
            marked = allVideos
            markedFolders = allFolders
        }
    }

    fun clear() {
        marked = emptySet()
        markedFolders = emptySet()
    }

    /**
     * The films the selection acts on, in the order they are shown rather than the order they were
     * tapped -- a marked folder standing for everything under it, nested folders included.
     *
     * Distinct, because one film can be reached both ways: marked on its own, and again inside a
     * folder marked after it. Shared twice, it would attach the same file twice.
     */
    fun selectionOf(videos: List<Video>, folders: List<Folder> = emptyList()): List<Video> {
        val fromFolders = folders.filter { it.path in markedFolders }.flatMap { it.allMediaList }
        return (videos.filter { it.uriString in marked } + fromFolders).distinctBy { it.uriString }
    }

    companion object {
        val Saver: Saver<SelectionState, List<List<String>>> = Saver(
            save = { listOf(it.marked.toList(), it.markedFolders.toList()) },
            restore = { SelectionState(it[0].toSet(), it[1].toSet()) },
        )
    }
}

@Composable
internal fun rememberSelectionState(): SelectionState = rememberSaveable(saver = SelectionState.Saver) {
    SelectionState()
}
