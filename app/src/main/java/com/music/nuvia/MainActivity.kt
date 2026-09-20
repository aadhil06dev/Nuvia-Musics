package com.music.nuvia

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.ui.unit.Constraints
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.isSystemInDarkTheme
import com.music.nuvia.ui.components.nuviaSpecularBorder
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.nuviaGlass
import com.music.nuvia.ui.components.nuviaTactilePress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.music.nuvia.auth.DiscordLoginScreen
import com.music.nuvia.auth.YtMusicLoginScreen
import com.music.nuvia.data.LocalMediaRepository
import com.music.nuvia.data.NerdStats
import com.music.nuvia.data.TrackLog
import com.music.nuvia.data.model.BrowseType
import com.music.nuvia.ui.components.NUViAAmbientBackground
import com.music.nuvia.ui.components.NUViAHomeEntranceOverlay
import com.music.nuvia.ui.components.NUViALaunchOverlay
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.NUViATheme
import com.music.nuvia.ui.theme.rememberNUViADynamicPalette
import com.music.nuvia.data.model.LikeStatus
import com.music.nuvia.data.model.SearchFilter
import com.music.nuvia.data.model.SearchResult
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.UiState
import com.music.nuvia.data.model.UserPlaylist
import com.music.nuvia.data.scrobbling.LastFM
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.data.settings.ThemeMode
import com.music.nuvia.data.sources.SourceRegistry
import com.music.nuvia.data.sources.TrackMatcher
import com.music.nuvia.ui.screens.AccountAndScrobblingScreen
import com.music.nuvia.ui.screens.DiscordDialog
import com.music.nuvia.ui.screens.DiscordDialogHost
import com.music.nuvia.ui.screens.DiscordScreen
import com.music.nuvia.ui.screens.JioSaavnSourceScreen
import com.music.nuvia.ui.screens.SettingsScreen
import com.music.nuvia.playback.QueueBuilder
import com.music.nuvia.playback.QueueShuffle
import com.music.nuvia.playback.autoplaySectionStart
import com.music.nuvia.playback.dropAutoplayTracks
import com.music.nuvia.playback.playSongs
import com.music.nuvia.playback.toMediaItem
import com.music.nuvia.download.DownloadSession
import com.music.nuvia.download.DownloadStore
import com.music.nuvia.download.Downloads
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.ui.components.BrowseActionsSheet
import com.music.nuvia.ui.components.BrowseTarget
import com.music.nuvia.ui.components.DownloadManagerSheet
import com.music.nuvia.ui.components.PlaylistActionsSheet
import com.music.nuvia.ui.components.PlaylistPickerSheet
import com.music.nuvia.ui.components.SongActionsSheet
import com.music.nuvia.ui.components.TopBarDownloadButton
import com.music.nuvia.playback.rememberMediaController
import com.music.nuvia.playback.rememberPlaybackPosition
import com.music.nuvia.playback.rememberPlayerState
import com.music.nuvia.ui.MainViewModel
import com.music.nuvia.ui.components.BottomTab
import com.music.nuvia.ui.components.FloatingBottomBar
import com.music.nuvia.ui.components.FrostedTopBar
import com.music.nuvia.ui.components.LastfmLoginAlert
import com.music.nuvia.ui.components.ListenBrainzTokenAlert
import com.music.nuvia.ui.components.MiniPlayer
import com.music.nuvia.ui.components.TopBarAccountButton
import com.music.nuvia.ui.components.LyricsSourcesDialog
import com.music.nuvia.ui.components.AppLanguageDialog
import com.music.nuvia.ui.components.LocalLiquidGlassBlurEnabled
import com.music.nuvia.ui.components.LocalLiquidGlassEnabled
import com.music.nuvia.ui.icons.NUViAIcons
import androidx.media3.common.Player
import com.music.nuvia.data.YtMusicRepository
import com.music.nuvia.ui.player.NowPlayingScreen
import com.music.nuvia.ui.screens.DetailScreen
import com.music.nuvia.ui.screens.LocalMusicScreen
import com.music.nuvia.ui.screens.HomeScreen
import com.music.nuvia.ui.screens.ExploreScreen
import com.music.nuvia.ui.screens.LibraryScreen
import com.music.nuvia.auth.AuthStore
import com.music.nuvia.data.AppUpdateChecker
import com.music.nuvia.data.history.LocalHistoryStore
import com.music.nuvia.ui.components.AccountProfileSelector
import com.music.nuvia.ui.components.UpdateAvailableDialog
import com.music.nuvia.ui.player.AudioOutputSheet
import com.music.nuvia.ui.screens.EqualizerScreen
import com.music.nuvia.ui.screens.HistoryScreen
import com.music.nuvia.ui.screens.ListenTogetherScreen
import com.music.nuvia.ui.screens.ReplayScreen
import com.music.nuvia.ui.screens.SpotifyCanvasAuthScreen
import com.music.nuvia.ui.screens.SearchScreen
import com.music.nuvia.ui.screens.PreferenceSurveyScreen
import com.music.nuvia.ui.screens.GettingStartedScreen
import com.music.nuvia.ui.screens.AccountConnectScreen
import com.music.nuvia.ui.screens.LoginSuccessScreen
import com.music.nuvia.ui.screens.DeveloperAboutScreen
import com.music.nuvia.ui.screens.DeveloperDiagnosticsScreen
import com.music.nuvia.ui.screens.AboutScreen
import com.music.nuvia.ui.screens.WhatsNewScreen
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.snapshotFlow
import com.music.nuvia.ui.navigation.TabNavigation
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import kotlin.math.roundToInt
import com.music.nuvia.ui.theme.rememberArtworkPalette
import com.music.nuvia.ui.theme.SystemBarIcons
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme by AppSettings.themeMode.collectAsStateWithLifecycle()
            val darkTheme = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
            val glassEffects by AppSettings.glassEffects.collectAsStateWithLifecycle()
            val liquidGlassBlur by AppSettings.liquidGlassBlur.collectAsStateWithLifecycle()
            val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()

            val effectiveGlass = !minimalUi && glassEffects && !reduceDynamicBlur
            val effectiveBlur = effectiveGlass && liquidGlassBlur

            CompositionLocalProvider(
                LocalLiquidGlassEnabled provides effectiveGlass,
                LocalLiquidGlassBlurEnabled provides effectiveBlur,
            ) {
                NUViATheme(darkTheme = darkTheme) {
                    NUViAApp(darkTheme = darkTheme)
                }
            }
        }
    }
}

private enum class OnboardingStep {
    GETTING_STARTED,
    DEVELOPER,
    ACCOUNT_CONNECT,
    LOGIN,
    LOGIN_SUCCESS,
    SURVEY,
    COMPLETED,
}

