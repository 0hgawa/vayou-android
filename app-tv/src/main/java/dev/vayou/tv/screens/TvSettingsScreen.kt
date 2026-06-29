package dev.vayou.tv.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Audiotrack
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vayou.core.data.repository.MediaRepository
import dev.vayou.core.data.repository.PreferencesRepository
import dev.vayou.core.media.sync.MediaInfoSynchronizer
import dev.vayou.core.model.ApplicationPreferences
import dev.vayou.core.model.DecoderPriority
import dev.vayou.core.model.Folder
import dev.vayou.core.model.Font
import dev.vayou.core.model.PlayerPreferences
import dev.vayou.core.model.Resume
import dev.vayou.core.ui.R as CoreUiR
import dev.vayou.core.ui.designsystem.theme.VayouTheme
import dev.vayou.tv.BuildConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val ScreenPadding = 56.dp
private val RowSpacing = 8.dp
private val SectionListWidth = 280.dp
private val DetailMaxWidth = 720.dp

private val LocalMenuFocus = staticCompositionLocalOf<FocusRequester?> { null }
private val LocalPaneFocus = staticCompositionLocalOf<FocusRequester?> { null }

@HiltViewModel
class TvSettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val mediaInfoSynchronizer: MediaInfoSynchronizer,
    mediaRepository: MediaRepository,
) : ViewModel() {

    val applicationPreferences: StateFlow<ApplicationPreferences> = preferencesRepository.applicationPreferences
    val playerPreferences: StateFlow<PlayerPreferences> = preferencesRepository.playerPreferences

    val folders: StateFlow<List<Folder>> = mediaRepository.getFoldersFlow()
        .map { list -> list.sortedBy { it.name.lowercase() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updatePlayer(transform: (PlayerPreferences) -> PlayerPreferences) {
        viewModelScope.launch { preferencesRepository.updatePlayerPreferences(transform) }
    }

    fun updateApp(transform: (ApplicationPreferences) -> ApplicationPreferences) {
        viewModelScope.launch { preferencesRepository.updateApplicationPreferences(transform) }
    }

    fun toggleFolderExcluded(path: String) = updateApp {
        val next = if (path in it.excludeFolders) it.excludeFolders - path else it.excludeFolders + path
        it.copy(excludeFolders = next)
    }

    fun clearThumbnailCache() {
        viewModelScope.launch { mediaInfoSynchronizer.clearThumbnailsCache() }
    }

    fun resetPreferences() {
        viewModelScope.launch { preferencesRepository.resetPreferences() }
    }
}

private enum class Section(val label: String, val secondary: String, val icon: ImageVector) {
    Player("Reprodução", "Retomar, velocidade, autoplay", Icons.Outlined.PlayCircleOutline),
    Decoder("Decodificador", "Prioridade de hardware/software", Icons.Outlined.Memory),
    Audio("Áudio", "Idioma preferido, reforço de volume", Icons.Outlined.Audiotrack),
    Subtitle("Legendas", "Idioma, codificação, estilo", Icons.Outlined.Subtitles),
    Library("Biblioteca", "Pastas incluídas, último reproduzido", Icons.Outlined.VideoLibrary),
    General("Geral", "Cache de thumbs, redefinir configurações", Icons.Outlined.BuildCircle),
    About("Sobre", "Versão e créditos", Icons.Outlined.Info),
}

@Composable
fun TvSettingsScreen(
    onBack: () -> Unit,
    viewModel: TvSettingsViewModel = hiltViewModel(),
) {
    var current by remember { mutableStateOf(Section.Player) }
    var menuHasFocus by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    val player by viewModel.playerPreferences.collectAsStateWithLifecycle()
    val app by viewModel.applicationPreferences.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val menuFallback = remember { FocusRequester() }
    val paneFallback = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { menuFallback.requestFocus() } }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .width(SectionListWidth)
                .fillMaxHeight()
                .padding(start = ScreenPadding / 2, end = 12.dp)
                .onFocusChanged { menuHasFocus = it.hasFocus }
                .focusRestorer(menuFallback),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Section.entries.forEach { section ->
                SectionMenuItem(
                    section = section,
                    showSelectedFill = section == current && !menuHasFocus,
                    onSelect = { current = section },
                    modifier = Modifier
                        .then(if (section == current) Modifier.focusRequester(menuFallback) else Modifier)
                        .focusProperties { right = paneFallback },
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, end = ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = current.label,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Box(modifier = Modifier.widthIn(max = DetailMaxWidth).fillMaxHeight()) {
                CompositionLocalProvider(
                    LocalMenuFocus provides menuFallback,
                    LocalPaneFocus provides paneFallback,
                ) {
                    when (current) {
                        Section.Player -> PlayerSection(player, viewModel::updatePlayer)
                        Section.Decoder -> DecoderSection(player, viewModel::updatePlayer)
                        Section.Audio -> AudioSection(player, viewModel::updatePlayer)
                        Section.Subtitle -> SubtitleSection(player, viewModel::updatePlayer)
                        Section.Library -> LibrarySection(app, folders, viewModel::updateApp, viewModel::toggleFolderExcluded)
                        Section.General -> GeneralSection(viewModel::clearThumbnailCache, viewModel::resetPreferences)
                        Section.About -> AboutSection()
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionMenuItem(
    section: Section,
    showSelectedFill: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { if (isFocused) onSelect() }
    val cs = MaterialTheme.colorScheme
    Surface(
        onClick = onSelect,
        interactionSource = interaction,
        modifier = modifier.fillMaxWidth(),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (showSelectedFill) cs.secondaryContainer.copy(alpha = 0.4f) else Color.Transparent,
            focusedContainerColor = cs.inverseSurface,
            contentColor = if (showSelectedFill) cs.onSecondaryContainer else cs.onSurface,
            focusedContentColor = cs.inverseOnSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(section.icon, null, modifier = Modifier.size(VayouTheme.iconSize.sm))
            Spacer(Modifier.width(16.dp))
            Text(
                text = section.label,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlayerSection(prefs: PlayerPreferences, update: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    SectionList { firstFocus ->
        item("resume") {
            SettingsRow(
                title = "Retomar reprodução",
                value = if (prefs.resume == Resume.YES) "Sim" else "Não",
                onClick = { update { it.copy(resume = if (it.resume == Resume.YES) Resume.NO else Resume.YES) } },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        item("speed") {
            SettingsRow(
                title = "Velocidade padrão",
                value = "${prefs.defaultPlaybackSpeed}x",
                onClick = { dialog = "speed" },
            )
        }
        item("autoplay") {
            SettingsSwitchRow(
                title = "Reproduzir próximo automaticamente",
                checked = prefs.autoplay,
                onChange = { v -> update { it.copy(autoplay = v) } },
            )
        }
        item("remember") {
            SettingsSwitchRow(
                title = "Lembrar legendas/áudio escolhidos",
                checked = prefs.rememberSelections,
                onChange = { v -> update { it.copy(rememberSelections = v) } },
            )
        }
        item("autohide") {
            SettingsRow(
                title = "Ocultar controles após",
                value = "${prefs.controllerAutoHideTimeout}s",
                onClick = { dialog = "autohide" },
            )
        }
    }
    when (dialog) {
        "speed" -> RadioDialog(
            title = "Velocidade",
            options = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).map { it to "${it}x" },
            selected = prefs.defaultPlaybackSpeed,
            onSelect = { v -> update { it.copy(defaultPlaybackSpeed = v) } },
            onDismiss = { dialog = null },
        )
        "autohide" -> RadioDialog(
            title = "Tempo até ocultar",
            options = listOf(2, 4, 6, 10).map { it to "${it}s" },
            selected = prefs.controllerAutoHideTimeout,
            onSelect = { v -> update { it.copy(controllerAutoHideTimeout = v) } },
            onDismiss = { dialog = null },
        )
    }
}

@Composable
private fun DecoderSection(prefs: PlayerPreferences, update: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    SectionList { firstFocus ->
        item("priority") {
            SettingsRow(
                title = "Prioridade do decoder",
                value = decoderLabel(prefs.decoderPriority),
                onClick = { dialog = true },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
    }
    if (dialog) RadioDialog(
        title = "Prioridade",
        options = DecoderPriority.entries.map { it to decoderLabel(it) },
        selected = prefs.decoderPriority,
        onSelect = { v -> update { it.copy(decoderPriority = v) } },
        onDismiss = { dialog = false },
    )
}

private fun decoderLabel(p: DecoderPriority): String = when (p) {
    DecoderPriority.PREFER_DEVICE -> "Preferir dispositivo (hardware)"
    DecoderPriority.PREFER_APP -> "Preferir app (software)"
    DecoderPriority.DEVICE_ONLY -> "Apenas dispositivo"
}

@Composable
private fun AudioSection(prefs: PlayerPreferences, update: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    var dialog by remember { mutableStateOf(false) }
    SectionList { firstFocus ->
        item("lang") {
            SettingsRow(
                title = "Idioma de áudio preferido",
                value = languageLabel(prefs.preferredAudioLanguage),
                onClick = { dialog = true },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        item("boost") {
            SettingsSwitchRow(
                title = "Reforço de volume",
                checked = prefs.enableVolumeBoost,
                onChange = { v -> update { it.copy(enableVolumeBoost = v) } },
            )
        }
    }
    if (dialog) RadioDialog(
        title = "Idioma",
        options = LanguageOptions,
        selected = prefs.preferredAudioLanguage,
        onSelect = { v -> update { it.copy(preferredAudioLanguage = v) } },
        onDismiss = { dialog = false },
    )
}

@Composable
private fun SubtitleSection(prefs: PlayerPreferences, update: ((PlayerPreferences) -> PlayerPreferences) -> Unit) {
    var dialog by remember { mutableStateOf<String?>(null) }
    val charsetOpts = charsetOptions()
    SectionList { firstFocus ->
        item("lang") {
            SettingsRow(
                title = "Idioma de legendas",
                value = languageLabel(prefs.preferredSubtitleLanguage),
                onClick = { dialog = "lang" },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        item("encoding") {
            SettingsRow(
                title = "Codificação",
                value = charsetOpts.firstOrNull { it.first == prefs.subtitleTextEncoding }?.second ?: "Detectar",
                onClick = { dialog = "encoding" },
            )
        }
        item("size") {
            SettingsRow(
                title = "Tamanho do texto",
                value = "${prefs.subtitleTextSize}",
                onClick = { dialog = "size" },
            )
        }
        item("font") {
            SettingsRow(
                title = "Fonte",
                value = fontLabel(prefs.subtitleFont),
                onClick = { dialog = "font" },
            )
        }
        item("bold") {
            SettingsSwitchRow(
                title = "Negrito",
                checked = prefs.subtitleTextBold,
                onChange = { v -> update { it.copy(subtitleTextBold = v) } },
            )
        }
        item("bg") {
            SettingsSwitchRow(
                title = "Fundo da legenda",
                checked = prefs.subtitleBackground,
                onChange = { v -> update { it.copy(subtitleBackground = v) } },
            )
        }
        item("embedded") {
            SettingsSwitchRow(
                title = "Aplicar estilos embarcados",
                checked = prefs.applyEmbeddedStyles,
                onChange = { v -> update { it.copy(applyEmbeddedStyles = v) } },
            )
        }
    }
    when (dialog) {
        "lang" -> RadioDialog(
            title = "Idioma",
            options = LanguageOptions,
            selected = prefs.preferredSubtitleLanguage,
            onSelect = { v -> update { it.copy(preferredSubtitleLanguage = v) } },
            onDismiss = { dialog = null },
        )
        "encoding" -> RadioDialog(
            title = "Codificação",
            options = charsetOpts,
            selected = prefs.subtitleTextEncoding,
            onSelect = { v -> update { it.copy(subtitleTextEncoding = v) } },
            onDismiss = { dialog = null },
        )
        "size" -> RadioDialog(
            title = "Tamanho",
            options = (16..32 step 2).map { it to "$it" },
            selected = prefs.subtitleTextSize,
            onSelect = { v -> update { it.copy(subtitleTextSize = v) } },
            onDismiss = { dialog = null },
        )
        "font" -> RadioDialog(
            title = "Fonte",
            options = Font.entries.map { it to fontLabel(it) },
            selected = prefs.subtitleFont,
            onSelect = { v -> update { it.copy(subtitleFont = v) } },
            onDismiss = { dialog = null },
        )
    }
}

private fun fontLabel(f: Font): String = when (f) {
    Font.DEFAULT -> "Padrão"
    Font.MONOSPACE -> "Monoespaçada"
    Font.SANS_SERIF -> "Sem serifa"
    Font.SERIF -> "Com serifa"
}

@Composable
private fun LibrarySection(
    app: ApplicationPreferences,
    folders: List<Folder>,
    update: ((ApplicationPreferences) -> ApplicationPreferences) -> Unit,
    toggleFolder: (String) -> Unit,
) {
    SectionList { firstFocus ->
        item("mark") {
            SettingsSwitchRow(
                title = "Marcar último reproduzido",
                checked = app.markLastPlayedMedia,
                onChange = { v -> update { it.copy(markLastPlayedMedia = v) } },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        if (folders.isNotEmpty()) {
            item("header") {
                Text(
                    text = "Pastas escaneadas",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                )
            }
            items(folders, key = { it.path }) { folder ->
                val included = folder.path !in app.excludeFolders
                SettingsSwitchRow(
                    title = folder.name,
                    subtitle = folder.path,
                    checked = included,
                    onChange = { toggleFolder(folder.path) },
                )
            }
        }
    }
}

@Composable
private fun GeneralSection(onClearCache: () -> Unit, onReset: () -> Unit) {
    var confirm by remember { mutableStateOf<String?>(null) }
    SectionList { firstFocus ->
        item("cache") {
            SettingsRow(
                title = "Limpar cache de miniaturas",
                onClick = { confirm = "cache" },
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
        item("reset") {
            SettingsRow(
                title = "Redefinir configurações",
                subtitle = "Restaura todos os valores padrão",
                onClick = { confirm = "reset" },
            )
        }
    }
    confirm?.let { kind ->
        ConfirmDialog(
            title = if (kind == "cache") "Limpar cache?" else "Redefinir tudo?",
            message = if (kind == "cache") "As miniaturas serão regeradas conforme você navega."
                      else "Todas as preferências voltam ao padrão.",
            onConfirm = {
                if (kind == "cache") onClearCache() else onReset()
                confirm = null
            },
            onDismiss = { confirm = null },
        )
    }
}

@Composable
private fun AboutSection() {
    SectionList { firstFocus ->
        item("ver") {
            SettingsRow(
                title = "Versão",
                value = BuildConfig.VERSION_NAME,
                onClick = {},
                modifier = Modifier.focusRequester(firstFocus),
            )
        }
    }
}

@Composable
private fun SectionList(content: androidx.compose.foundation.lazy.LazyListScope.(FocusRequester) -> Unit) {
    val firstFocus = LocalPaneFocus.current ?: remember { FocusRequester() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().focusRestorer(firstFocus),
        verticalArrangement = Arrangement.spacedBy(RowSpacing),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        content(firstFocus)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String? = null,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isFocused by interaction.collectIsFocusedAsState()
    val cs = MaterialTheme.colorScheme
    val menuFocus = LocalMenuFocus.current
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = modifier
            .fillMaxWidth()
            .then(if (menuFocus != null) Modifier.focusProperties { left = menuFocus } else Modifier),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = cs.inverseSurface,
            contentColor = cs.onBackground,
            focusedContentColor = cs.inverseOnSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) cs.inverseOnSurface.copy(alpha = 0.75f) else cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!value.isNullOrBlank()) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isFocused) cs.inverseOnSurface.copy(alpha = 0.85f) else cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        value = if (checked) "Ativado" else "Desativado",
        onClick = { onChange(!checked) },
        modifier = modifier,
    )
}

@Composable
private fun <T> RadioDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(560.dp),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            onClick = {},
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(options, key = { it.first.toString() }) { (value, label) ->
                        SettingsRow(
                            title = label,
                            value = if (value == selected) "✓" else null,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.width(480.dp),
            shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                focusedContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
            onClick = {},
        ) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    dev.vayou.tv.ui.VayouTvButton(onClick = onDismiss, label = "Cancelar")
                    dev.vayou.tv.ui.VayouTvButton(onClick = onConfirm, label = "Confirmar", primary = true)
                }
            }
        }
    }
}

private fun languageLabel(code: String): String =
    LanguageOptions.firstOrNull { it.first == code }?.second ?: "Automático"

private val LanguageOptions: List<Pair<String, String>> = listOf(
    "" to "Automático",
    "por" to "Português",
    "eng" to "Inglês",
    "spa" to "Espanhol",
    "fra" to "Francês",
    "ger" to "Alemão",
    "ita" to "Italiano",
    "jpn" to "Japonês",
    "kor" to "Coreano",
    "chi" to "Chinês",
)

@Composable
private fun charsetOptions(): List<Pair<String, String>> {
    val raw = stringArrayResource(id = CoreUiR.array.charsets_list)
    return listOf("" to "Detectar") + raw.mapNotNull { line ->
        val parts = line.split(";", limit = 2)
        if (parts.size == 2) parts[1] to parts[0] else null
    }
}
