package com.music.nuvia.ui.screens

import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BlurCircular
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Brightness4
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MotionPhotosOff
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.ui.window.Dialog
import com.music.nuvia.ui.components.CustomColorPickerSheet
import com.music.nuvia.ui.components.NUViASelectionSheet
import com.music.nuvia.ui.components.SelectionOption
import com.music.nuvia.ui.components.SpotifyCanvasSheet
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import com.music.nuvia.BuildConfig
import com.music.nuvia.R
import com.music.nuvia.data.AppUpdateChecker
import com.music.nuvia.data.LocalMediaRepository
import com.music.nuvia.data.backup.BackupRepository
import com.music.nuvia.data.backup.ValidationResult
import com.music.nuvia.data.history.LocalHistoryStore
import com.music.nuvia.data.model.Account
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.AudioQuality
import com.music.nuvia.data.settings.DownloadQuality
import com.music.nuvia.data.settings.OutputPcmMode
import com.music.nuvia.data.settings.SearchHistory
import com.music.nuvia.data.settings.ThemeMode
import com.music.nuvia.data.sources.SourceKind
import com.music.nuvia.data.sources.SourceRegistry
import com.music.nuvia.playback.AudioCache
import com.music.nuvia.playback.DolbyAtmos
import com.music.nuvia.playback.SleepTimer
import com.music.nuvia.ui.components.BackupRestoreSheet
import com.music.nuvia.ui.components.LiquidGlassCard
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.LiquidToggle
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.nuviaAmbientBacklight
import com.music.nuvia.ui.components.nuviaGlass
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.nuviaTactilePress
import com.music.nuvia.ui.components.thumbnailBorder
import com.music.nuvia.ui.player.fullBleedArtworkAvailable
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * NUViA Settings experience: AMOLED Black + Liquid Glass + 15 Canonical Categories.
 * Organizes every configuration option into coherent, tactile glass cards with 100% real backing implementations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onAccountScrobbling: () -> Unit,
    onSources: () -> Unit,
    onLyricsSources: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onAppLanguage: () -> Unit = {},
    onEqualizer: () -> Unit = {},
    onListenTogether: () -> Unit = {},
    onSpotifyCanvas: () -> Unit = {},
    onReplay: () -> Unit = {},
    onHistory: () -> Unit = {},
    onDownloadManager: () -> Unit = {},
    onAccountSelector: () -> Unit = {},
    onDeveloperAbout: () -> Unit = {},
    onDeveloperDiagnostics: () -> Unit = onDeveloperAbout,
    onAbout: () -> Unit = onDeveloperAbout,
    onWhatsNew: () -> Unit = {},
    onOpenDiscord: () -> Unit = {},
    scrollState: ScrollState = rememberScrollState(),
) {
    val context = LocalContext.current
    val nuviaColors = LocalNUViAColors.current
    val coroutineScope = rememberCoroutineScope()

    // --- State collections from AppSettings ---
    val configs by SourceRegistry.configs.collectAsStateWithLifecycle()
    val jioSaavnConfig = configs.firstOrNull { it.kind == SourceKind.JIOSAAVN }
    val isJioSaavnActive = jioSaavnConfig?.enabled != false

    val losslessAudio by AppSettings.losslessAudio.collectAsStateWithLifecycle()
    val playbackSpeed by AppSettings.playbackSpeed.collectAsStateWithLifecycle()
    val crossfade by AppSettings.crossfadeSeconds.collectAsStateWithLifecycle()
    val smartFade by AppSettings.smartFadeEnabled.collectAsStateWithLifecycle()
    val skipSilence by AppSettings.skipSilence.collectAsStateWithLifecycle()

    val wifiQuality by AppSettings.audioQualityWifi.collectAsStateWithLifecycle()
    val cellularQuality by AppSettings.audioQualityCellular.collectAsStateWithLifecycle()
    val metered by AppSettings.forceMeteredConnection.collectAsStateWithLifecycle()
    val spatialAudio by AppSettings.spatialAudio.collectAsStateWithLifecycle()
    val atmosSupported by DolbyAtmos.supported.collectAsStateWithLifecycle()
    val atmosEnabled by DolbyAtmos.enabledOnDevice.collectAsStateWithLifecycle()

    val downloadQuality by AppSettings.downloadQuality.collectAsStateWithLifecycle()
    val wifiOnlyDownloads by AppSettings.wifiOnlyDownloads.collectAsStateWithLifecycle()

    val syncedLyrics by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val syllableSync by AppSettings.prioritizeSyllableSync.collectAsStateWithLifecycle()
    val orderedLyricsSummary = remember(lyricsSources) {
        lyricsSources.joinToString(" → ") { it.label }
    }

    val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val glassEffects by AppSettings.glassEffects.collectAsStateWithLifecycle()
    val liquidGlassBlur by AppSettings.liquidGlassBlur.collectAsStateWithLifecycle()
    val ambientLighting by AppSettings.ambientLighting.collectAsStateWithLifecycle()
    val dynamicArtworkColors by AppSettings.dynamicArtworkColors.collectAsStateWithLifecycle()
    val customThemeColor by AppSettings.customThemeColor.collectAsStateWithLifecycle()
    val lyricsSyncOffsetMs by AppSettings.lyricsSyncOffsetMs.collectAsStateWithLifecycle()
    var showColorPicker by remember { mutableStateOf(false) }
    val animatedCanvas by AppSettings.animatedCanvas.collectAsStateWithLifecycle()
    val spotifyToken by AppSettings.spotifyToken.collectAsStateWithLifecycle()
    val fullBleedArtwork by AppSettings.fullBleedArtwork.collectAsStateWithLifecycle()

    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

    val filterNonMusicAudio by AppSettings.filterNonMusicAudio.collectAsStateWithLifecycle()
    val localMusicFolderUri by AppSettings.localMusicFolderUri.collectAsStateWithLifecycle()

    val cacheLimitMb by AppSettings.audioCacheLimitMb.collectAsStateWithLifecycle()
    var currentCacheBytes by remember { mutableStateOf(AudioCache.currentSizeBytes()) }
    LifecycleResumeEffect(Unit) {
        currentCacheBytes = AudioCache.currentSizeBytes()
        onPauseOrDispose { }
    }

    val replayGenres by AppSettings.replayGenres.collectAsStateWithLifecycle()
    val searchHistoryEnabled by AppSettings.searchHistoryEnabled.collectAsStateWithLifecycle()

    val playNextOnSwipe by AppSettings.swipeToPlayNext.collectAsStateWithLifecycle()
    val stopOnTaskRemoved by AppSettings.stopOnTaskRemoved.collectAsStateWithLifecycle()
    val autoplay by AppSettings.autoplay.collectAsStateWithLifecycle()

    val nerdStats by AppSettings.showNerdStats.collectAsStateWithLifecycle()
    val outputPcmMode by AppSettings.outputPcmMode.collectAsStateWithLifecycle()

    val updateAvailable by AppUpdateChecker.available.collectAsStateWithLifecycle()
    var updateChecking by remember { mutableStateOf(false) }

    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmEnabled by AppSettings.lastfmEnabled.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    val discordToken by AppSettings.discordToken.collectAsStateWithLifecycle()

    // --- Modal Sheets and Dialog states ---
    var pickingQuality by remember { mutableStateOf<QualityTarget?>(null) }
    var pickingDownloadQuality by remember { mutableStateOf(false) }
    var pickingSpeed by remember { mutableStateOf(false) }
    var pickingSleepTimer by remember { mutableStateOf(false) }
    var pickingTheme by remember { mutableStateOf(false) }
    var pickingPcmMode by remember { mutableStateOf(false) }
    var confirmClearHistory by remember { mutableStateOf(false) }
    var confirmResetSettings by remember { mutableStateOf(false) }
    var showBackupRestore by remember { mutableStateOf(false) }
    var pendingValidation by remember { mutableStateOf<ValidationResult?>(null) }

    // Folder picker launcher for local media scoping
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            AppSettings.setLocalMusicFolderUri(uri.toString())
            LocalMediaRepository.rescan()
            Toast.makeText(context, "Music folder updated", Toast.LENGTH_SHORT).show()
        }
    }

    val backupCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = BackupRepository.exportToFile(context, uri)
                if (res.isSuccess) {
                    Toast.makeText(context, "Backup exported successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Backup failed: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val backupRestorePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val res = BackupRepository.readAndValidate(context, uri)
                pendingValidation = res
                showBackupRestore = true
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var showSpotifyCanvasSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled)
            .verticalScroll(scrollState)
            .padding(bottom = contentPadding.calculateBottomPadding()),
    ) {
        // Natural Scrolling Header: Back (if present), Title, Subtitle, and SearchBar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = GROUP_INSET + 4.dp, vertical = 8.dp),
        ) {
            if (onBack != null) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = onBack,
                    contentDescription = "Back",
                    size = 40.dp,
                    iconSize = 20.dp,
                    tier = NUViAGlassTier.Elevated,
                )
                Spacer(Modifier.height(10.dp))
            }

            Text(
                text = "Settings",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 30.sp,
                    letterSpacing = (-0.8).sp,
                ),
                color = nuviaColors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Pure Acoustic Architecture.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                ),
                color = nuviaColors.textSecondary,
            )

            Spacer(Modifier.height(14.dp))

            SettingsSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(4.dp))

        // ====================================================================
        // 1. ACCOUNT & INTEGRATIONS
        // ====================================================================
        SettingsGroup(
            header = "Account & Integrations",
            footer = "Manage accounts, scrobblers and connected services.",
            visible = matchesSettingsQuery(searchQuery, "Account & Integrations", "Account", "Sign in", "YouTube Music", "Switch account", "Scrobble", "Scrobbler", "Scrobbling", "Last.fm", "ListenBrainz", "Discord", "Rich Presence"),
        ) {
            if (signedIn) {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = account?.name?.takeIf { it.isNotBlank() } ?: "YouTube Music Account",
                    subtitle = account?.email?.takeIf { it.isNotBlank() } ?: "Signed in",
                    badge = "Connected",
                    onClick = onAccountSelector,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.RestartAlt,
                    title = "Switch account",
                    subtitle = "Manage or add alternate YouTube Music profiles",
                    onClick = onAccountSelector,
                )
            } else {
                SettingsRow(
                    icon = Icons.Rounded.Person,
                    title = "Sign in to YouTube Music",
                    subtitle = "Sync your personal library, playlists, and recommendations",
                    onClick = onSignIn,
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Equalizer,
                title = "Scrobbling & metadata",
                subtitle = when {
                    lastfmEnabled && lastfmUsername.isNotBlank() -> "Last.fm connected ($lastfmUsername)"
                    lastfmEnabled -> "Last.fm enabled"
                    listenBrainzToken.isNotBlank() -> "ListenBrainz active"
                    else -> "Last.fm and ListenBrainz scrobblers"
                },
                onClick = onAccountScrobbling,
            )
            RowDivider()
            SettingsRow(
                icon = ImageVector.vectorResource(R.drawable.ic_discord),
                title = "Discord Rich Presence",
                subtitle = if (discordToken.isNotBlank()) "Connected to Discord profile" else "Show what you're listening to on Discord",
                badge = if (discordToken.isNotBlank()) "Active" else null,
                onClick = onOpenDiscord,
            )
        }

        // ====================================================================
        // 2. SOURCES & PROVIDERS
        // ====================================================================
        SettingsGroup(
            header = "Sources & Providers",
            footer = "Configure music search, streaming resolution, and external audio providers.",
            visible = matchesSettingsQuery(searchQuery, "Sources & Providers", "Active sources", "YouTube Music", "JioSaavn", "Stream", "Provider"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Wifi,
                title = "Active sources",
                subtitle = if (isJioSaavnActive) "YouTube Music + JioSaavn" else "YouTube Music only",
                onClick = onSources,
            )
        }

        // ====================================================================
        // 3. PLAYBACK ENGINE
        // ====================================================================
        SettingsGroup(
            header = "Playback Engine",
            footer = "Audio pacing, transitions, and timing controls.",
            visible = matchesSettingsQuery(searchQuery, "Playback Engine", "Playback speed", "Sleep timer", "Smart crossfade", "Smart fade", "Skip silence"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Speed,
                title = "Playback speed",
                subtitle = "Tempo without pitch distortion",
                value = "${playbackSpeed}x",
                onClick = { pickingSpeed = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Timer,
                title = "Sleep timer",
                subtitle = sleepTimerCountdown()?.let { "Remaining: $it" } ?: "Turn off playback automatically",
                value = if (sleepTimerCountdown() != null) "Active" else "Off",
                onClick = { pickingSleepTimer = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Waves,
                title = "Crossfade",
                subtitle = if (crossfade > 0) "Fade smoothly between consecutive tracks" else "Gapless track transition",
                value = if (crossfade > 0) "${crossfade}s" else "Off",
                trailing = {
                    NUViASwitch(
                        checked = crossfade > 0,
                        onCheckedChange = { AppSettings.setCrossfadeSeconds(if (it) 5 else 0) },
                    )
                },
                onClick = { AppSettings.setCrossfadeSeconds(if (crossfade > 0) 0 else 5) },
            )
            if (crossfade > 0) {
                RowDivider()
                SliderRow(
                    icon = Icons.Rounded.Waves,
                    title = "Crossfade duration",
                    subtitle = "Transition blend window",
                    value = "${crossfade}s",
                    sliderValue = crossfade.toFloat(),
                    onSliderValue = { AppSettings.setCrossfadeSeconds(it.roundToInt()) },
                    valueRange = 1f..15f,
                    steps = 13,
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Smart automix fade",
                subtitle = "Analyzes musical outro and intro curves for studio-grade blend",
                trailing = {
                    NUViASwitch(
                        checked = smartFade,
                        onCheckedChange = AppSettings::setSmartFadeEnabled,
                    )
                },
                onClick = { AppSettings.setSmartFadeEnabled(!smartFade) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                title = "Skip silence",
                subtitle = "Trims leading and trailing silence between tracks",
                trailing = {
                    NUViASwitch(
                        checked = skipSilence,
                        onCheckedChange = AppSettings::setSkipSilence,
                    )
                },
                onClick = { AppSettings.setSkipSilence(!skipSilence) },
            )
        }

        // ====================================================================
        // 4. AUDIO QUALITY & BIT DEPTH
        // ====================================================================
        SettingsGroup(
            header = "Audio Quality & Bit Depth",
            footer = "Master audio output resolution and streaming bitrates.",
            visible = matchesSettingsQuery(searchQuery, "Audio Quality & Bit Depth", "Streaming on Wi-Fi", "Streaming on mobile data", "Metered connection", "Equalizer", "Dolby Atmos", "Spatial audio expander", "Audio output pipeline", "Audio Quality", "FLAC", "Lossless"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Wifi,
                title = "Streaming on Wi-Fi",
                subtitle = wifiQuality.detail,
                value = wifiQuality.label,
                onClick = { pickingQuality = QualityTarget.WIFI },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SignalCellularAlt,
                title = "Streaming on mobile data",
                subtitle = cellularQuality.detail,
                value = cellularQuality.label,
                onClick = { pickingQuality = QualityTarget.CELLULAR },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SignalCellularAlt,
                title = "Metered connection",
                subtitle = "Treat Wi-Fi connections as mobile data to save bandwidth",
                trailing = {
                    NUViASwitch(
                        checked = metered,
                        onCheckedChange = AppSettings::setMeteredConnection,
                    )
                },
                onClick = { AppSettings.setMeteredConnection(!metered) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Equalizer",
                subtitle = "NUViA 10-band tone shaper, bass boost & reverb",
                onClick = onEqualizer,
            )
            if (atmosSupported) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.SurroundSound,
                    title = "Dolby Atmos",
                    subtitle = if (atmosEnabled) "Hardware spatializer active" else "Disabled on device",
                    badge = if (atmosEnabled) "On" else "Off",
                    onClick = { openAtmosSettings(context) },
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SurroundSound,
                title = "Spatial audio expander",
                subtitle = "Headphone virtualization and binaural soundstage expansion",
                trailing = {
                    NUViASwitch(
                        checked = spatialAudio,
                        onCheckedChange = AppSettings::setSpatialAudio,
                    )
                },
                onClick = { AppSettings.setSpatialAudio(!spatialAudio) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = "Audio output pipeline",
                subtitle = "Internal ExoPlayer audio sink bit depth",
                value = outputPcmMode.label,
                onClick = { pickingPcmMode = true },
            )
        }

        // ====================================================================
        // 5. OFFLINE & DOWNLOADS
        // ====================================================================
        SettingsGroup(
            header = "Offline & Downloads",
            footer = "Local audio cache bitrate and network sync rules.",
            visible = matchesSettingsQuery(searchQuery, "Offline & Downloads", "Download quality", "Download over Wi-Fi only", "Download manager", "Storage", "Downloads"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Download,
                title = "Download quality",
                subtitle = downloadQuality.detail,
                value = downloadQuality.label,
                onClick = { pickingDownloadQuality = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Wifi,
                title = "Download over Wi-Fi only",
                subtitle = "Pause download queue while on mobile data",
                trailing = {
                    NUViASwitch(
                        checked = wifiOnlyDownloads,
                        onCheckedChange = AppSettings::setWifiOnlyDownloads,
                    )
                },
                onClick = { AppSettings.setWifiOnlyDownloads(!wifiOnlyDownloads) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Storage,
                title = "Download manager",
                subtitle = "View active download progress and offline files",
                onClick = onDownloadManager,
            )
        }

        // ====================================================================
        // 6. LYRICS & SYNC
        // ====================================================================
        SettingsGroup(
            header = "Lyrics & Sync",
            footer = "Synchronized lyrics engine, provider prioritization and syllable tracking.",
            visible = matchesSettingsQuery(searchQuery, "Lyrics & Sync", "Synced lyrics", "Lyrics sources", "Prioritize syllable sync", "Manual sync offset", "Lyrics", "Sync"),
        ) {
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.Notes,
                title = "Synced lyrics",
                subtitle = "Show time-aligned lyrics during playback",
                trailing = {
                    NUViASwitch(
                        checked = syncedLyrics,
                        onCheckedChange = AppSettings::setSyncedLyrics,
                    )
                },
                onClick = { AppSettings.setSyncedLyrics(!syncedLyrics) },
            )
            if (syncedLyrics) {
                RowDivider()
                SettingsRow(
                    icon = Icons.AutoMirrored.Rounded.Notes,
                    title = "Lyrics sources",
                    subtitle = orderedLyricsSummary,
                    onClick = onLyricsSources,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Prioritize syllable sync",
                    subtitle = "Hold line-synced lyrics as fallback while searching for word-by-word timestamps",
                    trailing = {
                        NUViASwitch(
                            checked = syllableSync,
                            onCheckedChange = AppSettings::setPrioritizeSyllableSync,
                        )
                    },
                    onClick = { AppSettings.setPrioritizeSyllableSync(!syllableSync) },
                )
                RowDivider()
                SliderRow(
                    icon = Icons.Rounded.GraphicEq,
                    title = "Manual sync offset",
                    subtitle = if (lyricsSyncOffsetMs == 0) "Synchronized with track audio" else if (lyricsSyncOffsetMs > 0) "Lyrics advanced by +${lyricsSyncOffsetMs}ms" else "Lyrics delayed by ${lyricsSyncOffsetMs}ms",
                    value = if (lyricsSyncOffsetMs == 0) "0ms" else if (lyricsSyncOffsetMs > 0) "+${lyricsSyncOffsetMs}ms" else "${lyricsSyncOffsetMs}ms",
                    sliderValue = lyricsSyncOffsetMs.toFloat(),
                    onSliderValue = { value ->
                        val rounded = kotlin.math.round(value / 50f) * 50f
                        val snapped = if (kotlin.math.abs(rounded) < 40f) 0 else rounded.toInt()
                        AppSettings.setLyricsSyncOffsetMs(snapped)
                    },
                    valueRange = -5000f..5000f,
                    steps = 199,
                )
            }
        }

        // ====================================================================
        // 7. APPEARANCE & AMOLED
        // ====================================================================
        SettingsGroup(
            header = "Appearance & AMOLED",
            footer = "Visual theme, contrast mode and player artwork presentation.",
            visible = matchesSettingsQuery(searchQuery, "Appearance & AMOLED", "Theme", "Minimal UI", "Liquid Glass", "Liquid Glass Blur", "Ambient lighting", "Dynamic artwork colors", "Custom accent color", "Animated canvas", "Spotify Canvas", "Full-bleed artwork", "Appearance", "AMOLED"),
        ) {
            SegmentedControl(
                options = ThemeMode.entries.map { it.label },
                selectedIndex = ThemeMode.entries.indexOf(theme),
                onSelect = { AppSettings.setThemeMode(ThemeMode.entries[it]) },
                modifier = Modifier.padding(start = ROW_INSET, end = ROW_INSET, top = 14.dp, bottom = 14.dp),
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Contrast,
                title = "Minimal UI",
                subtitle = "Distraction-free interface with neutral tones and no decorative glows",
                badge = if (minimalUi) "Active" else null,
                trailing = {
                    NUViASwitch(
                        checked = minimalUi,
                        onCheckedChange = AppSettings::setMinimalUi,
                    )
                },
                onClick = { AppSettings.setMinimalUi(!minimalUi) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurOn,
                title = "Liquid Glass",
                subtitle = if (minimalUi) "Overridden by Minimal UI" else "Refractive glass surfaces, fluid highlights and dynamic materials",
                enabled = !minimalUi,
                trailing = {
                    NUViASwitch(
                        checked = glassEffects,
                        enabled = !minimalUi,
                        onCheckedChange = AppSettings::setGlassEffects,
                    )
                },
                onClick = if (!minimalUi) { { AppSettings.setGlassEffects(!glassEffects) } } else null,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurCircular,
                title = "Liquid Glass Blur",
                subtitle = if (minimalUi) "Overridden by Minimal UI" else "Real-time background lens blur on applicable surfaces",
                enabled = !minimalUi && glassEffects,
                trailing = {
                    NUViASwitch(
                        checked = liquidGlassBlur,
                        enabled = !minimalUi && glassEffects,
                        onCheckedChange = AppSettings::setLiquidGlassBlur,
                    )
                },
                onClick = if (!minimalUi && glassEffects) { { AppSettings.setLiquidGlassBlur(!liquidGlassBlur) } } else null,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.WbTwilight,
                title = "Ambient lighting",
                subtitle = if (minimalUi) "Overridden by Minimal UI" else "Atmospheric artwork color illumination around player & bars",
                enabled = !minimalUi,
                trailing = {
                    NUViASwitch(
                        checked = ambientLighting,
                        enabled = !minimalUi,
                        onCheckedChange = AppSettings::setAmbientLighting,
                    )
                },
                onClick = if (!minimalUi) { { AppSettings.setAmbientLighting(!ambientLighting) } } else null,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = "Dynamic artwork colors",
                subtitle = if (minimalUi) "Overridden by Minimal UI" else "Tint UI accent colors using current track palette",
                enabled = !minimalUi,
                trailing = {
                    NUViASwitch(
                        checked = dynamicArtworkColors,
                        enabled = !minimalUi,
                        onCheckedChange = AppSettings::setDynamicArtworkColors,
                    )
                },
                onClick = if (!minimalUi) { { AppSettings.setDynamicArtworkColors(!dynamicArtworkColors) } } else null,
            )
            if (!dynamicArtworkColors && !minimalUi) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.ColorLens,
                    title = "Custom accent color",
                    subtitle = "Pick a custom color applied when dynamic colors is disabled",
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(Color(customThemeColor))
                                .border(1.dp, Color(0x40FFFFFF), androidx.compose.foundation.shape.CircleShape)
                        )
                    },
                    onClick = { showColorPicker = true },
                )
            }
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Animation,
                title = "Animated Canvas",
                subtitle = "Display looped video canvases from Spotify Canvas",
                trailing = {
                    NUViASwitch(
                        checked = animatedCanvas,
                        onCheckedChange = AppSettings::setAnimatedCanvas,
                    )
                },
                onClick = { AppSettings.setAnimatedCanvas(!animatedCanvas) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.VpnKey,
                title = "Spotify Canvas authorization",
                subtitle = if (spotifyToken.isNotBlank()) "Spotify session active" else "Authenticate with Spotify for background video canvases",
                badge = if (spotifyToken.isNotBlank()) "Authorized" else null,
                onClick = { showSpotifyCanvasSheet = true },
            )
            if (fullBleedArtworkAvailable()) {
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.Fullscreen,
                    title = "Full-bleed artwork",
                    subtitle = "Stretch square album artwork across the full width of the player",
                    trailing = {
                        NUViASwitch(
                            checked = fullBleedArtwork,
                            onCheckedChange = AppSettings::setFullBleedArtwork,
                        )
                    },
                    onClick = { AppSettings.setFullBleedArtwork(!fullBleedArtwork) },
                )
            }
        }

        // ====================================================================
        // 8. PERFORMANCE & MOTION
        // ====================================================================
        SettingsGroup(
            header = "Performance & Motion",
            footer = "Optimize rendering complexity and frame rates for lower power consumption.",
            visible = matchesSettingsQuery(searchQuery, "Performance & Motion", "Reduce animation", "Reduce dynamic blur", "Performance", "Motion"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.MotionPhotosOff,
                title = "Reduce animation",
                subtitle = "Replace fluid spatial transforms with snappy fades",
                trailing = {
                    NUViASwitch(
                        checked = reduceAnimation,
                        onCheckedChange = AppSettings::setReduceAnimation,
                    )
                },
                onClick = { AppSettings.setReduceAnimation(!reduceAnimation) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.BlurCircular,
                title = "Reduce dynamic blur",
                subtitle = "Use lightweight fallback backgrounds instead of real-time blur passes",
                trailing = {
                    NUViASwitch(
                        checked = reduceDynamicBlur,
                        onCheckedChange = AppSettings::setReduceDynamicBlur,
                    )
                },
                onClick = { AppSettings.setReduceDynamicBlur(!reduceDynamicBlur) },
            )
        }

        // ====================================================================
        // 9. LOCAL MEDIA & SCANNER
        // ====================================================================
        SettingsGroup(
            header = "Local Media & Scanner",
            footer = "Device audio indexing, short clip filtering, and directory scopes.",
            visible = matchesSettingsQuery(searchQuery, "Local Media & Scanner", "Rescan device library", "Filter non-music audio", "Local music folder", "Local Media", "Scanner", "Storage"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Folder,
                title = "Rescan device library",
                subtitle = "Index new downloads and local tracks from device storage",
                onClick = {
                    LocalMediaRepository.rescan()
                    Toast.makeText(context, "Scanning device media...", Toast.LENGTH_SHORT).show()
                },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.MusicOff,
                title = "Filter non-music audio",
                subtitle = "Exclude recordings, alarms, notifications, and audio shorter than 30s",
                trailing = {
                    NUViASwitch(
                        checked = filterNonMusicAudio,
                        onCheckedChange = AppSettings::setFilterNonMusicAudio,
                    )
                },
                onClick = { AppSettings.setFilterNonMusicAudio(!filterNonMusicAudio) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Folder,
                title = "Local music folder",
                subtitle = if (localMusicFolderUri.isNotBlank()) {
                    Uri.parse(localMusicFolderUri).lastPathSegment ?: localMusicFolderUri
                } else {
                    "All device storage"
                },
                onClick = { folderPickerLauncher.launch(null) },
            )
        }

        // ====================================================================
        // 10. STORAGE & CACHE
        // ====================================================================
        SettingsGroup(
            header = "Storage & Cache",
            footer = "Manage local media cache sizes and clear transient buffers.",
            visible = matchesSettingsQuery(searchQuery, "Storage & Cache", "Audio cache limit", "Clear audio cache", "Clear image cache", "Storage", "Cache"),
        ) {
            SliderRow(
                icon = Icons.Rounded.Storage,
                title = "Audio cache limit",
                subtitle = "Allocated storage ceiling",
                value = formatCacheSize(cacheLimitMb),
                sliderValue = cacheLimitMb.toFloat(),
                onSliderValue = { AppSettings.setAudioCacheLimitMb(it.roundToInt()) },
                valueRange = 512f..10240f,
                steps = 18,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.DeleteSweep,
                title = "Clear audio cache",
                subtitle = "Currently using ${formatCacheSize((currentCacheBytes / (1024 * 1024)).toInt())}",
                onClick = {
                    AudioCache.clear()
                    currentCacheBytes = AudioCache.currentSizeBytes()
                    Toast.makeText(context, "Audio cache cleared", Toast.LENGTH_SHORT).show()
                },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Clear,
                title = "Clear image cache",
                subtitle = "Frees space used by cached album artwork and artist pictures",
                onClick = {
                    coroutineScope.launch {
                        val imageLoader = SingletonImageLoader.get(context)
                        imageLoader.memoryCache?.clear()
                        imageLoader.diskCache?.clear()
                        Toast.makeText(context, "Image cache cleared", Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }

        // ====================================================================
        // 11. YOUR DATA & REPLAY
        // ====================================================================
        SettingsGroup(
            header = "Your Data & Replay",
            footer = "Listening trends, timeline history, and backup archives.",
            visible = matchesSettingsQuery(searchQuery, "Your Data & Replay", "NUViA Replay", "Artist genre taxonomy", "Listening history", "Clear listening history", "Search history", "Create backup", "Restore backup", "Replay", "History", "Backup"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "NUViA Replay",
                subtitle = "Explore top artists, tracks, and genre breakdowns",
                onClick = onReplay,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "Artist genre taxonomy",
                subtitle = "Enrich Replay charts with Last.fm verified genre tags",
                trailing = {
                    NUViASwitch(
                        checked = replayGenres,
                        onCheckedChange = AppSettings::setReplayGenres,
                    )
                },
                onClick = { AppSettings.setReplayGenres(!replayGenres) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.History,
                title = "Listening history",
                subtitle = "Browse your playback chronicle",
                onClick = onHistory,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.DeleteSweep,
                title = "Clear listening history",
                subtitle = "Permanently delete listening logs and Replay stats",
                onClick = { confirmClearHistory = true },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.History,
                title = "Search history",
                subtitle = if (searchHistoryEnabled) "Recent search queries saved" else "Search queries not recorded",
                trailing = {
                    NUViASwitch(
                        checked = searchHistoryEnabled,
                        onCheckedChange = AppSettings::setSearchHistoryEnabled,
                    )
                },
                onClick = { AppSettings.setSearchHistoryEnabled(!searchHistoryEnabled) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.CloudUpload,
                title = "Create backup",
                subtitle = "Export playlists, history, and configuration to a JSON file",
                onClick = {
                    val defaultName = "nuvia_backup_${System.currentTimeMillis()}.json"
                    backupCreateLauncher.launch(defaultName)
                },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Download,
                title = "Restore backup",
                subtitle = "Validate and import data from a saved backup file",
                onClick = {
                    backupRestorePicker.launch("application/json")
                },
            )
        }

        // ====================================================================
        // 12. BEHAVIOR
        // ====================================================================
        SettingsGroup(
            header = "Behavior",
            footer = "Gesture shortcuts and background playback lifecycle rules.",
            visible = matchesSettingsQuery(searchQuery, "Behavior", "Play next on swipe", "Stop on close from recents", "Autoplay recommendations", "Behavior", "Swipe", "Autoplay"),
        ) {
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                title = "Play next on swipe",
                subtitle = "Swiping a song enqueues it immediately after the current song",
                trailing = {
                    NUViASwitch(
                        checked = playNextOnSwipe,
                        onCheckedChange = AppSettings::setSwipeToPlayNext,
                    )
                },
                onClick = { AppSettings.setSwipeToPlayNext(!playNextOnSwipe) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.RestartAlt,
                title = "Stop on close from recents",
                subtitle = "Terminates playback when NUViA is swiped away from Android recents",
                trailing = {
                    NUViASwitch(
                        checked = stopOnTaskRemoved,
                        onCheckedChange = AppSettings::setStopOnTaskRemoved,
                    )
                },
                onClick = { AppSettings.setStopOnTaskRemoved(!stopOnTaskRemoved) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                title = "Autoplay recommendations",
                subtitle = "Continue playing similar music when playback queue ends",
                trailing = {
                    NUViASwitch(
                        checked = autoplay,
                        onCheckedChange = AppSettings::setAutoplay,
                    )
                },
                onClick = { AppSettings.setAutoplay(!autoplay) },
            )
        }

        // ====================================================================
        // 13. ADVANCED & DIAGNOSTICS
        // ====================================================================
        SettingsGroup(
            header = "Advanced & Diagnostics",
            footer = "Technical telemetry, audio sink telemetry and factory resets.",
            visible = matchesSettingsQuery(searchQuery, "Advanced & Diagnostics", "Stats for nerds", "Developer & Diagnostics", "Reset settings to default", "Advanced", "Diagnostics"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Code,
                title = "Stats for nerds",
                subtitle = "Display audio codec, container, bitrate, and sink metrics in the player",
                trailing = {
                    NUViASwitch(
                        checked = nerdStats,
                        onCheckedChange = AppSettings::setShowNerdStats,
                    )
                },
                onClick = { AppSettings.setShowNerdStats(!nerdStats) },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = "Developer & Diagnostics",
                subtitle = "Hardware blur, audio routing telemetry, and system info",
                onClick = onDeveloperDiagnostics,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.RestartAlt,
                title = "Reset settings to default",
                subtitle = "Restore all configurations to their original factory values",
                onClick = { confirmResetSettings = true },
            )
        }

        // ====================================================================
        // 14. ABOUT
        // ====================================================================
        SettingsGroup(
            header = "About NUViA",
            footer = "NUViA is open source and designed for uncompromising audio fidelity.",
            visible = matchesSettingsQuery(searchQuery, "About NUViA", "About", "Version", "What's New", "Support development", "Check for updates"),
        ) {
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = "About NUViA",
                subtitle = "Version ${BuildConfig.VERSION_NAME} · Created by Adhil CLT",
                onClick = onAbout,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.AutoAwesome,
                title = "What's New in NUViA",
                subtitle = "Key features, design updates and audio capabilities",
                onClick = onWhatsNew,
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.Favorite,
                title = "Support development",
                subtitle = "buymeatea.online/adhil",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeatea.online/adhil")).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
            )
            RowDivider()
            SettingsRow(
                icon = Icons.Rounded.SystemUpdate,
                title = "Check for updates",
                subtitle = when {
                    updateChecking -> "Checking for updates..."
                    updateAvailable != null -> "New version ${updateAvailable?.version} available!"
                    else -> "You are on the latest build"
                },
                badge = if (updateAvailable != null) "Update" else null,
                onClick = {
                    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch {
                        updateChecking = true
                        AppUpdateChecker.check()
                        updateChecking = false
                    }
                },
            )
        }

        val anyGroupVisible = listOf(
            matchesSettingsQuery(searchQuery, "Account & Integrations", "Account", "Sign in", "YouTube Music", "Switch account", "Scrobbling", "Last.fm", "ListenBrainz", "Discord", "Rich Presence"),
            matchesSettingsQuery(searchQuery, "Sources & Providers", "Active sources", "YouTube Music", "JioSaavn", "Stream", "Provider"),
            matchesSettingsQuery(searchQuery, "Playback Engine", "Playback speed", "Sleep timer", "Smart crossfade", "Smart fade", "Skip silence"),
            matchesSettingsQuery(searchQuery, "Audio Quality & Bit Depth", "Streaming on Wi-Fi", "Streaming on mobile data", "Metered connection", "Equalizer", "Dolby Atmos", "Spatial audio expander", "Audio output pipeline", "Audio Quality", "FLAC", "Lossless"),
            matchesSettingsQuery(searchQuery, "Offline & Downloads", "Download quality", "Download over Wi-Fi only", "Download manager", "Storage", "Downloads"),
            matchesSettingsQuery(searchQuery, "Lyrics & Sync", "Synced lyrics", "Lyrics sources", "Prioritize syllable sync", "Manual sync offset", "Lyrics", "Sync"),
            matchesSettingsQuery(searchQuery, "Appearance & AMOLED", "Theme", "Minimal UI", "Liquid Glass", "Liquid Glass Blur", "Ambient lighting", "Dynamic artwork colors", "Custom accent color", "Animated canvas", "Spotify Canvas", "Full-bleed artwork", "Appearance", "AMOLED"),
            matchesSettingsQuery(searchQuery, "Performance & Motion", "Reduce animation", "Reduce dynamic blur", "Performance", "Motion"),
            matchesSettingsQuery(searchQuery, "Local Media & Scanner", "Rescan device library", "Filter non-music audio", "Local music folder", "Local Media", "Scanner", "Storage"),
            matchesSettingsQuery(searchQuery, "Storage & Cache", "Audio cache limit", "Clear audio cache", "Clear image cache", "Storage", "Cache"),
            matchesSettingsQuery(searchQuery, "Your Data & Replay", "NUViA Replay", "Artist genre taxonomy", "Listening history", "Clear listening history", "Search history", "Create backup", "Restore backup", "Replay", "History", "Backup"),
            matchesSettingsQuery(searchQuery, "Behavior", "Play next on swipe", "Stop on close from recents", "Autoplay recommendations", "Behavior", "Swipe", "Autoplay"),
            matchesSettingsQuery(searchQuery, "Advanced & Diagnostics", "Stats for nerds", "Developer & Diagnostics", "Reset settings to default", "Advanced", "Diagnostics"),
            matchesSettingsQuery(searchQuery, "About NUViA", "About", "Version", "What's New", "Support development", "Check for updates"),
        ).any { it }

        if (!anyGroupVisible && searchQuery.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = GROUP_INSET, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = nuviaColors.textMuted,
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "No settings matching \"$searchQuery\"",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Try searching for audio, lyrics, theme, cache, or canvas",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                    ),
                    color = nuviaColors.textMuted,
                )
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    if (showSpotifyCanvasSheet) {
        SpotifyCanvasSheet(
            onDismissRequest = { showSpotifyCanvasSheet = false },
        )
    }

    // --- Bottom Sheets and Modals ---
    pickingQuality?.let { target ->
        val currentQuality = when (target) {
            QualityTarget.WIFI -> wifiQuality
            QualityTarget.CELLULAR -> cellularQuality
        }
        NUViASelectionSheet(
            title = "Streaming on ${target.title.lowercase()}",
            subtitle = "Maximum bitrate allowed on this connection",
            options = AudioQuality.entries.reversed().map { quality ->
                SelectionOption(
                    value = quality,
                    title = quality.label,
                    subtitle = "${quality.detail} · ~${quality.hourly} per hour",
                    icon = target.icon,
                )
            },
            selected = currentQuality,
            onSelect = { quality ->
                when (target) {
                    QualityTarget.WIFI -> AppSettings.setAudioQualityWifi(quality)
                    QualityTarget.CELLULAR -> AppSettings.setAudioQualityCellular(quality)
                }
                pickingQuality = null
            },
            onDismiss = { pickingQuality = null },
        )
    }

    if (pickingDownloadQuality) {
        NUViASelectionSheet(
            title = "Download quality",
            subtitle = "Bitrate for tracks saved offline on device",
            options = DownloadQuality.entries.reversed().map { quality ->
                SelectionOption(
                    value = quality,
                    title = quality.label,
                    subtitle = "${quality.detail} · ~${quality.perTrack} per track",
                    icon = Icons.Rounded.Download,
                )
            },
            selected = downloadQuality,
            onSelect = { quality ->
                AppSettings.setDownloadQuality(quality)
                pickingDownloadQuality = false
            },
            onDismiss = { pickingDownloadQuality = false },
        )
    }

    if (pickingSpeed) {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        NUViASelectionSheet(
            title = "Playback speed",
            subtitle = "Tempo without pitch distortion",
            options = speeds.map { speed ->
                SelectionOption(
                    value = speed,
                    title = "${speed}x" + if (speed == 1.0f) " (Normal)" else "",
                    icon = Icons.Rounded.Speed,
                )
            },
            selected = playbackSpeed,
            onSelect = { speed ->
                AppSettings.setPlaybackSpeed(speed)
                pickingSpeed = false
            },
            onDismiss = { pickingSpeed = false },
        )
    }

    if (pickingSleepTimer) {
        val timerOptions = listOf(
            15 to "15 minutes",
            30 to "30 minutes",
            45 to "45 minutes",
            60 to "1 hour",
            90 to "1 hour 30 minutes",
            120 to "2 hours",
        )
        val isTimerRunning = SleepTimer.deadline.collectAsStateWithLifecycle().value != null || SleepTimer.isRunning
        val options = buildList {
            timerOptions.forEach { (mins, label) ->
                add(
                    SelectionOption(
                        value = mins,
                        title = label,
                        icon = Icons.Rounded.Timer,
                    )
                )
            }
            if (isTimerRunning) {
                add(
                    SelectionOption(
                        value = 0,
                        title = "Turn off timer",
                        subtitle = "Cancel active countdown",
                        icon = Icons.Rounded.Clear,
                    )
                )
            }
        }
        NUViASelectionSheet(
            title = "Sleep timer",
            subtitle = if (isTimerRunning) "Timer currently active" else "Stop playback after a set duration",
            options = options,
            selected = -1,
            onSelect = { mins ->
                if (mins == 0) {
                    SleepTimer.cancel()
                } else {
                    SleepTimer.start(mins)
                }
                pickingSleepTimer = false
            },
            onDismiss = { pickingSleepTimer = false },
        )
    }

    if (pickingTheme) {
        NUViASelectionSheet(
            title = "Appearance Theme",
            subtitle = "AMOLED Dark, Clean Light, or System",
            options = ThemeMode.entries.map { mode ->
                SelectionOption(
                    value = mode,
                    title = mode.label,
                    icon = Icons.Rounded.Brightness4,
                )
            },
            selected = theme,
            onSelect = { mode ->
                AppSettings.setThemeMode(mode)
                pickingTheme = false
            },
            onDismiss = { pickingTheme = false },
        )
    }

    if (pickingPcmMode) {
        NUViASelectionSheet(
            title = "Audio output pipeline",
            subtitle = "Internal ExoPlayer audio sink bit depth",
            options = OutputPcmMode.entries.map { mode ->
                SelectionOption(
                    value = mode,
                    title = mode.label,
                    subtitle = if (mode == OutputPcmMode.FLOAT_32) {
                        "Full 32-bit floating point processing for high-res DACs"
                    } else {
                        "Standard 16-bit integer PCM output"
                    },
                    icon = Icons.Rounded.GraphicEq,
                )
            },
            selected = outputPcmMode,
            onSelect = { mode ->
                AppSettings.setOutputPcmMode(mode)
                pickingPcmMode = false
            },
            onDismiss = { pickingPcmMode = false },
        )
    }

    if (confirmClearHistory) {
        AlertDialog(
            onDismissRequest = { confirmClearHistory = false },
            title = {
                Text(
                    text = "Clear listening history?",
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    color = nuviaColors.textPrimary,
                )
            },
            text = {
                Text(
                    text = "This will permanently delete all local listening history on this device. This action cannot be undone.",
                    fontFamily = Manrope,
                    color = nuviaColors.textSecondary,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        LocalHistoryStore.clear()
                        confirmClearHistory = false
                        Toast.makeText(context, "Listening history cleared", Toast.LENGTH_SHORT).show()
                    },
                ) {
                    Text("Clear", color = MaterialTheme.colorScheme.error, fontFamily = Sora, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearHistory = false }) {
                    Text("Cancel", color = nuviaColors.textSecondary, fontFamily = Sora)
                }
            },
            containerColor = nuviaColors.glassSurface,
        )
    }

    if (confirmResetSettings) {
        Dialog(onDismissRequest = { confirmResetSettings = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.94f)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                    .background(if (nuviaColors.isDark) Color(0xF8121318) else Color(0xFAF8FAFC))
                    .nuviaSpecularBorder(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                        width = 1.dp,
                        topHighlight = if (nuviaColors.isDark) Color(0x40FFFFFF) else Color(0x60FFFFFF),
                        bottomBorder = if (nuviaColors.isDark) Color(0x20FFFFFF) else Color(0x18000000),
                    )
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "Reset all settings?",
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = nuviaColors.textPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "This will restore all audio, playback, and appearance settings to their default values. Your saved playlists and history will not be affected.",
                        fontFamily = Sora,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = nuviaColors.textSecondary,
                    )
                    Spacer(Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = { confirmResetSettings = false }) {
                            Text("Cancel", color = nuviaColors.textSecondary, fontFamily = Sora, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                AppSettings.resetToDefaults()
                                confirmResetSettings = false
                                Toast.makeText(context, "Settings reset to defaults", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset", color = Color(0xFFFF4D4D), fontFamily = Sora, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showColorPicker) {
        CustomColorPickerSheet(
            initialColor = customThemeColor,
            onColorSelected = { AppSettings.setCustomThemeColor(it) },
            onDismiss = { showColorPicker = false },
        )
    }

    val validation = pendingValidation
    if (showBackupRestore && validation != null) {
        BackupRestoreSheet(
            validationResult = validation,
            onDismiss = {
                showBackupRestore = false
                pendingValidation = null
            },
            onRestoreConfirmed = { options ->
                if (validation is ValidationResult.Success) {
                    coroutineScope.launch {
                        val result = BackupRepository.restore(context, validation.backup, options)
                        showBackupRestore = false
                        pendingValidation = null
                        if (result.isSuccess) {
                            val summary = result.getOrNull()
                            val msg = buildString {
                                append("Backup restored successfully!")
                                if (summary != null) {
                                    if (summary.restoredPlaylists > 0) append(" ${summary.restoredPlaylists} playlist(s).")
                                    if (summary.settingsRestored) append(" Settings updated.")
                                }
                            }
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            val err = result.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(context, "Restore failed: $err", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
        )
    }
}

/** Which ceiling the open picker is editing. */
private enum class QualityTarget(val title: String, val icon: ImageVector) {
    WIFI("Wi-Fi", Icons.Rounded.Wifi),
    CELLULAR("Mobile data", Icons.Rounded.SignalCellularAlt),
}

private fun openAtmosSettings(context: Context) {
    val intent = DolbyAtmos.settingsIntent(context)
    if (intent == null) {
        Toast.makeText(context, "No Dolby Atmos panel on this device", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.onFailure {
        Toast.makeText(context, "Couldn't open Dolby Atmos settings", Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun sleepTimerCountdown(): String? {
    val deadline by SleepTimer.deadline.collectAsStateWithLifecycle()
    val remaining by produceState<Long?>(initialValue = SleepTimer.remainingMs(), deadline) {
        while ((deadline ?: 0L) > 0L) {
            value = SleepTimer.remainingMs()
            delay(1000L)
        }
        value = null
    }
    val ms = remaining ?: return null
    if (ms <= 0L) return null
    val totalSeconds = (ms / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
}

private const val CACHE_WARNING_MB = 2048

private fun formatCacheSize(mb: Int): String {
    if (mb < 1024) return "$mb MB"
    val gb = mb / 1024f
    return if (gb == gb.toInt().toFloat()) "${gb.toInt()} GB" else "%.1f GB".format(gb)
}

/** Who you're signed in as, presented in a glowing liquid-glass card. */
@Composable
internal fun AccountCard(
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET)
            .clip(GroupShape)
            .background(nuviaColors.glassSurface)
            .nuviaSpecularBorder(GroupShape, 0.75.dp)
            .nuviaGlass(
                tier = NUViAGlassTier.Elevated,
                shape = GroupShape,
            )
            .then(if (signedIn) Modifier else Modifier.clickable(onClick = onSignIn))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (account?.thumbnailUrl != null) {
            AsyncImage(
                model = account.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .thumbnailBorder(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0x18FFFFFF))
                    .nuviaSpecularBorder(CircleShape, 0.75.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    tint = nuviaColors.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = account?.name ?: if (signedIn) "Signed in" else "Not signed in",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                ),
                color = nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = account?.email?.takeIf { it.isNotBlank() }
                    ?: if (signedIn) "Google Account" else "Tap to sign in with Google",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 13.sp,
                ),
                color = nuviaColors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!signedIn) {
            Spacer(Modifier.width(8.dp))
            Chevron()
        }
    }
}

// ---- Building blocks --------------------------------------------------------

internal val GroupShape = RoundedCornerShape(18.dp)
internal val GROUP_INSET = 16.dp
internal val ROW_INSET = 16.dp
internal val ICON_SIZE = 36.dp
internal val ICON_GAP = 14.dp
internal val TEXT_INSET = ROW_INSET + ICON_SIZE + ICON_GAP

internal fun matchesSettingsQuery(query: String, vararg keywords: String): Boolean {
    if (query.isBlank()) return true
    val clean = query.trim().lowercase()
    val cleanStem = when {
        clean.length > 4 && clean.endsWith("ing") -> clean.removeSuffix("ing")
        clean.length > 4 && clean.endsWith("ies") -> clean.removeSuffix("ies") + "y"
        clean.length > 4 && clean.endsWith("es") -> clean.removeSuffix("es")
        clean.length > 3 && clean.endsWith("s") -> clean.removeSuffix("s")
        clean.length > 3 && clean.endsWith("e") -> clean.removeSuffix("e")
        else -> clean
    }
    return keywords.any { kw ->
        val kwClean = kw.lowercase()
        kwClean.contains(clean) || kwClean.contains(cleanStem)
    }
}

@Composable
internal fun SettingsSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val capsuleShape = RoundedCornerShape(20.dp)
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(capsuleShape)
            .background(nuviaColors.glassSurfaceElevated)
            .border(0.75.dp, nuviaColors.glassBorder.copy(alpha = 0.60f), capsuleShape)
            .nuviaSpecularBorder(
                shape = capsuleShape,
                width = 0.75.dp,
                topHighlight = Color.White.copy(alpha = 0.25f),
                bottomBorder = Color.Black.copy(alpha = 0.50f),
            )
            .clickable {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = "Search settings",
                tint = if (query.isNotBlank()) nuviaColors.primary else nuviaColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )

            Spacer(Modifier.width(10.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = "Search settings, features & audio…",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Manrope,
                            fontSize = 13.5.sp,
                        ),
                        color = nuviaColors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = nuviaColors.textPrimary,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    cursorBrush = SolidColor(nuviaColors.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }

            if (query.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .clickable {
                            onQueryChange("")
                            focusRequester.requestFocus()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear",
                        tint = nuviaColors.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Liquid Glass Settings Group with dynamic specular rim border and subtle typography.
 */
@Composable
internal fun SettingsGroup(
    header: String? = null,
    footer: String? = null,
    visible: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!visible) return
    val nuviaColors = LocalNUViAColors.current
    if (header != null) {
        Text(
            text = header.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
            ),
            color = nuviaColors.primary.copy(alpha = 0.9f),
            modifier = Modifier.padding(
                start = GROUP_INSET + 6.dp,
                end = GROUP_INSET,
                top = 26.dp,
                bottom = 10.dp,
            ),
        )
    } else {
        Spacer(Modifier.height(24.dp))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = GROUP_INSET)
            .nuviaGlass(
                tier = NUViAGlassTier.Elevated,
                shape = GroupShape,
            ),
    ) {
        content()
    }
    if (footer != null) {
        Text(
            text = footer,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Manrope,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            ),
            color = nuviaColors.textMuted,
            modifier = Modifier.padding(
                start = GROUP_INSET + 6.dp,
                end = GROUP_INSET + 6.dp,
                top = 8.dp,
                bottom = 4.dp,
            ),
        )
    }
}

@Composable
internal fun RowDivider() {
    val nuviaColors = LocalNUViAColors.current
    HorizontalDivider(
        thickness = 0.5.dp,
        color = nuviaColors.divider,
        modifier = Modifier.padding(start = TEXT_INSET),
    )
}

/** An actionable option: glyph, two-line title/subtitle, and an action cue. */
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    value: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null && enabled) {
                    Modifier.nuviaTactilePress(pressedScale = 0.98f, onClick = onClick)
                } else Modifier,
            )
            .padding(horizontal = ROW_INSET, vertical = 14.dp)
            .alpha(if (enabled) 1f else 0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(ICON_SIZE)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x14FFFFFF))
                .nuviaSpecularBorder(RoundedCornerShape(10.dp), 0.6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) nuviaColors.textPrimary else nuviaColors.textMuted,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(ICON_GAP))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Badge(badge)
                }
            }
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        if (trailing != null) {
            trailing()
        } else if (value != null || onClick != null) {
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    ),
                    color = nuviaColors.primary,
                    maxLines = 1,
                )
                Spacer(Modifier.width(4.dp))
            }
            Chevron()
        }
    }
}

/**
 * Switch component for settings. Uses [LiquidToggle] natively.
 */
@Composable
fun NUViASwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LiquidToggle(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
    )
}

/** Subtle, premium badge for active indicators and beta tags. */
@Composable
internal fun Badge(text: String) {
    val nuviaColors = LocalNUViAColors.current
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = Sora,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.6.sp,
        ),
        color = nuviaColors.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(nuviaColors.primary.copy(alpha = 0.16f))
            .nuviaSpecularBorder(
                RoundedCornerShape(6.dp),
                0.5.dp,
                topHighlight = nuviaColors.primary.copy(alpha = 0.35f),
                bottomBorder = nuviaColors.primary.copy(alpha = 0.1f),
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

@Composable
internal fun Chevron() {
    val nuviaColors = LocalNUViAColors.current
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = nuviaColors.textMuted.copy(alpha = 0.6f),
        modifier = Modifier.size(20.dp),
    )
}

/** Continuous setting with dynamic gradient active track and floating value pill. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SliderRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    sliderValue: Float,
    onSliderValue: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = LocalHapticFeedback.current
    var lastHapticStep by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ROW_INSET, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(ICON_SIZE)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x14FFFFFF))
                    .nuviaSpecularBorder(RoundedCornerShape(10.dp), 0.6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = nuviaColors.textPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
            Spacer(Modifier.width(ICON_GAP))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(nuviaColors.primary.copy(alpha = 0.16f))
                    .nuviaSpecularBorder(
                        RoundedCornerShape(8.dp),
                        0.5.dp,
                        topHighlight = nuviaColors.primary.copy(alpha = 0.35f),
                        bottomBorder = nuviaColors.primary.copy(alpha = 0.1f),
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    ),
                    color = nuviaColors.primary,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                if (steps > 0) {
                    val stepIndex = (((newValue - valueRange.start) / (valueRange.endInclusive - valueRange.start)) * steps).roundToInt()
                    if (stepIndex != lastHapticStep) {
                        lastHapticStep = stepIndex
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
                onSliderValue(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = TEXT_INSET - ROW_INSET),
            colors = SliderDefaults.colors(
                thumbColor = nuviaColors.primary,
                activeTrackColor = nuviaColors.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
    }
}

/** Sliding pill selector with tactile feedback. */
@Composable
internal fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .nuviaSpecularBorder(RoundedCornerShape(12.dp), 0.75.dp)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val chosen = index == selectedIndex
            val pill by androidx.compose.animation.animateColorAsState(
                targetValue = if (chosen) {
                    if (enabled) nuviaColors.primary else nuviaColors.primary.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                animationSpec = tween(160),
                label = "segmentPill",
            )
            val labelColor by androidx.compose.animation.animateColorAsState(
                targetValue = if (chosen) {
                    nuviaColors.onPrimary
                } else {
                    if (enabled) nuviaColors.textSecondary else nuviaColors.textMuted
                },
                animationSpec = tween(160),
                label = "segmentLabel",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(pill)
                    .clickable(enabled = enabled) {
                        if (!chosen) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(index)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Sora,
                        fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = labelColor,
                    maxLines = 1,
                )
            }
        }
    }
}