enum class SubpageDestination {
    SETTINGS,
    DEVELOPER_DIAGNOSTICS,
    ABOUT,
    WHATS_NEW,
    SOURCES,
    ACCOUNT_SCROBBLING,
    DISCORD,
    EQUALIZER,
    LISTEN_TOGETHER,
    SPOTIFY_CANVAS,
    HISTORY,
    REPLAY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NUViAApp(darkTheme: Boolean, viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val hazeState = remember { HazeState() }
    var showLaunchOverlay by remember { mutableStateOf(true) }
    var showSurveyEntranceOverlay by remember { mutableStateOf(false) }
    val onboardingCompleted by AppSettings.onboardingCompleted.collectAsStateWithLifecycle()
    var onboardingActive by remember(onboardingCompleted) {
        mutableStateOf(!onboardingCompleted)
    }
    val onboardingBackstack = remember {
        mutableStateListOf<OnboardingStep>(OnboardingStep.GETTING_STARTED)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var activeNowPlayingSong by remember { mutableStateOf<Song?>(null) }
    var showLogin by remember { mutableStateOf(false) }
    var isAddAccountLogin by remember { mutableStateOf(false) }
    val subpageStack = remember { mutableStateListOf<SubpageDestination>() }
    val currentSubpage = subpageStack.lastOrNull()
    val isSubpageActive = subpageStack.isNotEmpty()

    fun navigateToSubpage(dest: SubpageDestination) {
        if (subpageStack.lastOrNull() != dest) {
            subpageStack.add(dest)
        }
    }

    fun popSubpage(): Boolean {
        if (subpageStack.isNotEmpty()) {
            subpageStack.removeAt(subpageStack.lastIndex)
            return true
        }
        return false
    }

    fun dismissAllSubpages() {
        subpageStack.clear()
        viewModel.clearDetail()
    }

    val showSettings = currentSubpage == SubpageDestination.SETTINGS
    val showAccountScrobbling = currentSubpage == SubpageDestination.ACCOUNT_SCROBBLING
    val showSources = currentSubpage == SubpageDestination.SOURCES
    val showDiscord = currentSubpage == SubpageDestination.DISCORD
    val showListenTogether = currentSubpage == SubpageDestination.LISTEN_TOGETHER
    val showEqualizer = currentSubpage == SubpageDestination.EQUALIZER
    val showHistory = currentSubpage == SubpageDestination.HISTORY
    val showReplay = currentSubpage == SubpageDestination.REPLAY
    val showSpotifyCanvasAuth = currentSubpage == SubpageDestination.SPOTIFY_CANVAS
    val showDeveloperAbout = false
    val showDeveloperDiagnostics = currentSubpage == SubpageDestination.DEVELOPER_DIAGNOSTICS
    val showAbout = currentSubpage == SubpageDestination.ABOUT
    val showWhatsNew = currentSubpage == SubpageDestination.WHATS_NEW
    val settingsScrollState = remember { ScrollState(0) }

    var showDownloadManager by remember { mutableStateOf(false) }
    var showLyricsSources by remember { mutableStateOf(false) }
    var showAppLanguage by remember { mutableStateOf(false) }
    var showListenBrainzLogin by remember { mutableStateOf(false) }
    var showLastfmLogin by remember { mutableStateOf(false) }
    var showAccountSelector by remember { mutableStateOf(false) }
    var showAudioOutputSheet by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val updateAvailable by AppUpdateChecker.available.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        AppUpdateChecker.check()
    }
    LaunchedEffect(updateAvailable) {
        if (updateAvailable != null) {
            showUpdateDialog = true
        }
    }
    // Discord Rich Presence: its own page under Account & integrations, its own
    // full-screen sign-in, and one slot for whichever of its alerts is open.
    // The alerts live out here rather than on the page because their scrim has
    // to cover the tab bar and mini player, which are drawn after it.
    var showDiscordLogin by remember { mutableStateOf(false) }
    var discordDialog by remember { mutableStateOf<DiscordDialog?>(null) }
    var songActions by remember { mutableStateOf<Song?>(null) }
    // Whether the player's album/artist lookup (below, for the current track)
    // is still in flight — read by the long-press sheet so it can show a
    // loading row instead of the two just being absent while it waits.
    var linksLoading by remember { mutableStateOf(false) }
    // Which track the playlist picker is adding, or null when it's closed.
    // Separate from [songActions] so the menu can close behind it — the picker
    // is the next step, not a second sheet stacked on the first.
    var playlistTarget by remember { mutableStateOf<Song?>(null) }
    // The picker opened from the Library tab, where there is no track and
    // creating the playlist is the whole errand.
    var creatingPlaylist by remember { mutableStateOf(false) }
    var playlistActions by remember { mutableStateOf<UserPlaylist?>(null) }
    var browseActions by remember { mutableStateOf<BrowseTarget?>(null) }
    val pinnedPlaylists by AppSettings.pinnedPlaylists.collectAsStateWithLifecycle()
    val autoplay by AppSettings.autoplay.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    val reduceAnimation by AppSettings.reduceAnimation.collectAsStateWithLifecycle()
    // Incremented each time the search tab is re-tapped while already selected,
    // which SearchScreen uses as a signal to focus the input field.
    var searchFocusTrigger by remember { mutableIntStateOf(0) }

    // The player fills the screen with dark artwork whichever theme is on, so
    // it keeps light glyphs; every other surface follows the theme.
    SystemBarIcons(dark = !darkTheme && !showNowPlaying)

    val homeState by viewModel.home.collectAsStateWithLifecycle()
    val homeLoadingMore by viewModel.homeLoadingMore.collectAsStateWithLifecycle()

    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val exploreState by viewModel.explore.collectAsStateWithLifecycle()
    val libraryState by viewModel.library.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val account by viewModel.account.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsSource by viewModel.lyricsSource.collectAsStateWithLifecycle()
    val lyricsChecked by viewModel.lyricsChecked.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val searchSuggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val detailStack by viewModel.detailStack.collectAsStateWithLifecycle()
    val detail = detailStack.lastOrNull()
    // Local Music has no artwork to wash the bar in, so it renders with a
    // plain status bar rather than the artwork-driven blur other detail
    // pages (album/artist/playlist) get. Downloads is the same page, and the
    // tab row it now carries sits directly under the bar, so it needs the same
    // treatment — an artwork blur over it would tint the tabs.
    val isLocalDetail = detail?.browseId?.startsWith("local:") == true
    val likeStatuses by viewModel.likeStatuses.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val playlistsLoading by viewModel.playlistsLoading.collectAsStateWithLifecycle()

    // The Downloads page is a snapshot of the folder, taken when it was opened.
    // Saving a track or deleting one while it is on screen changes what belongs
    // on it — and now that the page groups by artist and album, a stale list is
    // stale counts and a missing row in three places rather than one. So it is
    // taken again whenever the record of what's on disk changes.
    val savedDownloads by Downloads.saved.collectAsStateWithLifecycle()
    LaunchedEffect(savedDownloads, detail?.browseId) {
        if (detail?.browseId == "local:downloads") {
            viewModel.reloadLocalDetail("local:downloads")
        }
    }

    val controller = rememberMediaController()
    val player = rememberPlayerState(controller)
    val playbackPositionState = rememberPlaybackPosition(controller, player.isPlaying)
    val playbackPositionProvider: () -> Long = remember(playbackPositionState) {
        { playbackPositionState.value }
    }
    val nuviaColors = rememberNUViADynamicPalette(player.song?.thumbnailUrl, dark = darkTheme)
    val shuffleEnabled by QueueShuffle.enabled.collectAsStateWithLifecycle()

    // AutoPlay: once the queue reaches its last track, extend it with YouTube
    // Music's radio mix for that song so playback carries on by itself.
    //
    // Repeat-all is left out of the trigger: its whole point is to loop the
    // queue as it stands, which AutoPlay extending it forever would defeat —
    // the queue would never actually reach the end repeat-all is meant to
    // wrap from. See onCycleRepeat, which drops whatever AutoPlay has already
    // added the moment repeat-all is turned on.
    var autoplaySeed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(autoplay, player.queueIndex, player.queue.size, player.song?.videoId, player.repeatMode) {
        val song = player.song
        val current = song?.videoId
        if (!autoplay || song == null || current == null) return@LaunchedEffect
        if (player.repeatMode == Player.REPEAT_MODE_ALL) return@LaunchedEffect
        if (player.queueIndex < player.queue.lastIndex) return@LaunchedEffect
        if (autoplaySeed == current) return@LaunchedEffect
        autoplaySeed = current
        // Radio is YouTube's, and only YouTube's — a module track's id means
        // nothing to it. See [youtubeSeedFor].
        val seed = youtubeSeedFor(song) ?: return@LaunchedEffect
        YtMusicRepository.radio(seed).onSuccess { related ->
            val extra = QueueBuilder.extend(player.queue, related, RADIO_BATCH)
            if (extra.isNotEmpty()) {
                // Swapped for the catalogue audio track before it ever
                // reaches the queue — see YtMusicRepository.resolveAudio.
                val resolved = coroutineScope {
                    extra.map { async { YtMusicRepository.resolveAudio(it) } }.awaitAll()
                }
                controller?.addMediaItems(
                    resolved.map { it.copy(fromAutoplay = true).toMediaItem() },
                )
            }
        }
    }

    // Lyrics follow whatever is playing; duration lands a beat after the track.
    // Keyed on the lyric settings too, so turning a source on or off, reordering,
    // or toggling syllable sync applies to the track already playing.
    val syncedLyricsEnabled by AppSettings.syncedLyrics.collectAsStateWithLifecycle()
    val lyricsSources by AppSettings.lyricsSources.collectAsStateWithLifecycle()
    val lyricsSourceOrder by AppSettings.lyricsSourceOrder.collectAsStateWithLifecycle()
    val prioritizeSyllableSync by AppSettings.prioritizeSyllableSync.collectAsStateWithLifecycle()
    LaunchedEffect(
        player.song?.videoId,
        player.durationMs,
        syncedLyricsEnabled,
        lyricsSources,
        lyricsSourceOrder,
        prioritizeSyllableSync,
    ) {
        player.song?.let {
            viewModel.loadLyrics(
                it.videoId,
                it.title,
                it.artist,
                player.durationMs,
                it.albumName,
                it.localUri,
            )
        }
    }

    val homeListState = rememberLazyListState()
    val exploreListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val searchListState = rememberLazyListState()
    val currentListState = when (selectedTab) {
        TAB_HOME -> homeListState
        TAB_EXPLORE -> exploreListState
        TAB_LIBRARY -> libraryListState
        else -> searchListState
    }

    // Pull-to-refresh: the drag lives with the feed, but the indicator is the
    // line under the top bar, so the state has to be visible to both.
    val homePull = rememberPullToRefreshState()
    val explorePull = rememberPullToRefreshState()
    val libraryPull = rememberPullToRefreshState()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val currentFeed = when {
        showSettings || showAccountScrobbling || showSources || detail != null -> null
        selectedTab == TAB_HOME -> MainViewModel.Feed.HOME
        selectedTab == TAB_EXPLORE -> MainViewModel.Feed.EXPLORE
        selectedTab == TAB_LIBRARY -> MainViewModel.Feed.LIBRARY
        else -> null
    }
    // The lead shelf is listening history, so opening Home after playing
    // something is exactly when it needs re-fetching.
    LaunchedEffect(currentFeed) {
        if (currentFeed == MainViewModel.Feed.HOME) viewModel.onHomeShown()
        // Likewise for Library: a playlist created or a song liked since it
        // was last fetched is a change to exactly this page.
        if (currentFeed == MainViewModel.Feed.LIBRARY) viewModel.onLibraryShown()
    }

    val currentPull = when (currentFeed) {
        MainViewModel.Feed.HOME -> homePull
        MainViewModel.Feed.EXPLORE -> explorePull
        MainViewModel.Feed.LIBRARY -> libraryPull
        null -> null
    }
    val detailListState = remember(detail?.browseId) { LazyListState() }
    val detailTitleDrop = with(LocalDensity.current) { DETAIL_TITLE_DROP.toPx() }
    val detailScrolled by remember(detailListState, detailTitleDrop) {
        derivedStateOf {
            detailListState.firstVisibleItemIndex > 0 ||
                detailListState.firstVisibleItemScrollOffset > detailTitleDrop
        }
    }

    val activeScrollListState = if (detail != null) detailListState else currentListState

    val scrolled by remember(activeScrollListState) {
        derivedStateOf {
            activeScrollListState.firstVisibleItemIndex > 0 ||
                activeScrollListState.firstVisibleItemScrollOffset > 24
        }
    }

    var isScrollingDown by remember { mutableStateOf(false) }

    val globalNestedScrollConnection = remember(player.song) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (player.song != null) {
                    if (available.y < -8f) {
                        isScrollingDown = true
                    } else if (available.y > 8f) {
                        isScrollingDown = false
                    }
                } else {
                    isScrollingDown = false
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(selectedTab, currentSubpage, detail) {
        isScrollingDown = false
    }

    val tabs = remember {
        listOf(
            BottomTab("Home", NUViAIcons.Home, NUViAIcons.HomeFilled),
            BottomTab("Explore", NUViAIcons.Explore, NUViAIcons.ExploreFilled),
            BottomTab("Library", NUViAIcons.Library, NUViAIcons.LibraryFilled),
            BottomTab("Search", NUViAIcons.Search, NUViAIcons.SearchFilled),
        )
    }

    val scope = rememberCoroutineScope()

    /**
     * A video-tagged [Song] is swapped for its catalogue audio release
     * before the queue, the notification or YouTube's own history ever see
     * it — see [YtMusicRepository.resolveAudio]. Plain songs pass through
     * this untouched and unawaited (`isVideo` is false, so the suspend call
     * returns immediately), so this costs nothing on the common path.
     */
    suspend fun List<Song>.resolvedForQueue(): List<Song> = coroutineScope {
        map { async { YtMusicRepository.resolveAudio(it) } }.awaitAll()
    }

    val play: (List<Song>, Int) -> Unit = remember(controller, scope) {
        { songs, index ->
            val targetSong = songs.getOrNull(index)
            if (targetSong != null) {
                activeNowPlayingSong = targetSong
                showNowPlaying = true
            }
            scope.launch {
                val songToPlay = targetSong ?: return@launch
                val cached = YtMusicRepository.getCachedResolvedAudio(songToPlay)
                val queued = songs.toMutableList()
                if (cached != null) {
                    queued[index] = cached
                }
                // Prepare queue items off the main thread to ensure immediate first frame
                val shuffled = QueueShuffle.enabled.value
                val (items, startIndex) = withContext(Dispatchers.Default) {
                    val queue = if (shuffled) QueueShuffle.startingOrder(queued, index) else queued
                    val startIdx = if (shuffled) 0 else index
                    Pair(queue.map { it.toMediaItem() }, startIdx)
                }
                // Start playback immediately
                controller?.setMediaItems(items, startIndex, 0L)
                controller?.prepare()
                controller?.play()

                // Background resolution for starting track if video and not cached
                if (cached == null && songToPlay.isVideo) {
                    launch {
                        val resolved = YtMusicRepository.resolveAudio(songToPlay)
                        if (resolved.videoId != songToPlay.videoId) {
                            val c = controller ?: return@launch
                            val at = (0 until c.mediaItemCount)
                                .firstOrNull { c.getMediaItemAt(it).mediaId == songToPlay.videoId }
                                ?: return@launch
                            if (c.currentMediaItemIndex != at) {
                                c.replaceMediaItem(at, resolved.toMediaItem())
                            }
                            if (activeNowPlayingSong?.videoId == songToPlay.videoId) {
                                activeNowPlayingSong = resolved
                            }
                        }
                    }
                }

                // The rest of the queue resolves in background and patches into the queue
                queued.forEachIndexed { i, song ->
                    if (i == index || !song.isVideo) return@forEachIndexed
                    launch {
                        val resolved = YtMusicRepository.resolveAudio(song)
                        if (resolved.videoId == song.videoId) return@launch
                        val c = controller ?: return@launch
                        val at = (0 until c.mediaItemCount)
                            .firstOrNull { c.getMediaItemAt(it).mediaId == song.videoId }
                            ?: return@launch
                        c.replaceMediaItem(at, resolved.toMediaItem())
                    }
                }
            }
        }
    }

    /**
     * A song picked on its own — off a home card or a search hit — starts a
     * station rather than queueing the list it was shown in. Searching
     * "Perfect" and tapping the top hit otherwise queues twenty covers and
     * remixes of the same song. Album, artist and playlist pages keep [play],
     * where the surrounding list *is* the thing the user asked for.
     */
    val playRadio: (Song) -> Unit = remember(controller, scope) {
        { song ->
            activeNowPlayingSong = song
            showNowPlaying = true
            autoplaySeed = song.videoId
            scope.launch {
                val cached = YtMusicRepository.getCachedResolvedAudio(song)
                val starting = cached ?: song
                autoplaySeed = starting.videoId
                // Start immediately
                controller?.playSongs(listOf(starting), 0)

                val resolved = if (cached != null) cached else YtMusicRepository.resolveAudio(song)
                if (resolved.videoId != starting.videoId) {
                    autoplaySeed = resolved.videoId
                    if (activeNowPlayingSong?.videoId == starting.videoId) {
                        activeNowPlayingSong = resolved
                    }
                }
                // Radio is YouTube's, and only YouTube's — see [youtubeSeedFor].
                val seed = youtubeSeedFor(resolved) ?: return@launch
                YtMusicRepository.radio(seed).onSuccess { related ->
                    // The user may have moved on while the mix was loading.
                    if (controller?.currentMediaItem?.mediaId != resolved.videoId &&
                        controller?.currentMediaItem?.mediaId != starting.videoId
                    ) return@onSuccess
                    val extra = QueueBuilder.extend(listOf(resolved), related, RADIO_BATCH)
                    if (extra.isNotEmpty()) {
                        controller?.addMediaItems(
                            extra.resolvedForQueue().map {
                                it.copy(fromAutoplay = true).toMediaItem()
                            },
                        )
                    }
                }
            }
        }
    }
    val addToQueue: (Song) -> Unit = remember(controller, scope) {
        { song ->
            scope.launch {
                val resolved = YtMusicRepository.resolveAudio(song)
                // The end of what the user queued, not the end of the queue: a song
                // asked for by name outranks whatever AutoPlay lined up behind it.
                controller?.let { it.addMediaItem(it.autoplaySectionStart(), resolved.toMediaItem()) }
            }
        }
    }
    val playNext: (Song) -> Unit = remember(controller, scope) {
        { song ->
            scope.launch {
                val resolved = YtMusicRepository.resolveAudio(song)
                controller?.let {
                    it.addMediaItem(
                        (it.currentMediaItemIndex + 1).coerceAtMost(it.mediaItemCount),
                        resolved.toMediaItem(),
                    )
                }
            }
        }
    }
    val onSongSwipe: (Song) -> Unit = remember(playNext, addToQueue) {
        { song ->
            if (AppSettings.swipeToPlayNext.value) playNext(song) else addToQueue(song)
        }
    }

    // ---- Downloads ----
    // Two permissions, and never both on one device: writing to the shared
    // Music folder needs storage access below API 29 and none at all from
    // 29 on, where MediaStore grants an app its own rows; notifications are
    // only asked for from API 33. So the branches below are mutually exclusive
    // by SDK level, and nothing here can stack two dialogs on each other.
    var downloadPending by remember { mutableStateOf<List<Song>>(emptyList()) }
    val notifyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Refusing costs the progress notification, not the download. */ }
    val storagePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val songs = downloadPending
        downloadPending = emptyList()
        when {
            songs.isEmpty() -> Unit
            granted -> songs.forEach { Downloads.enqueue(context, it) }
            // The one case where refusing is fatal: below API 29 there is no
            // other way to reach the Music folder.
            else -> Toast
                .makeText(context, "Storage access is needed to save songs", Toast.LENGTH_SHORT)
                .show()
        }
    }
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.reloadLocalDetail("local:all")
        } else {
            Toast.makeText(context, "Storage permission is required to read local audio files", Toast.LENGTH_SHORT).show()
        }
    }
    // Takes a list so a single tap on an album/playlist header can queue the
    // whole thing — the permission dance only needs to happen once for the
    // batch, not once per track.
    val startDownload: (List<Song>) -> Unit = { requested ->
        val saved = Downloads.saved.value
        // Already on disk, and already queued or running: neither needs asking
        // again. What's left is what a tap on "Download" actually means.
        val songs = requested.filter { it.videoId !in saved }
        if (songs.isNotEmpty()) {
            val needsStorage = DownloadStore.needsLegacyPermission() &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ) != PackageManager.PERMISSION_GRANTED

            // Asked for here rather than at launch because here is where it means
            // something: a download is the first thing this app does that the user
            // is expected to walk away from.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notifyPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (needsStorage) {
                downloadPending = songs
                storagePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                songs.forEach { Downloads.enqueue(context, it) }
            }
        }
        if (requested.size > 1) {
            val message = if (songs.isEmpty()) {
                "Already downloaded"
            } else {
                "Downloading ${songs.size} song" + if (songs.size == 1) "" else "s"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Content padding leaves room for the frosted bar above and the tab bar
    // (plus mini player) below, so nothing is ever trapped under the glass.
    val hasPlayingSong = player.song != null
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val rootTopContentInset = statusBarTop + 8.dp
    val subpageTopContentInset = statusBarTop + 52.dp + 16.dp
    val bottomContentInset = (if (hasPlayingSong) (68.dp + MINI_PLAYER_BOTTOM_BAR_GAP + 56.dp + 16.dp) else (68.dp + 16.dp)) + navBarBottom
    val bottomContentInset = (if (hasPlayingSong) (68.dp + MINI_PLAYER_BOTTOM_BAR_GAP + 56.dp + 16.dp) else (68.dp + 16.dp)) + navBarBottom + 24.dp

    val listPadding = remember(hasPlayingSong, rootTopContentInset, subpageTopContentInset, bottomContentInset, isSubpageActive, detail) {
        val topPadding = if (isSubpageActive || detail != null) subpageTopContentInset else rootTopContentInset
        PaddingValues(
            top = topPadding,
            bottom = bottomContentInset,
        )
    }

    // What colour the page currently under the bars is. The fades either end
    // of the screen are flat colour wherever their blur has least to say, so
    // handing them the theme's background puts a black band on a page that is
    // washed in an artwork's colour instead. Off a detail page this resolves
    // to the theme's background anyway, which is exactly right there.
    val detailPalette = rememberArtworkPalette(detail?.thumbnailUrl)

    val openBrowseActionsForShelfItem: (ShelfItem) -> Unit = remember(viewModel) {
        { item ->
            val id = item.browseId
            if (id != null) {
                val userPlaylist = viewModel.editablePlaylist(id)
                browseActions = BrowseTarget(
                    browseId = id,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    type = if (userPlaylist != null) BrowseType.PLAYLIST else BrowseType.OTHER,
                    playlist = userPlaylist,
                    fromCard = true,
                )
            }
        }
    }

    val isItemPinnedHelper: (ShelfItem) -> Boolean = remember(pinnedPlaylists) {
        { item ->
            val id = item.browseId
            id != null && (
                id in pinnedPlaylists ||
                id.removePrefix("VL") in pinnedPlaylists ||
                "VL$id" in pinnedPlaylists
            )
        }
    }

    val withBrowseSongs: (BrowseTarget, (List<Song>) -> Unit) -> Unit = remember(viewModel, context) {
        { target, block ->
            if (target.songs.isNotEmpty()) {
                block(target.songs)
            } else if (target.browseId != null) {
                viewModel.collectSongs(target.browseId, target.thumbnailUrl) { result ->
                    result.onSuccess { songs ->
                        if (songs.isNotEmpty()) {
                            block(songs)
                        } else {
                            Toast.makeText(context, "No songs found", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure { err ->
                        Toast.makeText(context, err.message ?: "Failed to load songs", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val onShelfItemClick: (ShelfItem) -> Unit = remember(playRadio, viewModel) {
        { item ->
            when {
                item.videoId != null -> playRadio(
                    Song(
                        videoId = item.videoId,
                        title = item.title,
                        artist = item.subtitle,
                        thumbnailUrl = item.thumbnailUrl,
                    ),
                )
                item.browseId == "nuvia:history" -> navigateToSubpage(SubpageDestination.HISTORY)
                item.browseId == "nuvia:replay" -> navigateToSubpage(SubpageDestination.REPLAY)
                item.browseId != null -> viewModel.openDetail(
                    browseId = item.browseId,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                )
            }
        }
    }
    val onSignInAction = remember { { showLogin = true } }
    val onHomeAccountClick = remember(signedIn) {
        {
            if (signedIn) {
                showAccountSelector = true
            } else {
                navigateToSubpage(SubpageDestination.SETTINGS)
            }
        }
    }
    val onHomeDownloadClick = remember { { showDownloadManager = true } }
    val onRefreshHome = remember(viewModel) { { viewModel.refresh(MainViewModel.Feed.HOME) } }
    val onRefreshExplore = remember(viewModel) { { viewModel.refresh(MainViewModel.Feed.EXPLORE) } }
    val onRefreshLibrary = remember(viewModel) { { viewModel.refresh(MainViewModel.Feed.LIBRARY) } }

    NUViATheme(
        colors = nuviaColors,
        darkTheme = darkTheme,
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(nuviaColors.surfaceOled)
                .nestedScroll(globalNestedScrollConnection),
        ) {
            if (onboardingActive) {
                val currentOnboardingStep = onboardingBackstack.lastOrNull() ?: OnboardingStep.GETTING_STARTED
                val canGoBack = onboardingBackstack.size > 1

                BackHandler(enabled = canGoBack) {
                    if (onboardingBackstack.size > 1) {
                        onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                    }
                }

                AnimatedContent(
                    targetState = currentOnboardingStep,
                    transitionSpec = {
                        (fadeIn(androidx.compose.animation.core.tween(350, easing = CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f))) +
                            slideInVertically(androidx.compose.animation.core.tween(350, easing = CubicBezierEasing(0.2f, 0.0f, 0.1f, 1.0f))) { it / 6 })
                            .togetherWith(
                                fadeOut(androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "onboarding_flow_content",
                ) { step ->
                    when (step) {
                        OnboardingStep.GETTING_STARTED -> {
                            GettingStartedScreen(
                                onContinue = {
                                    onboardingBackstack.add(OnboardingStep.DEVELOPER)
                                },
                                onSkip = {
                                    onboardingBackstack.add(OnboardingStep.DEVELOPER)
                                },
                            )
                        }
                        OnboardingStep.DEVELOPER -> {
                            DeveloperAboutScreen(
                                onContinue = {
                                    onboardingBackstack.add(OnboardingStep.ACCOUNT_CONNECT)
                                },
                                onBack = {
                                    if (onboardingBackstack.size > 1) {
                                        onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                                    }
                                },
                            )
                        }
                        OnboardingStep.ACCOUNT_CONNECT -> {
                            AccountConnectScreen(
                                onConnectAccount = {
                                    onboardingBackstack.add(OnboardingStep.LOGIN)
                                },
                                onSkip = {
                                    onboardingBackstack.add(OnboardingStep.SURVEY)
                                },
                                onBack = {
                                    if (onboardingBackstack.size > 1) {
                                        onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                                    }
                                },
                            )
                        }
                        OnboardingStep.LOGIN -> {
                            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                                Column(Modifier.fillMaxSize()) {
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        IconButton(onClick = {
                                            if (onboardingBackstack.size > 1) {
                                                onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                                            }
                                        }) {
                                            Icon(
                                                Icons.Rounded.Close,
                                                contentDescription = "Close",
                                                tint = MaterialTheme.colorScheme.onBackground,
                                            )
                                        }
                                        Text(
                                            "Sign in to YouTube Music",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                    }
                                    YtMusicLoginScreen(
                                        onCookiesCaptured = { cookie ->
                                            viewModel.onSignedIn(cookie)
                                            onboardingBackstack.add(OnboardingStep.LOGIN_SUCCESS)
                                        },
                                    )
                                }
                            }
                        }
                        OnboardingStep.LOGIN_SUCCESS -> {
                            LoginSuccessScreen(
                                accountName = account?.name,
                                accountEmail = account?.email,
                                accountThumbnailUrl = account?.thumbnailUrl,
                                onContinue = {
                                    AppSettings.setOnboardingCompleted(true)
                                    showSurveyEntranceOverlay = true
                                    onboardingActive = false
                                },
                                onBack = {
                                    if (onboardingBackstack.size > 1) {
                                        onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                                    }
                                },
                            )
                        }
                        OnboardingStep.SURVEY -> {
                            PreferenceSurveyScreen(
                                onFinish = { genres, moods, languages, moments, discovery, content ->
                                    AppSettings.setPreferredMusicPreferences(
                                        genres = genres,
                                        moods = moods,
                                        languages = languages,
                                        moments = moments,
                                        discovery = discovery,
                                        content = content,
                                    )
                                    AppSettings.setOnboardingCompleted(true)
                                    showSurveyEntranceOverlay = true
                                    onboardingActive = false
                                },
                                onSkip = {
                                    AppSettings.setOnboardingCompleted(true)
                                    showSurveyEntranceOverlay = true
                                    onboardingActive = false
                                },
                                onBack = {
                                    if (onboardingBackstack.size > 1) {
                                        onboardingBackstack.removeAt(onboardingBackstack.lastIndex)
                                    }
                                },
                            )
                        }
                        OnboardingStep.COMPLETED -> Unit
                    }
                }
            } else {
                NUViAAmbientBackground()
        // Subpage and detail stack back handling:
        BackHandler(enabled = isSubpageActive) {
            popSubpage()
        }
        BackHandler(enabled = detail != null && !isSubpageActive) {
            viewModel.closeDetail()
        }
        BackHandler(enabled = detail == null && !isSubpageActive && selectedTab != TAB_HOME) {
            selectedTab = TAB_HOME
        }
        BackHandler(enabled = showListenBrainzLogin) { showListenBrainzLogin = false }
        BackHandler(enabled = showLastfmLogin) { showLastfmLogin = false }
        BackHandler(enabled = discordDialog != null) { discordDialog = null }

        AnimatedContent(
            targetState = when {
                showDeveloperDiagnostics -> "developer_diagnostics"
                showAbout -> "about"
                showWhatsNew -> "whats_new"
                showDeveloperAbout -> "developer_about"
                showDiscord -> "discord"
                showSpotifyCanvasAuth -> "spotify_canvas_auth"
                showListenTogether -> "listen_together"
                showEqualizer -> "equalizer"
                showHistory -> "history"
                showReplay -> "replay"
                showAccountScrobbling -> "account_scrobbling"
                showSources -> "sources"
                showSettings -> "settings"
                currentSubpage != null -> when (currentSubpage) {
                    SubpageDestination.DEVELOPER_DIAGNOSTICS -> "developer_diagnostics"
                    SubpageDestination.ABOUT -> "about"
                    SubpageDestination.WHATS_NEW -> "whats_new"
                    SubpageDestination.DISCORD -> "discord"
                    SubpageDestination.SPOTIFY_CANVAS -> "spotify_canvas_auth"
                    SubpageDestination.LISTEN_TOGETHER -> "listen_together"
                    SubpageDestination.EQUALIZER -> "equalizer"
                    SubpageDestination.HISTORY -> "history"
                    SubpageDestination.REPLAY -> "replay"
                    SubpageDestination.ACCOUNT_SCROBBLING -> "account_scrobbling"
                    SubpageDestination.SOURCES -> "sources"
                    SubpageDestination.SETTINGS -> "settings"
                }
                detail != null -> detail.browseId
                else -> "tab:$selectedTab"
            },
            transitionSpec = {
                if (reduceAnimation) {
                    ContentTransform(
                        targetContentEnter = fadeIn(animationSpec = androidx.compose.animation.core.tween(120)),
                        initialContentExit = fadeOut(animationSpec = androidx.compose.animation.core.tween(100)),
                        sizeTransform = null,
                    )
                } else {
                    val isInitialTab = initialState.startsWith("tab:")
                    val isTargetTab = targetState.startsWith("tab:")

                    if (isInitialTab && isTargetTab) {
                        val initialIndex = TabNavigation.parseTabIndex(initialState) ?: 0
                        val targetIndex = TabNavigation.parseTabIndex(targetState) ?: 0
                        TabNavigation.tabTransition(
                            initialIndex = initialIndex,
                            targetIndex = targetIndex,
                            reduceMotion = false,
                        )
                    } else if (!isTargetTab && isInitialTab) {
                        // Forward push to subpage (detail, settings, sources, discord, etc.)
                        ContentTransform(
                            targetContentEnter = (fadeIn(animationSpec = androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing)) +
                                slideInVertically(animationSpec = androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing)) { (it * 0.05f).roundToInt() } +
                                scaleIn(initialScale = 0.975f, animationSpec = androidx.compose.animation.core.tween(250, easing = FastOutSlowInEasing))),
                            initialContentExit = (fadeOut(animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.985f, animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing))),
                            targetContentZIndex = 1f,
                            sizeTransform = null,
                        )
                    } else if (isTargetTab && !isInitialTab) {
                        // Pop return from subpage to tab
                        ContentTransform(
                            targetContentEnter = (fadeIn(animationSpec = androidx.compose.animation.core.tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.985f, animationSpec = androidx.compose.animation.core.tween(220, easing = FastOutSlowInEasing))),
                            initialContentExit = (fadeOut(animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing)) +
                                slideOutVertically(animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing)) { (it * 0.05f).roundToInt() }),
                            targetContentZIndex = 0f,
                            sizeTransform = null,
                        )
                    } else {
                        // Navigation between subpages
                        ContentTransform(
                            targetContentEnter = (fadeIn(animationSpec = androidx.compose.animation.core.tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.98f, animationSpec = androidx.compose.animation.core.tween(220, easing = FastOutSlowInEasing))),
                            initialContentExit = (fadeOut(animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.985f, animationSpec = androidx.compose.animation.core.tween(180, easing = FastOutSlowInEasing))),
                            sizeTransform = null,
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
            label = "content",
        ) { key ->
            Box(modifier = Modifier.fillMaxSize()) {
            val page = detailStack.lastOrNull()?.takeIf {
                it.browseId == key && key != "settings" && key != "account_scrobbling" && key != "discord" && key != "sources" &&
                    key != "listen_together" && key != "equalizer" && key != "history" && key != "replay" && key != "spotify_canvas_auth" &&
                    key != "developer_about" && key != "developer_diagnostics" && key != "about" && key != "whats_new"
            }
            if (key == "developer_diagnostics") {
                DeveloperDiagnosticsScreen(
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (key == "about") {
                AboutScreen(
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (key == "whats_new") {
                WhatsNewScreen(
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (key == "developer_about") {
                DeveloperAboutScreen(
                    onContinue = { popSubpage() },
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (key == "discord") {
                DiscordScreen(
                    song = player.song,
                    positionMs = player.positionMs,
                    positionProvider = playbackPositionProvider,
                    durationMs = player.durationMs,
                    onOpenLogin = { showDiscordLogin = true },
                    onOpenDialog = { discordDialog = it },
                    contentPadding = listPadding,
                )
            } else if (key == "sources") {
                JioSaavnSourceScreen(
                    contentPadding = listPadding,
                )
            } else if (key == "account_scrobbling") {
                AccountAndScrobblingScreen(
                    signedIn = signedIn,
                    account = account,
                    onSignIn = {
                        subpageStack.clear()
                        showLogin = true
                    },
                    onSignOut = { viewModel.signOut() },
                    onOpenListenBrainzLogin = { showListenBrainzLogin = true },
                    onOpenLastfmLogin = { showLastfmLogin = true },
                    onOpenDiscord = { navigateToSubpage(SubpageDestination.DISCORD) },
                    contentPadding = listPadding,
                )
            } else if (key == "settings") {
                SettingsScreen(
                    signedIn = signedIn,
                    account = account,
                    onSignIn = {
                        subpageStack.clear()
                        showLogin = true
                    },
                    onSignOut = { viewModel.signOut() },
                    onAccountScrobbling = { navigateToSubpage(SubpageDestination.ACCOUNT_SCROBBLING) },
                    onSources = { navigateToSubpage(SubpageDestination.SOURCES) },
                    onLyricsSources = { showLyricsSources = true },
                    onEqualizer = { navigateToSubpage(SubpageDestination.EQUALIZER) },
                    onListenTogether = { navigateToSubpage(SubpageDestination.LISTEN_TOGETHER) },
                    onSpotifyCanvas = { navigateToSubpage(SubpageDestination.SPOTIFY_CANVAS) },
                    onReplay = { navigateToSubpage(SubpageDestination.REPLAY) },
                    onHistory = { navigateToSubpage(SubpageDestination.HISTORY) },
                    onDownloadManager = { showDownloadManager = true },
                    onAccountSelector = { showAccountSelector = true },
                    onDeveloperAbout = { navigateToSubpage(SubpageDestination.DEVELOPER_DIAGNOSTICS) },
                    onDeveloperDiagnostics = { navigateToSubpage(SubpageDestination.DEVELOPER_DIAGNOSTICS) },
                    onAbout = { navigateToSubpage(SubpageDestination.ABOUT) },
                    onWhatsNew = { navigateToSubpage(SubpageDestination.WHATS_NEW) },
                    onOpenDiscord = { navigateToSubpage(SubpageDestination.DISCORD) },
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                    scrollState = settingsScrollState,
                )
            } else if (key == "listen_together") {
                ListenTogetherScreen(
                    signedIn = signedIn,
                    onSignIn = { showLogin = true },
                    onBack = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (key == "equalizer") {
                EqualizerScreen(
                    contentPadding = listPadding,
                )
            } else if (key == "history") {
                val historySongs by LocalHistoryStore.history.collectAsStateWithLifecycle()
                val historyListState = rememberLazyListState()
                HistoryScreen(
                    state = UiState.Success(historySongs),
                    listState = historyListState,
                    onSongClick = play,
                    onSongLongPress = { songActions = it },
                    onSongSwipe = onSongSwipe,
                    onRetry = {},
                    contentPadding = listPadding,
                    onClearHistory = { LocalHistoryStore.clear() },
                )
            } else if (key == "replay") {
                ReplayScreen(
                    onBack = { popSubpage() },
                    onSongClick = playRadio,
                    contentPadding = listPadding,
                )
            } else if (key == "spotify_canvas_auth") {
                SpotifyCanvasAuthScreen(
                    onNavigateUp = { popSubpage() },
                    contentPadding = listPadding,
                )
            } else if (page != null && page.browseId.startsWith("local:")) {
                // Local Music and Downloads — both the tabbed Songs / Artists /
                // Albums view. Two folders of tracks already on the device, so
                // there is nothing to tell them apart on screen beyond what is
                // in them and what to say when that is nothing.
                val localState = page.songs
                val localSongs = (localState as? com.music.nuvia.data.model.UiState.Success)
                    ?.data.orEmpty()
                LocalMusicScreen(
                    songs = localSongs,
                    onSongClick = play,
                    onSongLongPress = { songActions = it },
                    onSongSwipe = onSongSwipe,
                    onShuffle = { songs ->
                        QueueShuffle.enableForNextQueue()
                        play(songs, songs.indices.random())
                    },
                    emptyMessage = (localState as? com.music.nuvia.data.model.UiState.Error)
                        ?.message,
                    title = page.title,
                    onBack = { viewModel.closeDetail() },
                    contentPadding = listPadding,
                )
            } else if (page != null) {
                // An album page's rows carry no album name of their own — the
                // release is billed once, in the header the rows hang under — so
                // the page title is stamped on as they leave for the download
                // queue or the track menu. Without it every track saved from an
                // album arrives in the Downloads folder with nothing to group it
                // under, and its Albums tab stays empty however much is in it.
                val withAlbum: (Song) -> Song = { song ->
                    if (page.type == BrowseType.ALBUM) {
                        song.copy(albumName = song.albumName ?: page.title)
                    } else {
                        song
                    }
                }
                DetailScreen(
                    page = page,
                    listState = detailListState,
                    currentPlayingSongId = player.song?.videoId,
                    isPlaying = player.isPlaying,
                    onSongClick = play,
                    onSongLongPress = { songActions = withAlbum(it) },
                    onSongSwipe = onSongSwipe,
                    onShuffle = { songs ->
                        // Shuffle goes on first so the queue is built shuffled
                        // as it is set — the random pick here only decides
                        // which track leads it.
                        QueueShuffle.enableForNextQueue()
                        play(songs, songs.indices.random())
                    },
                    onSectionItemClick = { item ->
                        item.browseId?.let { id ->
                            viewModel.openDetail(
                                browseId = id,
                                title = item.title,
                                subtitle = item.subtitle,
                                thumbnailUrl = item.thumbnailUrl,
                                type = BrowseType.ALBUM,
                            )
                        }
                    },
                    onDownloadAll = { songs -> startDownload(songs.map(withAlbum)) },
                    onArtistClick = { id, name ->
                        viewModel.openDetail(id, name, "Artist", null, BrowseType.ARTIST)
                    },
                    onAddSuggested = { song -> viewModel.addSuggestedSong(page.browseId, song) },
                    // Saving is an account action, so it isn't offered to a
                    // guest at all — same as the like and add-to-playlist rows
                    // in the track menu.
                    onToggleLibrary = if (signedIn) {
                        { viewModel.toggleLibrary(page.browseId) }
                    } else {
                        null
                    },
                    onOpenBrowseActions = { target -> browseActions = target },
                    onRetry = { viewModel.retryDetail(page.browseId) },
                    contentPadding = listPadding,
                )
            } else {
                val tabIndex = TabNavigation.parseTabIndex(key) ?: selectedTab
                when (tabIndex) {
                    TAB_HOME -> HomeScreen(
                        state = homeState,
                        listState = homeListState,
                        account = account,
                        onAccountClick = onHomeAccountClick,
                        onDownloadClick = onHomeDownloadClick,
                        signedIn = signedIn,
                        onSignIn = onSignInAction,
                        onItemClick = onShelfItemClick,
                        onItemLongPress = openBrowseActionsForShelfItem,
                        isItemPinned = isItemPinnedHelper,
                        onRetry = viewModel::loadHome,
                        refreshing = MainViewModel.Feed.HOME in refreshing,
                        onRefresh = onRefreshHome,
                        pullState = homePull,
                        contentPadding = listPadding,
                        onLoadMore = viewModel::loadMoreHome,
                        loadingMore = homeLoadingMore,
                    )
                    TAB_EXPLORE -> ExploreScreen(
                        state = exploreState,
                        listState = exploreListState,
                        onItemClick = onShelfItemClick,
                        onRetry = viewModel::loadExplore,
                        refreshing = MainViewModel.Feed.EXPLORE in refreshing,
                        onRefresh = onRefreshExplore,
                        pullState = explorePull,
                        contentPadding = listPadding,
                    )
                    TAB_LIBRARY -> LibraryScreen(
                        signedIn = signedIn,
                        state = libraryState,
                        listState = libraryListState,
                        onShelfItemClick = { item ->
                            item.browseId?.let { id ->
                                if (id == "nuvia:history") {
                                    navigateToSubpage(SubpageDestination.HISTORY)
                                } else if (id == "nuvia:replay") {
                                    navigateToSubpage(SubpageDestination.REPLAY)
                                } else if (id == "local:all" && !LocalMediaRepository.hasStoragePermission(context)) {
                                    val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        Manifest.permission.READ_MEDIA_AUDIO
                                    } else {
                                        Manifest.permission.READ_EXTERNAL_STORAGE
                                    }
                                    mediaPermissionLauncher.launch(perm)
                                } else {
                                    viewModel.openDetail(
                                        browseId = id,
                                        title = item.title,
                                        subtitle = item.subtitle,
                                        thumbnailUrl = item.thumbnailUrl,
                                    )
                                }
                            }
                        },
                        onShelfItemLongPress = openBrowseActionsForShelfItem,
                        onNewPlaylist = { creatingPlaylist = true },
                        onSignIn = onSignInAction,
                        onRetry = viewModel::loadLibrary,
                        refreshing = MainViewModel.Feed.LIBRARY in refreshing,
                        onRefresh = onRefreshLibrary,
                        pullState = libraryPull,
                        contentPadding = listPadding,
                    )
                    TAB_SEARCH -> SearchScreen(
                        query = query,
                        onQueryChange = viewModel::onQueryChange,
                        filter = filter,
                        onFilterChange = viewModel::onFilterChange,
                        results = results,
                        listState = searchListState,
                        focusTrigger = searchFocusTrigger,
                        // Search hits are alternatives to each other, not a running
                        // order — play the one tapped and build a station from it.
                        onSongClick = { songs, index ->
                            songs.getOrNull(index)?.let {
                                // Acting on a hit is what makes the query worth
                                // keeping — see MainViewModel.recordSearch.
                                viewModel.recordSearch()
                                playRadio(it)
                            }
                        },
                        onSongLongPress = { songActions = it },
                        onSongSwipe = onSongSwipe,
                        onBrowseClick = { item ->
                            viewModel.recordSearch()
                            viewModel.openDetail(
                                browseId = item.browseId,
                                title = item.title,
                                subtitle = item.subtitle,
                                thumbnailUrl = item.thumbnailUrl,
                                type = item.type,
                            )
                        },
                        history = searchHistory,
                        suggestions = searchSuggestions,
                        onSubmit = viewModel::submitSearch,
                        // A suggestion and a recent search are the same act — a
                        // term picked out of a list rather than typed — so they run
                        // through the same path and both land in the history.
                        onSuggestionClick = viewModel::searchFor,
                        onHistoryClick = viewModel::searchFor,
                        onHistoryRemove = viewModel::removeSearch,
                        onHistoryClear = viewModel::clearSearchHistory,
                        contentPadding = listPadding,
                    )
                    else -> HomeScreen(
                        state = homeState,
                        listState = homeListState,
                        account = account,
                        onAccountClick = onHomeAccountClick,
                        onDownloadClick = onHomeDownloadClick,
                        signedIn = signedIn,
                        onSignIn = onSignInAction,
                        onItemClick = onShelfItemClick,
                        onItemLongPress = openBrowseActionsForShelfItem,
                        isItemPinned = isItemPinnedHelper,
                        onRetry = viewModel::loadHome,
                        refreshing = MainViewModel.Feed.HOME in refreshing,
                        onRefresh = onRefreshHome,
                        pullState = homePull,
                        contentPadding = listPadding,
                        onLoadMore = viewModel::loadMoreHome,
                        loadingMore = homeLoadingMore,
                    )
                }
            }
            }
        }

        // A detail page's artwork runs up under the status bar, so the bar
        // there is a fade rather than a pane — see [TopFadeBlur]. Drawn before
        // the bar so the bar's own content sits on top of it.
        val isSubpageActive = showSettings || showAccountScrobbling || showSources || showDiscord ||
            showListenTogether || showEqualizer || showHistory || showReplay || showSpotifyCanvasAuth ||
            showDeveloperAbout || showDeveloperDiagnostics || showAbout || showWhatsNew

        val hasDedicatedTopBar = showSettings || showDeveloperAbout || showDeveloperDiagnostics ||
            showAbout || showWhatsNew || showSpotifyCanvasAuth || isLocalDetail || showReplay ||
            (!isSubpageActive && detail == null)
        if (!hasDedicatedTopBar) {
            FrostedTopBar(
                title = when {
                    showDeveloperDiagnostics -> "Developer & Diagnostics"
                    showAbout -> "About NUViA"
                    showWhatsNew -> "What's New in NUViA"
                    showDeveloperAbout -> "Developer & Diagnostics"
                    showDiscord -> "Discord"
                    showAccountScrobbling -> "Account & scrobbling"
                    showSources -> "Music source"
                    showSettings -> "Settings"
                    showListenTogether -> "Listen together"
                    showEqualizer -> "Equalizer"
                    showHistory -> "History"
                    showReplay -> "Replay"
                    showSpotifyCanvasAuth -> "Spotify Canvas"
                    detail != null -> detail.title
                    else -> tabs[selectedTab].let {
                        it.label
                    }
                },
                hazeState = hazeState,
                ownBackdrop = detail == null || isLocalDetail,
                // Search has no large in-list header to hand the title back to —
                // the field takes that space — so its bar title is always up.
                scrolled = when {
                    showSettings -> false
                    selectedTab == TAB_HOME && !isSubpageActive && detail == null -> false
                    isSubpageActive -> true
                    detail != null -> detailScrolled
                    else -> scrolled || selectedTab == TAB_SEARCH
                },
                refreshing = currentFeed != null && currentFeed in refreshing,
                pullFraction = { currentPull?.distanceFraction ?: 0f },
                onBack = when {
                    isSubpageActive -> ({ popSubpage() })
                    detail != null -> ({ viewModel.closeDetail(); Unit })
                    else -> null
                },
                modifier = Modifier.align(Alignment.TopCenter),
                actions = {
                    if (!isSubpageActive) {
                        TopBarDownloadButton(onClick = { showDownloadManager = true })
                        TopBarAccountButton(
                            account = account,
                            onClick = {
                                if (signedIn) {
                                    showAccountSelector = true
                                } else {
                                    navigateToSubpage(SubpageDestination.SETTINGS)
                                }
                            },
                        )
                    }
                },
            )
        }

        val shouldShowNavigationShell = !onboardingActive && !showNowPlaying && !showLogin
        if (shouldShowNavigationShell) {
            val shellProgress by animateFloatAsState(
                targetValue = if (isScrollingDown && player.song != null) 1f else 0f,
                animationSpec = if (reduceAnimation) {
                    androidx.compose.animation.core.snap()
                } else {
                    spring(
                        dampingRatio = 0.85f,
                        stiffness = 350f,
                    )
                },
                label = "navShellProgress",
            )
            val density = LocalDensity.current
            val navBarTravel = with(density) { 110.dp.toPx() }
            val miniPlayerTravel = with(density) { (68.dp + MINI_PLAYER_BOTTOM_BAR_GAP).toPx() }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                // 1. Floating Bottom Bar (translates down & fades when compact, remains fixed when player.song == null)
                FloatingBottomBar(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    hazeState = hazeState,
                    ownBackdrop = false,
                    modifier = Modifier.graphicsLayer {
                        val progress = if (player.song != null) shellProgress else 0f
                        translationY = progress * navBarTravel
                        alpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
                    },
                    onTabSelected = { index ->
                        if (shellProgress > 0.5f) return@FloatingBottomBar
                        if (index == TAB_SEARCH && selectedTab == TAB_SEARCH && !isSubpageActive && detail == null) {
                            searchFocusTrigger++
                            return@FloatingBottomBar
                        }
            PersistentNavigationShell(
                modifier = Modifier.align(Alignment.BottomCenter),
                isScrollingDown = isScrollingDown,
                reduceAnimation = reduceAnimation,
                song = player.song,
                isPlaying = player.isPlaying,
                isLoading = player.isLoading,
                hasPrevious = player.hasPrevious,
                durationMs = player.durationMs,
                playbackPositionProvider = playbackPositionProvider,
                tabs = tabs,
                selectedTab = selectedTab,
                hazeState = hazeState,
                isSubpageActive = isSubpageActive,
                isDetailActive = detail != null,
                onTabSelected = { index ->
                    if (index == TAB_SEARCH && selectedTab == TAB_SEARCH && !isSubpageActive && detail == null) {
                        searchFocusTrigger++
                    } else if (index == selectedTab && detail == null && !isSubpageActive) {
                        // Keep current tab active
                    } else {
                        if (index != TAB_SEARCH) {
                            searchFocusTrigger = 0
                        }
                        if (index == selectedTab && detail == null && !isSubpageActive) {
                            return@FloatingBottomBar
                        }
                        dismissAllSubpages()
                        selectedTab = index
                    },
                )

                // 2. MiniPlayer Row (centered, translates down to bottom, flanked by Home & Search on scroll down)
                player.song?.let { song ->
                    val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = navBarsBottomPadding + 68.dp + MINI_PLAYER_BOTTOM_BAR_GAP)
                            .graphicsLayer {
                                translationY = shellProgress * miniPlayerTravel
                            }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Home button (emerges on scroll down with Liquid Glass)
                        Box(
                            modifier = Modifier
                                .layout { measurable, constraints ->
                                    val w = (48.dp.toPx() * shellProgress).roundToInt()
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = w,
                                            maxWidth = w,
                                            minHeight = 48.dp.roundToPx(),
                                            maxHeight = 48.dp.roundToPx(),
                                        )
                                    )
                                    layout(w, placeable.height) {
                                        placeable.placeRelative(0, 0)
                                    }
                                }
                                .graphicsLayer {
                                    alpha = shellProgress
                                    scaleX = 0.6f + 0.4f * shellProgress
                                    scaleY = 0.6f + 0.4f * shellProgress
                                }
                                .clip(CircleShape)
                                .nuviaTactilePress()
                                .nuviaGlass(tier = NUViAGlassTier.Elevated, shape = CircleShape, hazeState = hazeState)
                                .nuviaSpecularBorder(CircleShape, 0.75.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = shellProgress > 0.5f,
                                ) {
                                    if (selectedTab != TAB_HOME || isSubpageActive || detail != null) {
                                        dismissAllSubpages()
                                        selectedTab = TAB_HOME
                                    }
                                    scope.launch {
                                        homeListState.animateScrollToItem(0)
                                    }
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selectedTab == TAB_HOME && !isSubpageActive && detail == null) NUViAIcons.HomeFilled else NUViAIcons.Home,
                                contentDescription = "Home",
                                tint = if (selectedTab == TAB_HOME && !isSubpageActive && detail == null) nuviaColors.primary else nuviaColors.textPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(
                            modifier = Modifier.layout { measurable, constraints ->
                                val w = (8.dp.toPx() * shellProgress).roundToInt()
                                layout(w, 0) {}
                            }
                        )

                        // Centered MiniPlayer
                        Box(modifier = Modifier.weight(1f)) {
                            MiniPlayer(
                                song = song,
                                isPlaying = player.isPlaying,
                                isLoading = player.isLoading,
                                hazeState = hazeState,
                                ownBackdrop = false,
                                onPlayPause = {
                                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                                },
                                onNext = { controller?.seekToNextMediaItem() },
                                onPrevious = if (player.hasPrevious) { { controller?.seekToPreviousMediaItem() } } else null,
                                onExpand = {
                                    activeNowPlayingSong = player.song
                                    showNowPlaying = true
                                },
                                progressProvider = {
                                    if (player.durationMs > 0) {
                                        (playbackPositionState.value.toFloat() / player.durationMs).coerceIn(0f, 1f)
                                    } else 0f
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        Spacer(
                            modifier = Modifier.layout { measurable, constraints ->
                                val w = (8.dp.toPx() * shellProgress).roundToInt()
                                layout(w, 0) {}
                            }
                        )

                        // Search button (emerges on scroll down with Liquid Glass)
                        Box(
                            modifier = Modifier
                                .layout { measurable, constraints ->
                                    val w = (48.dp.toPx() * shellProgress).roundToInt()
                                    val placeable = measurable.measure(
                                        constraints.copy(
                                            minWidth = w,
                                            maxWidth = w,
                                            minHeight = 48.dp.roundToPx(),
                                            maxHeight = 48.dp.roundToPx(),
                                        )
                                    )
                                    layout(w, placeable.height) {
                                        placeable.placeRelative(0, 0)
                                    }
                                }
                                .graphicsLayer {
                                    alpha = shellProgress
                                    scaleX = 0.6f + 0.4f * shellProgress
                                    scaleY = 0.6f + 0.4f * shellProgress
                                }
                                .clip(CircleShape)
                                .nuviaTactilePress()
                                .nuviaGlass(tier = NUViAGlassTier.Elevated, shape = CircleShape, hazeState = hazeState)
                                .nuviaSpecularBorder(CircleShape, 0.75.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    enabled = shellProgress > 0.5f,
                                ) {
                                    if (selectedTab != TAB_SEARCH || isSubpageActive || detail != null) {
                                        dismissAllSubpages()
                                        selectedTab = TAB_SEARCH
                                    }
                                    searchFocusTrigger++
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (selectedTab == TAB_SEARCH && !isSubpageActive && detail == null) NUViAIcons.SearchFilled else NUViAIcons.Search,
                                contentDescription = "Search",
                                tint = if (selectedTab == TAB_SEARCH && !isSubpageActive && detail == null) nuviaColors.primary else nuviaColors.textPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
                },
                onHomeShortcutClicked = {
                    if (selectedTab != TAB_HOME || isSubpageActive || detail != null) {
                        dismissAllSubpages()
                        selectedTab = TAB_HOME
                    }
                    scope.launch {
                        homeListState.animateScrollToItem(0)
                    }
                },
                onSearchShortcutClicked = {
                    if (selectedTab != TAB_SEARCH || isSubpageActive || detail != null) {
                        dismissAllSubpages()
                        selectedTab = TAB_SEARCH
                    }
                    searchFocusTrigger++
                },
                onPlayPause = {
                    controller?.let { if (it.isPlaying) it.pause() else it.play() }
                },
                onNext = { controller?.seekToNextMediaItem() },
                onPrevious = if (player.hasPrevious) { { controller?.seekToPreviousMediaItem() } } else null,
                onExpandMiniPlayer = {
                    activeNowPlayingSong = player.song
                    showNowPlaying = true
                },
            )
        }

        // ---- Now Playing ----
        val activeSong = activeNowPlayingSong ?: player.song
        if (showNowPlaying && activeSong != null) {
            // Once the controller's playerState catches up, clear the fast-path override
            LaunchedEffect(player.song?.videoId) {
                val currentP = player.song
                val activeP = activeNowPlayingSong
                if (currentP != null && activeP != null && currentP.videoId == activeP.videoId) {
                    activeNowPlayingSong = null
                }
            }
            // Whatever started this track knew its title and its artwork, but
            // rarely which album or artist page it belongs to. Fill that in
            // once the player is up, so the credits can be tapped through.
            var links by remember { mutableStateOf<Song?>(null) }
            LaunchedEffect(activeSong.videoId) {
                links = null
                val current = activeSong
                if (current.albumId != null && current.artistId != null) return@LaunchedEffect
                if (songActions != null) linksLoading = true
                links = YtMusicRepository.trackLinks(current.videoId).getOrNull()
                linksLoading = false
            }
            val song = activeSong.let { current ->
                val extra = links?.takeIf { it.videoId == current.videoId }
                    ?: return@let current
                current.copy(
                    artistId = current.artistId ?: extra.artistId,
                    albumId = current.albumId ?: extra.albumId,
                    albumName = current.albumName ?: extra.albumName,
                )
            }
            // The three-dot menu snapshots `song` into songActions when it's
            // opened, so a menu opened before the lookup above resolves would
            // otherwise be stuck without album/artist rows even after the ids
            // come in. Keep it in sync while it's showing this track.
            LaunchedEffect(song) {
                if (songActions?.videoId == song.videoId) songActions = song
            }
            val isPendingOpening = activeNowPlayingSong != null && player.song?.videoId != activeNowPlayingSong?.videoId
            ModalBottomSheet(
                onDismissRequest = {
                    showNowPlaying = false
                    activeNowPlayingSong = null
                },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                // The player fills the screen and paints its own background to
                // the very top, so the sheet's default 28.dp top corners would
                // only cut two notches out of the artwork behind the status bar.
                shape = RectangleShape,
                containerColor = Color.Transparent,
                dragHandle = null,
                contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
            ) {
                NowPlayingScreen(
                    song = song,
                    isPlaying = if (isPendingOpening) true else player.isPlaying,
                    isLoading = if (isPendingOpening) true else player.isLoading,
                    positionMs = if (isPendingOpening) 0L else player.positionMs,
                    positionProvider = playbackPositionProvider,
                    durationMs = if (player.durationMs > 0) player.durationMs else (TrackMatcher.secondsOf(song.durationText)?.times(1000L) ?: 0L),
                    onPlayPause = {
                        controller?.let { if (it.isPlaying) it.pause() else it.play() }
                    },
                    onNext = { controller?.seekToNextMediaItem() },
                    onPrevious = { controller?.seekToPrevious() },
                    onSeekFraction = { fraction ->
                        controller?.let { player ->
                            // Read at the moment of the seek, not from the
                            // polled snapshot the screen draws with: a track
                            // change updates the current item before it updates
                            // the duration, so a fraction dropped seconds after
                            // a transition would otherwise be scaled by the
                            // previous song's length.
                            val duration = player.duration
                            if (duration > 0) {
                                player.seekTo(
                                    (fraction * duration).toLong()
                                        .coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L)),
                                )
                            }
                        }
                    },
                    onSeek = { target ->
                        controller?.let { player ->
                            // Clamped here rather than at each caller because
                            // not every caller can clamp. The scrubber's target
                            // is a fraction of the duration and cannot overrun,
                            // but a tapped lyric line seeks to a timestamp from
                            // whichever transcription matched on title, artist
                            // and duration — and a match against a slightly
                            // longer master puts every line late, so a tap near
                            // the end asks for a position past the end of this
                            // stream. Media3 answers that by clamping to the
                            // final millisecond, which ends the track and starts
                            // the next one: tapping the last line of a song
                            // skipped it.
                            val duration = player.duration
                            player.seekTo(
                                if (duration > 0) {
                                    target.coerceIn(0L, (duration - SEEK_END_GUARD_MS).coerceAtLeast(0L))
                                } else {
                                    target.coerceAtLeast(0L)
                                },
                            )
                        }
                    },
                    queue = player.queue,
                    queueIndex = player.queueIndex,
                    hasPrevious = player.hasPrevious,
                    hasNext = player.hasNext,
                    repeatMode = player.repeatMode,
                    shuffleEnabled = shuffleEnabled,
                    autoplayEnabled = autoplay,
                    signedIn = signedIn,
                    likeStatus = likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT,
                    onToggleLike = { viewModel.toggleLike(song.videoId) },
                    onToggleShuffle = { controller?.let(QueueShuffle::toggle) },
                    onCycleRepeat = {
                        controller?.let {
                            val next = when (it.repeatMode) {
                                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                                else -> Player.REPEAT_MODE_OFF
                            }
                            // Repeat-all loops the queue as it stands; AutoPlay's
                            // tracks are the opposite of that — an endless supply
                            // of new ones — so they come back out first. Native
                            // REPEAT_MODE_ALL then wraps a plain queue exactly as
                            // it should, and the LaunchedEffect above leaves it be
                            // for as long as repeat-all stays on.
                            if (next == Player.REPEAT_MODE_ALL) it.dropAutoplayTracks()
                            it.repeatMode = next
                        }
                    },
                    onToggleAutoplay = {
                        val on = !autoplay
                        AppSettings.setAutoplay(on)
                        if (on) {
                            // Let the effect above seed a mix for the track
                            // playing now, instead of passing over it as one it
                            // has already extended.
                            autoplaySeed = null
                        } else {
                            // Switching it off takes the mix back out of the
                            // queue — what's left is what was actually asked for.
                            controller?.dropAutoplayTracks()
                        }
                    },
                    onJumpTo = { controller?.seekToDefaultPosition(it) },
                    onRemoveFromQueue = { controller?.removeMediaItem(it) },
                    onMoveInQueue = { from, to -> controller?.moveMediaItem(from, to) },
                    // The enriched copy, not player.song — otherwise the menu
                    // hides the album and artist rows even once their browse
                    // ids have been resolved.
                    onOpenMenu = { songActions = song },
                    onOpenAlbum = { id ->
                        activeNowPlayingSong = null
                        showNowPlaying = false
                        viewModel.openDetail(
                            id,
                            song.albumName ?: song.title,
                            song.artist,
                            song.thumbnailUrl,
                            BrowseType.ALBUM,
                        )
                    },
                    onOpenArtist = { id ->
                        activeNowPlayingSong = null
                        showNowPlaying = false
                        // No artwork: this track's cover isn't the artist's
                        // picture, and the page fills its own in once loaded.
                        viewModel.openDetail(id, song.artist, "Artist", null, BrowseType.ARTIST)
                    },
                    lyrics = lyrics,
                    lyricsSource = lyricsSource,
                    lyricsUnavailable = lyricsChecked && lyrics.isNullOrEmpty(),
                    onOpenEqualizer = {
                        showNowPlaying = false
                        navigateToSubpage(SubpageDestination.EQUALIZER)
                    },
                    onOpenAudioOutput = {
                        showAudioOutputSheet = true
                    },
                    onClearQueue = {
                        // Keep what's playing; drop everything queued after it.
                        controller?.let { c ->
                            if (c.mediaItemCount > c.currentMediaItemIndex + 1) {
                                c.removeMediaItems(c.currentMediaItemIndex + 1, c.mediaItemCount)
                            }
                        }
                    },
                )
            }
        }

        // ---- Album / playlist detail ----
        // ---- Long-press track actions ----
        songActions?.let { song ->
            // The player is the only thing that can be on screen while this
            // sheet is up, so it's also what "opened from the player" means.
            val fromPlayer = showNowPlaying
            val share: () -> Unit = {
                Toast.makeText(context, "Sharing is coming soon", Toast.LENGTH_SHORT).show()
                songActions = null
            }
            // Navigating has to take the player down with the sheet, or the
            // page it opens lands behind a still-covering player.
            // The track's cover stands in for an album's, but never for an
            // artist's picture — that page loads its own.
            val openPage: (String, String, String, BrowseType) -> Unit = { id, title, sub, type ->
                songActions = null
                activeNowPlayingSong = null
                showNowPlaying = false
                val art = song.thumbnailUrl.takeUnless { type == BrowseType.ARTIST }
                viewModel.openDetail(id, title, sub, art, type)
            }
            // The library toggle needs tokens only YouTube can mint, and the
            // rating it comes back with is more authoritative than anything
            // the library feed knew — so the menu asks as it opens.
            LaunchedEffect(song.videoId) { viewModel.loadSongMenu(song.videoId) }
            // "Remove from this playlist" is only a sentence on a playlist
            // page the account can actually edit, and only for a row that
            // carries the per-entry id a removal is expressed in.
            val editable = viewModel.editablePlaylist(detail?.browseId)
                ?.takeIf { !fromPlayer && song.setVideoId != null }
            ModalBottomSheet(
                onDismissRequest = { songActions = null },
                // The sheet paints itself in the track's own colours, corners
                // and drag handle included — see SongActionsSheet.
                containerColor = Color.Transparent,
                dragHandle = null,
            ) {
                SongActionsSheet(
                    song = song,
                    signedIn = signedIn,
                    likeStatus = likeStatuses[song.videoId] ?: LikeStatus.INDIFFERENT,
                    onPlayNext = { playNext(song); songActions = null },
                    onAddToQueue = { addToQueue(song); songActions = null },
                    // Stays open: the row it replaces itself with is the
                    // progress, and closing the sheet would hide the only
                    // answer to "did that work?".
                    onDownload = { startDownload(listOf(song)) },
                    // The sheet stays up for a rating: it shows the new state
                    // in place, and people often thumb a song and then queue it.
                    onToggleLike = { viewModel.toggleLike(song.videoId) },
                    onToggleDislike = { viewModel.toggleDislike(song.videoId) },
                    onAddToPlaylist = {
                        songActions = null
                        viewModel.loadPlaylists()
                        playlistTarget = song
                    },
                    onRemoveFromPlaylist = editable?.let {
                        {
                            songActions = null
                            viewModel.removeFromPlaylist(it.browseId, song)
                        }
                    },
                    onOpenAlbum = { id ->
                        openPage(
                            id,
                            song.albumName ?: song.title,
                            song.artist,
                            BrowseType.ALBUM,
                        )
                    },
                    onOpenArtist = { id ->
                        openPage(id, song.artist, "Artist", BrowseType.ARTIST)
                    },
                    // Only the player's copy of a track is ever missing these
                    // and backfilling — a row opened from a list already has
                    // whatever ids it's ever going to have.
                    resolvingLinks = fromPlayer && linksLoading,
                    showSleepTimer = fromPlayer,
                    onShare = share.takeIf { fromPlayer },
                    onCopyLog = if (fromPlayer) {
                        {
                            songActions = null
                            scope.launch {
                                val text = TrackLog.forTrack(song, NerdStats.current.value)
                                clipboard.setText(AnnotatedString(text))
                                // The line count, not just "copied": it is the
                                // one thing the system's own paste confirmation
                                // doesn't say, and an empty log is a real
                                // outcome worth seeing rather than a silent one.
                                Toast.makeText(
                                    context,
                                    "Log copied · ${text.lineSequence().count()} lines",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        }
                    } else {
                        null
                    },
                )
            }
        }

        // ---- Add to playlist / new playlist ----
        // One sheet for both, because they are one decision: the list of
        // playlists with a way to make another. `creatingPlaylist` opens it
        // straight onto the form, which is what the Library tile means.
        if (playlistTarget != null || creatingPlaylist) {
            val target = playlistTarget
            val dismiss = {
                playlistTarget = null
                creatingPlaylist = false
            }
            ModalBottomSheet(
                onDismissRequest = dismiss,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PlaylistPickerSheet(
                    playlists = playlists,
                    loading = playlistsLoading,
                    song = target,
                    startCreating = target == null,
                    onPick = { playlist ->
                        target?.let { viewModel.addToPlaylist(playlist, it) }
                        dismiss()
                    },
                    onCreate = { title, privacy ->
                        viewModel.createPlaylist(title, privacy, target)
                        dismiss()
                    },
                )
            }
        }

        // ---- Playlist rename / delete (long-press on the Library tab) ----
        playlistActions?.let { playlist ->
            ModalBottomSheet(
                onDismissRequest = { playlistActions = null },
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                PlaylistActionsSheet(
                    playlist = playlist,
                    onOpen = {
                        playlistActions = null
                        viewModel.openDetail(
                            browseId = playlist.browseId,
                            title = playlist.title,
                            subtitle = playlist.subtitle,
                            thumbnailUrl = playlist.thumbnailUrl,
                            type = BrowseType.PLAYLIST,
                        )
                    },
                    onRename = { name ->
                        playlistActions = null
                        viewModel.renamePlaylist(playlist, name)
                    },
                    onDelete = {
                        playlistActions = null
                        viewModel.deletePlaylist(playlist)
                    },
                )
            }
        }

        // ---- Browse contextual actions (long-press on shelf card / detail more button) ----
        browseActions?.let { target ->
            val isTargetPinned = target.browseId?.let { id ->
                id in pinnedPlaylists ||
                id.removePrefix("VL") in pinnedPlaylists ||
                "VL$id" in pinnedPlaylists
            } ?: false

            ModalBottomSheet(
                onDismissRequest = { browseActions = null },
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                BrowseActionsSheet(
                    target = target,
                    isPinned = isTargetPinned,
                    onPlay = {
                        browseActions = null
                        withBrowseSongs(target) { play(it, 0) }
                    },
                    onShuffle = {
                        browseActions = null
                        withBrowseSongs(target) {
                            QueueShuffle.enableForNextQueue()
                            play(it, it.indices.random())
                        }
                    },
                    onPlayNext = {
                        browseActions = null
                        withBrowseSongs(target) { songs ->
                            songs.asReversed().forEach { playNext(it) }
                        }
                    },
                    onAddToQueue = {
                        browseActions = null
                        withBrowseSongs(target) { songs ->
                            songs.forEach { addToQueue(it) }
                        }
                    },
                    onOpen = if (target.fromCard && target.browseId != null) {
                        {
                            browseActions = null
                            viewModel.openDetail(
                                browseId = target.browseId,
                                title = target.title,
                                subtitle = target.subtitle,
                                thumbnailUrl = target.thumbnailUrl,
                                type = target.type,
                            )
                        }
                    } else null,
                    onDownloadAll = if (target.browseId?.startsWith("local:") != true && target.type != BrowseType.ARTIST) {
                        {
                            browseActions = null
                            withBrowseSongs(target) { startDownload(it) }
                        }
                    } else null,
                    onTogglePin = if (target.browseId != null && target.type != BrowseType.ARTIST) {
                        {
                            browseActions = null
                            AppSettings.togglePinnedPlaylist(target.browseId)
                        }
                    } else null,
                    onRename = target.playlist?.let { playlist ->
                        { name ->
                            browseActions = null
                            viewModel.renamePlaylist(playlist, name)
                        }
                    },
                    onDelete = target.playlist?.let { playlist ->
                        {
                            browseActions = null
                            viewModel.deletePlaylist(playlist)
                        }
                    },
                )
            }
        }

        if (showLyricsSources) {
            BackHandler { showLyricsSources = false }
            LyricsSourcesDialog(
                hazeState = hazeState,
                onDismiss = { showLyricsSources = false },
            )
        }

        if (showAppLanguage) {
            BackHandler { showAppLanguage = false }
            AppLanguageDialog(
                hazeState = hazeState,
                onDismiss = { showAppLanguage = false },
            )
        }

        if (showListenBrainzLogin) {
            var tokenInput by remember { mutableStateOf(listenBrainzToken) }
            ListenBrainzTokenAlert(
                hazeState = hazeState,
                tokenInput = tokenInput,
                onTokenInputChange = { tokenInput = it },
                onSave = {
                    AppSettings.setListenBrainzToken(tokenInput.trim())
                    showListenBrainzLogin = false
                },
                onDismiss = { showListenBrainzLogin = false },
            )
        }

        if (showLastfmLogin) {
            var usernameInput by remember { mutableStateOf("") }
            var passwordInput by remember { mutableStateOf("") }
            var lastfmError by remember { mutableStateOf<String?>(null) }
            var lastfmLoading by remember { mutableStateOf(false) }
            LastfmLoginAlert(
                hazeState = hazeState,
                usernameInput = usernameInput,
                onUsernameInputChange = { usernameInput = it },
                passwordInput = passwordInput,
                onPasswordInputChange = { passwordInput = it },
                error = lastfmError,
                loading = lastfmLoading,
                onSignIn = {
                    lastfmLoading = true
                    lastfmError = null
                    scope.launch {
                        try {
                            LastFM.initialize(
                                apiKey = LastFM.FALLBACK_COMPAT_API_KEY,
                                secret = LastFM.FALLBACK_COMPAT_SECRET,
                            )
                            LastFM.getMobileSession(usernameInput.trim(), passwordInput)
                                .onSuccess { auth ->
                                    AppSettings.setLastfmSessionKey(auth.session.key)
                                    AppSettings.setLastfmUsername(auth.session.name)
                                    AppSettings.setLastfmEnabled(true)
                                    showLastfmLogin = false
                                }
                                .onFailure { e ->
                                    lastfmError = e.message ?: "Login failed"
                                }
                        } catch (e: Exception) {
                            lastfmError = e.message ?: "Login failed"
                        } finally {
                            lastfmLoading = false
                        }
                    }
                },
                onDismiss = { if (!lastfmLoading) showLastfmLogin = false },
            )
        }

        // ---- Discord sign-in (full screen WebView) ----
        if (showDiscordLogin) {
            BackHandler { showDiscordLogin = false }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { showDiscordLogin = false }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            "Sign in to Discord",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    DiscordLoginScreen(
                        onTokenCaptured = { token ->
                            AppSettings.setDiscordToken(token)
                            showDiscordLogin = false
                        },
                    )
                }
            }
        }

        discordDialog?.let { which ->
            DiscordDialogHost(
                which = which,
                hazeState = hazeState,
                onDismiss = { discordDialog = null },
            )
        }

        if (showDownloadManager) {
            val closeDownloadManager = {
                showDownloadManager = false
                DownloadSession.markSeen()
            }
            BackHandler(onBack = closeDownloadManager)
            ModalBottomSheet(
                onDismissRequest = closeDownloadManager,
                containerColor = nuviaColors.surfaceOled,
                scrimColor = Color.Black.copy(alpha = 0.65f),
            ) {
                DownloadManagerSheet(onDismiss = closeDownloadManager)
            }
        }

        if (showAccountSelector) {
            val authStore = remember { AuthStore(context) }
            BackHandler { showAccountSelector = false }
            AccountProfileSelector(
                accounts = authStore.sessions,
                activeAccountId = authStore.activeAccountId,
                activeProfileId = authStore.activeProfileId,
                onSelect = { session, profile ->
                    authStore.select(session.accountId, profile.profileId)
                    val nextCookie = authStore.cookie
                    if (!nextCookie.isNullOrBlank()) {
                        viewModel.onSignedIn(nextCookie)
                    } else {
                        viewModel.signOut()
                    }
                    showAccountSelector = false
                },
                onAddAccount = {
                    showAccountSelector = false
                    isAddAccountLogin = true
                    showLogin = true
                },
                onRemoveAccount = { session ->
                    val fallback = authStore.removeAccount(session.accountId)
                    if (fallback != null && !fallback.cookie.isNullOrBlank()) {
                        viewModel.onSignedIn(fallback.cookie)
                    } else {
                        viewModel.signOut()
                    }
                },
                onOpenSettings = {
                    showAccountSelector = false
                    navigateToSubpage(SubpageDestination.SETTINGS)
                },
                onDismiss = { showAccountSelector = false },
                hazeState = hazeState,
            )
        }

        if (showAudioOutputSheet) {
            BackHandler { showAudioOutputSheet = false }
            AudioOutputSheet(
                accountName = account?.name,
                onDismiss = { showAudioOutputSheet = false },
                hazeState = hazeState,
            )
        }

        if (showUpdateDialog && updateAvailable != null) {
            val info = updateAvailable!!
            BackHandler { showUpdateDialog = false }
            UpdateAvailableDialog(
                version = info.version,
                notes = info.notes,
                hazeState = hazeState,
                onDismiss = { showUpdateDialog = false },
                onDownload = {
                    scope.launch { AppUpdateChecker.downloadApk(context) }
                },
                onCancelDownload = { AppUpdateChecker.cancelDownload() },
                onInstall = {
                    val state = AppUpdateChecker.download.value
                    if (state is AppUpdateChecker.DownloadState.Ready) {
                        AppUpdateChecker.installApk(context, state.file)
                    }
                },
                onOpenReleasePage = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.releaseUrl)))
                },
            )
        }

        // ---- Google sign-in (full screen WebView) ----
        if (showLogin) {
            BackHandler {
                showLogin = false
                isAddAccountLogin = false
            }
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            showLogin = false
                            isAddAccountLogin = false
                        }) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onBackground,
                            )
                        }
                        Text(
                            if (isAddAccountLogin) "Add Google Account" else "Sign in to YouTube Music",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    YtMusicLoginScreen(
                        isAddAccount = isAddAccountLogin,
                        onCookiesCaptured = { cookie ->
                            viewModel.onSignedIn(cookie)
                            showLogin = false
                            isAddAccountLogin = false
                            selectedTab = TAB_LIBRARY
                        },
                    )
                }
            }
        }
    }

    NUViALaunchOverlay(
            visible = showLaunchOverlay,
            onDismissed = { showLaunchOverlay = false },
        )

        NUViAHomeEntranceOverlay(
            visible = showSurveyEntranceOverlay,
            onDismissed = { showSurveyEntranceOverlay = false },
        )
    }
}
}

private fun tween(durationMillis: Int) =
    androidx.compose.animation.core.tween<Float>(durationMillis)

/** How many tracks a station pulls in at a time. */
private const val RADIO_BATCH = 20

/**
 * How far short of the end a seek is allowed to land.
 *
 * Seeking to the final millisecond is indistinguishable from the track running
 * out, so it starts the next song — which is not what anyone dragging to the end
 * of the bar, or tapping the last line of a lyric, is asking for. A second back
 * from the end plays the outro instead.
 */
private const val SEEK_END_GUARD_MS = 1_000L

/**
 * A YouTube video id to seed a radio station from, for a track that may not
 * have one of its own.
 *
 * Radio, related tracks and the home feed are YouTube's alone — see
 * [SourceKind.YOUTUBE][com.music.nuvia.data.sources.SourceKind.YOUTUBE].
 * A track played from module *search* carries a
 * [SourceRegistry.trackKey] as its media id, which means nothing to
 * Innertube: handing one to [YtMusicRepository.radio] gets an empty mix
 * back, which is why AutoPlay quietly stopped extending the queue after a
 * module search result. Looking the recording up on YouTube by name gives
 * the station something it can actually seed from, and the mix that comes
 * back is YouTube's — those tracks then take the ordinary YouTube path and
 * get substituted individually if a module happens to hold them.
 *
 * Null when the track isn't on YouTube at all, which is a real answer: no
 * station rather than a station for the wrong song.
 */
private suspend fun youtubeSeedFor(song: Song): String? {
    if (SourceRegistry.parseTrackKey(song.videoId) == null) return song.videoId
    val target = TrackMatcher.targetOf(song)
    val query = TrackMatcher.queries(target).firstOrNull() ?: return null
    return YtMusicRepository.search(query, SearchFilter.SONGS)
        .getOrNull()
        ?.filterIsInstance<SearchResult.Track>()
        ?.map { it.song }
        ?.let { TrackMatcher.best(it, target) }
        ?.videoId
}

/**
 * How far a detail page scrolls before its title moves up into the bar.
 *
 * Roughly the height of the sleeve and the credit stacked above the Play pair,
 * so the two titles hand over as the header one leaves rather than sitting on
 * screen together. The bar cross-fades over 220ms, which absorbs the difference
 * between that estimate and a particular page's real header.
 */
private val DETAIL_TITLE_DROP = 320.dp
private val MINI_PLAYER_BOTTOM_BAR_GAP = 12.dp

private const val TAB_HOME = TabNavigation.TAB_HOME
private const val TAB_EXPLORE = TabNavigation.TAB_EXPLORE
private const val TAB_LIBRARY = TabNavigation.TAB_LIBRARY
private const val TAB_SEARCH = TabNavigation.TAB_SEARCH

@Composable
private fun PersistentNavigationShell(
    modifier: Modifier = Modifier,
    isScrollingDown: Boolean,
    reduceAnimation: Boolean,
    song: Song?,
    isPlaying: Boolean,
    isLoading: Boolean,
    hasPrevious: Boolean,
    durationMs: Long,
    playbackPositionProvider: () -> Long,
    tabs: List<BottomTab>,
    selectedTab: Int,
    hazeState: HazeState,
    isSubpageActive: Boolean,
    isDetailActive: Boolean,
    onTabSelected: (Int) -> Unit,
    onHomeShortcutClicked: () -> Unit,
    onSearchShortcutClicked: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: (() -> Unit)?,
    onExpandMiniPlayer: () -> Unit,
) {
    val shellProgress = animateFloatAsState(
        targetValue = if (isScrollingDown && song != null) 1f else 0f,
        animationSpec = if (reduceAnimation) {
            androidx.compose.animation.core.snap()
        } else {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
        },
        label = "navShellProgress",
    )
    val density = LocalDensity.current
    val navBarTravel = with(density) { 110.dp.toPx() }
    val miniPlayerTravel = with(density) { (68.dp + MINI_PLAYER_BOTTOM_BAR_GAP).toPx() }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // 1. Floating Bottom Bar (translates down & fades when compact, remains fixed when song == null)
        FloatingBottomBar(
            tabs = tabs,
            selectedIndex = selectedTab,
            hazeState = hazeState,
            ownBackdrop = false,
            modifier = Modifier.graphicsLayer {
                val progress = if (song != null) shellProgress.value.coerceIn(0f, 1f) else 0f
                translationY = progress * navBarTravel
                alpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
            },
            onTabSelected = { index ->
                if (shellProgress.value.coerceIn(0f, 1f) > 0.5f) return@FloatingBottomBar
                onTabSelected(index)
            },
        )

        // 2. MiniPlayer Row (centered, translates down to bottom, flanked by Home & Search on scroll down)
        if (song != null) {
            val navBarsBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            val isHomeSelected = selectedTab == TAB_HOME && !isSubpageActive && !isDetailActive
            val isSearchSelected = selectedTab == TAB_SEARCH && !isSubpageActive && !isDetailActive
            val nuviaColors = LocalNUViAColors.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = navBarsBottomPadding + 68.dp + MINI_PLAYER_BOTTOM_BAR_GAP)
                    .graphicsLayer {
                        val progress = shellProgress.value.coerceIn(0f, 1f)
                        translationY = progress * miniPlayerTravel
                    }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Home button (emerges on scroll down with Liquid Glass)
                Box(
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val progress = shellProgress.value.coerceIn(0f, 1f)
                            val buttonSizePx = 48.dp.roundToPx()
                            if (progress <= 0.001f) {
                                layout(0, buttonSizePx) {}
                            } else {
                                val targetW = (buttonSizePx * progress).roundToInt().coerceIn(0, buttonSizePx)
                                val placeable = measurable.measure(
                                    Constraints.fixed(buttonSizePx, buttonSizePx)
                                )
                                layout(targetW, buttonSizePx) {
                                    val xOffset = (targetW - buttonSizePx) / 2
                                    placeable.placeRelative(xOffset, 0)
                                }
                            }
                        }
                        .graphicsLayer {
                            val progress = shellProgress.value.coerceIn(0f, 1f)
                            alpha = progress
                            scaleX = 0.6f + 0.4f * progress
                            scaleY = 0.6f + 0.4f * progress
                            clip = true
                        }
                        .clip(CircleShape)
                        .nuviaTactilePress()
                        .nuviaGlass(tier = NUViAGlassTier.Elevated, shape = CircleShape, hazeState = hazeState)
                        .nuviaSpecularBorder(CircleShape, 0.75.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (shellProgress.value.coerceIn(0f, 1f) > 0.5f) {
                                onHomeShortcutClicked()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isHomeSelected) NUViAIcons.HomeFilled else NUViAIcons.Home,
                        contentDescription = "Home",
                        tint = if (isHomeSelected) nuviaColors.primary else nuviaColors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }

                Spacer(
                    modifier = Modifier.layout { _, _ ->
                        val progress = shellProgress.value.coerceIn(0f, 1f)
                        val gapPx = 8.dp.roundToPx()
                        val w = (gapPx * progress).roundToInt().coerceIn(0, gapPx)
                        layout(w, 0) {}
                    }
                )

                // Centered MiniPlayer
                Box(modifier = Modifier.weight(1f)) {
                    MiniPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        hazeState = hazeState,
                        ownBackdrop = false,
                        onPlayPause = onPlayPause,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onExpand = onExpandMiniPlayer,
                        progressProvider = {
                            val pos = playbackPositionProvider()
                            if (durationMs > 0) {
                                (pos.toFloat() / durationMs).coerceIn(0f, 1f)
                            } else 0f
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                Spacer(
                    modifier = Modifier.layout { _, _ ->
                        val progress = shellProgress.value.coerceIn(0f, 1f)
                        val gapPx = 8.dp.roundToPx()
                        val w = (gapPx * progress).roundToInt().coerceIn(0, gapPx)
                        layout(w, 0) {}
                    }
                )

                // Search button (emerges on scroll down with Liquid Glass)
                Box(
                    modifier = Modifier
                        .layout { measurable, _ ->
                            val progress = shellProgress.value.coerceIn(0f, 1f)
                            val buttonSizePx = 48.dp.roundToPx()
                            if (progress <= 0.001f) {
                                layout(0, buttonSizePx) {}
                            } else {
                                val targetW = (buttonSizePx * progress).roundToInt().coerceIn(0, buttonSizePx)
                                val placeable = measurable.measure(
                                    Constraints.fixed(buttonSizePx, buttonSizePx)
                                )
                                layout(targetW, buttonSizePx) {
                                    val xOffset = (targetW - buttonSizePx) / 2
                                    placeable.placeRelative(xOffset, 0)
                                }
                            }
                        }
                        .graphicsLayer {
                            val progress = shellProgress.value.coerceIn(0f, 1f)
                            alpha = progress
                            scaleX = 0.6f + 0.4f * progress
                            scaleY = 0.6f + 0.4f * progress
                            clip = true
                        }
                        .clip(CircleShape)
                        .nuviaTactilePress()
                        .nuviaGlass(tier = NUViAGlassTier.Elevated, shape = CircleShape, hazeState = hazeState)
                        .nuviaSpecularBorder(CircleShape, 0.75.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) {
                            if (shellProgress.value.coerceIn(0f, 1f) > 0.5f) {
                                onSearchShortcutClicked()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSearchSelected) NUViAIcons.SearchFilled else NUViAIcons.Search,
                        contentDescription = "Search",
                        tint = if (isSearchSelected) nuviaColors.primary else nuviaColors.textPrimary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

