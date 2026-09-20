package com.music.nuvia.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.music.nuvia.auth.AuthStore
import com.music.nuvia.data.lyrics.LyricsSource
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.music.nuvia.playback.EqLayout
import com.music.nuvia.playback.EqualizerPreset
import kotlinx.coroutines.flow.MutableStateFlow

enum class EqualizerMode {
    DYNAMIC,
    MANUAL,
}

enum class OutputPcmMode(val label: String) {
    PCM_16("16-bit"),
    FLOAT_32("32-bit float"),
}

/**
 * Stream bitrate ceiling. HIGH means "whatever the best available format is".
 *
 * [hourly] is what the ceiling costs in data over an hour of listening, which
 * is the only part of this a user actually cares about on a metered plan.
 */
enum class AudioQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val hourly: String,
) {
    LOW(64, "Low", "~64 kbps · smallest download", "29 MB/hr"),
    MEDIUM(128, "Medium", "~128 kbps · balanced", "58 MB/hr"),
    HIGH(Int.MAX_VALUE, "High", "Best available · ~171 kbps Opus", "77 MB/hr"),
}

enum class ThemeMode(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

/**
 * What to keep when a track is saved to the device.
 */
enum class DownloadQuality(
    val maxKbps: Int,
    val label: String,
    val detail: String,
    val perTrack: String,
    val keepsLossless: Boolean,
) {
    STANDARD(128, "Standard", "~128 kbps AAC · fits more on the device", "~4 MB", false),
    HIGH(Int.MAX_VALUE, "High", "Best AAC on offer, usually ~256 kbps", "~8 MB", false),
    LOSSLESS(
        Int.MAX_VALUE,
        "Lossless",
        "Bit-exact if a source has it, best AAC if not",
        "~35 MB",
        true,
    ),
}

/**
 * App settings, backed by SharedPreferences and exposed as flows.
 *
 * PlaybackService runs in the same process as the UI, so it observes these
 * same flows and applies changes to the live ExoPlayer instance immediately —
 * no restart, no rebinding.
 */
object AppSettings {

    private lateinit var prefs: SharedPreferences

    /** Only for the Discord token — everything else on here is plain prefs. */
    private lateinit var authStore: AuthStore

    /**
     * Quality ceilings, one per kind of connection — the point of the split is
     * that Wi-Fi can stay on High while mobile data is capped. Both default to
     * High; the mobile plan is the user's to budget, not ours to assume.
     */
    val audioQualityWifi = MutableStateFlow(AudioQuality.HIGH)
    val audioQualityCellular = MutableStateFlow(AudioQuality.HIGH)

    val downloadQuality = MutableStateFlow(DownloadQuality.LOSSLESS)
    val wifiOnlyDownloads = MutableStateFlow(true)

    /** Whether the active network charges for data. `null` while offline. */
    val meteredConnection = MutableStateFlow<Boolean?>(null)

    /** User override to treat all connections as metered to save data. */
    val forceMeteredConnection = MutableStateFlow(false)

    val isMetered: Boolean
        get() = forceMeteredConnection.value || meteredConnection.value == true

    /**
     * Ask sources for the file they hold rather than a transcode of it.
     *
     * Off by default, and honestly labelled in Settings: YouTube has no
     * lossless rendition of anything, so this does nothing at all until a
     * source that holds real files is added on the Sources screen. It also
     * loses to [effectiveAudioQuality] — see
     * [SourceResolver.requestForNow][com.music.nuvia.data.sources.SourceResolver.requestForNow] —
     * because a capped connection is a budget, and a preference should not
     * quietly overspend one.
     */
    val losslessAudio = MutableStateFlow(true)

    val crossfadeSeconds = MutableStateFlow(0)

    /**
     * Lets Automix's analyzer decide the transition's timing and length
     * from each track's tempo, energy and structure, replacing the fixed
     * [crossfadeSeconds] window rather than needing it set to anything first
     * — [crossfadeSeconds] only matters here as a fallback while a pair is
     * still being analysed. Off by default: analysis costs a background
     * decode per track.
     *
     * See [com.music.nuvia.playback.smart.TransitionPlanner].
     */
    val smartFadeEnabled = MutableStateFlow(false)
    val skipSilence = MutableStateFlow(false)

    /**
     * Widens stereo output via [com.music.nuvia.playback.SpatialAudioProcessor],
     * a stereo widening + cross-feed effect running inside ExoPlayer's own
     * pipeline. Not true object-based spatial audio — YouTube only ever hands
     * us a stereo stream, so there's no Atmos-style source to render.
     *
     * The user's wish, not the final answer: it only takes effect on a device
     * with Dolby Atmos switched on, and [com.music.nuvia.playback.DolbyAtmos]
     * clears it back to false the moment that stops being true.
     */
    val spatialAudio = MutableStateFlow(false)
    val equalizerEnabled = MutableStateFlow(false)
    val equalizerMode = MutableStateFlow(EqualizerMode.DYNAMIC)
    val equalizerToneX = MutableStateFlow(0)
    val equalizerToneY = MutableStateFlow(0)
    val equalizerFocused = MutableStateFlow(false)
    val equalizerBalance = MutableStateFlow(0f)
    val equalizerBands = MutableStateFlow(EqualizerPreset.FLAT.bands)
    val equalizerPreset = MutableStateFlow(EqualizerPreset.FLAT)
    val outputPcmMode = MutableStateFlow(OutputPcmMode.PCM_16)
    val playbackSpeed = MutableStateFlow(1.0f)
    val themeMode = MutableStateFlow(ThemeMode.DARK)

    /** App language tag ("" for system default, or "en", "es", "fr", "de", "hi", "ja"). */
    val appLanguage = MutableStateFlow("")

    /** Keep playing similar music once the queue runs out. */
    val autoplay = MutableStateFlow(true)

    /** Put the playing track's codec, bitrate and sample rate on the player. */
    val showNerdStats = MutableStateFlow(false)

    /** Freezes the main player's mesh gradient instead of letting it drift/crossfade. */
    val reduceAnimation = MutableStateFlow(false)

    /** Stop playback when the app is swiped away from the recent apps screen. */
    val stopOnTaskRemoved = MutableStateFlow(false)

    /** Hides the volume slider on the main player, leaving the rest of the layout to reflow. */
    val hideVolumeBar = MutableStateFlow(false)

    /** Swiping a song row plays it next instead of adding it to the end of the queue. */
    val swipeToPlayNext = MutableStateFlow(false)

    /** Drops haze blur (status bar, mini player, bottom fade, lyrics focus) for a solid-fill look. */
    val reduceDynamicBlur = MutableStateFlow(false)

    /** Adapts the UI theme and liquid glass glow to match current album artwork. */
    val dynamicLighting = MutableStateFlow(true)

    /** Master minimal mode toggle: completely strips decorative lighting, glows, and artwork colors for pure monochrome minimalism. */
    val minimalUi = MutableStateFlow(false)

    /** Adapts UI accent colors to the currently playing album artwork. */
    val dynamicArtworkColors = MutableStateFlow(true)

    /** User-selected custom accent color applied when [dynamicArtworkColors] is OFF. */
    val customThemeColor = MutableStateFlow(0xFFE85D04.toInt())

    /** Adds atmospheric aurora glow behind the canvas and player. Off by default for pristine AMOLED black. */
    val ambientLighting = MutableStateFlow(false)

    /** Adds radiant glow illumination around buttons, switches, active chips, and sliders. Off by default for pure minimalism. */
    val uiGlow = MutableStateFlow(false)

    /** Enables translucent frosted glass surfaces, refraction shaders, and specular depth. */
    val glassEffects = MutableStateFlow(true)

    /** Enables real-time backdrop blurring behind Liquid Glass surfaces. Default is ON. */
    val liquidGlassBlur = MutableStateFlow(true)

    /** Effective state: Minimal UI acts as master override for all visual effects. */
    val effectiveMinimalUi: Boolean get() = minimalUi.value
    val effectiveDynamicColors: Boolean get() = !minimalUi.value && dynamicArtworkColors.value
    val effectiveAmbientLighting: Boolean get() = !minimalUi.value && ambientLighting.value
    val effectiveUiGlow: Boolean get() = !minimalUi.value && uiGlow.value
    val effectiveGlassEffects: Boolean get() = !minimalUi.value && glassEffects.value
    val effectiveLiquidGlassBlur: Boolean get() = effectiveGlassEffects && liquidGlassBlur.value

    /**
     * Plays a looping video behind the cover art on the player when one is
     * published for the track — Spotify's Canvas, Apple's motion artwork.
     *
     * Costs a video stream on top of the audio one and reaches three
     * services that have nothing to do with playback, so it stays a switch —
     * but it is the better default, and most tracks resolve to no canvas at
     * all. See [CanvasRepository][com.music.nuvia.data.canvas.CanvasRepository].
     */
    val animatedCanvas = MutableStateFlow(true)
    val spotifySpdcToken = MutableStateFlow("")
    val spotifyToken: MutableStateFlow<String> get() = spotifySpdcToken

    /**
     * Blows the player's cover art out to a full-bleed banner running off the
     * top of the screen, rather than sitting it in a square card.
     *
     * The treatment motion artwork has always had, applied to still sleeves too.
     * Off restores the card: the sleeve keeps its corners, its shadow and its
     * shrink-while-paused, and only a clip goes full-bleed. Phones only either
     * way — see the hero notes in
     * [NowPlayingScreen][com.music.nuvia.ui.player.NowPlayingScreen].
     */
    val fullBleedArtwork = MutableStateFlow(true)

    /**
     * Time-synced lyrics on the player, lit up as they are sung.
     *
     * On by default — it is most of the point of the player screen — but it
     * reaches third-party lyric databases for every track played, so it stays
     * a switch, and [lyricsSources] narrows which of them get asked.
     */
    val syncedLyrics = MutableStateFlow(true)

    /** The databases [syncedLyrics] may ask. Empty is the same as off. */
    val lyricsSources = MutableStateFlow(LyricsSource.entries.toSet())

    /**
     * The order [lyricsSources] are asked in — see [LyricsRepository][com.music.nuvia.data.lyrics.LyricsRepository]:
     * every enabled source is asked at once, but a higher-priority one still
     * pending is never preempted by a lower one that happened to answer first.
     * Reordered from Settings, so this is a full permutation of
     * [LyricsSource.entries] rather than a subset — enabling and ordering are
     * independent choices.
     */
    val lyricsSourceOrder = MutableStateFlow<List<LyricsSource>>(LyricsSource.entries)

    /**
     * Off, the highest-priority source to answer at all is taken as the
     * lyrics, word-synced or not. On, a merely line-synced answer is held as
     * a fallback while the rest of [lyricsSourceOrder] is still checked for a
     * word-synced one — worth the extra network calls to some, not to others,
     * which is why it defaults off rather than being how [LyricsRepository]
     * always behaved.
     */
    val prioritizeSyllableSync = MutableStateFlow(false)

    /** Manual lyrics time offset in milliseconds (-5000ms to +5000ms). Positive values advance lyrics; negative delay them. */
    val lyricsSyncOffsetMs = MutableStateFlow(0)

    /** Disk budget for cached audio. [AudioCache][com.music.nuvia.playback.AudioCache] evicts past it. */
    val audioCacheLimitBytes = MutableStateFlow(DEFAULT_CACHE_LIMIT_BYTES)
    val audioCacheLimitMb = MutableStateFlow((DEFAULT_CACHE_LIMIT_BYTES / (1024 * 1024)).toInt())

    /** Enables saving search queries in local search history. */
    val searchHistoryEnabled = MutableStateFlow(true)

    /** Look up artist genres via Last.fm top tags for the Replay chart. */
    val replayGenres = MutableStateFlow(true)

    /** Filters out ringtones, alarms, notifications, podcasts and short recordings from local music. */
    val filterNonMusicAudio = MutableStateFlow(true)

    /** Folder URI selected by the user for local music files, or empty for all device storage. */
    val localMusicFolderUri = MutableStateFlow("")

    // ── Library ─────────────────────────────────────────────────────────────

    /**
     * Browse ids of the playlists pinned to the top of the Library tab, in the
     * order they were pinned.
     */
    val pinnedPlaylists = MutableStateFlow<List<String>>(emptyList())

    /** How many playlists [pinnedPlaylists] can hold at once. */
    const val MAX_PINNED_PLAYLISTS = 5

    // ── Scrobbling ──────────────────────────────────────────────────────

    /**
     * Whether the scrobbling integrations are offered at all.
     *
     * Off for now: Last.fm and ListenBrainz are shelved until a later version,
     * and this is the one switch that shelves them — the settings rows dim
     * and the submit paths in
     * [PlaybackService][com.music.nuvia.playback.PlaybackService] go quiet.
     * Without the second half of that, a device that had Last.fm connected
     * before would keep scrobbling behind a screen saying the feature is gone.
     *
     * Nothing here clears the stored keys or toggles, so an account that was
     * connected comes back exactly as it was.
     *
     * A plain `val` rather than a `const val` on purpose: a const would be
     * folded away and every gate below would compile to a "condition is always
     * false" warning.
     */
    val scrobblingAvailable = true

    val lastfmEnabled = MutableStateFlow(false)
    val lastfmUsername = MutableStateFlow("")
    val lastfmSessionKey = MutableStateFlow("")
    val lastfmApiKey = MutableStateFlow("")
    val lastfmSecret = MutableStateFlow("")
    val lastfmEndpoint = MutableStateFlow("")
    val lastfmScrobbleEnabled = MutableStateFlow(false)
    val lastfmNowPlaying = MutableStateFlow(false)
    val scrobbleMinDuration = MutableStateFlow(30)
    val scrobbleDelayPercent = MutableStateFlow(0.5f)
    val scrobbleDelaySeconds = MutableStateFlow(180)
    val listenBrainzEnabled = MutableStateFlow(false)
    val listenBrainzToken = MutableStateFlow("")

    // ── Discord Rich Presence ───────────────────────────────────────────

    /**
     * The connected Discord account's token, mirrored out of [AuthStore] so
     * [PlaybackService][com.music.nuvia.playback.PlaybackService] can pick
     * up a login without polling for one. Empty means not connected.
     *
     * Only the mirror is here — the persisted copy is encrypted, because unlike
     * a scrobbler key this one is the account itself.
     */
    val discordToken = MutableStateFlow("")

    /**
     * Who the token belongs to, cached at login. Kept so the settings screen
     * can show the account without a round trip every time it opens, and can
     * still show it offline.
     */
    val discordUsername = MutableStateFlow("")
    val discordName = MutableStateFlow("")
    val discordAvatar = MutableStateFlow("")

    val discordRpcEnabled = MutableStateFlow(true)

    /** Put the track title on the bold profile line, in place of the artist. */
    val discordUseDetails = MutableStateFlow(false)

    /** Reveals the presence-shape controls: status, activity type/name, buttons. */
    val discordAdvancedMode = MutableStateFlow(false)

    val discordStatus = MutableStateFlow("online")
    val discordActivityType = MutableStateFlow("listening")

    /** Overrides the "Listening to ___" line; empty means the app's own name. */
    val discordActivityName = MutableStateFlow("")

    val discordButton1Text = MutableStateFlow("")
    val discordButton1Visible = MutableStateFlow(true)
    val discordButton2Text = MutableStateFlow("")
    val discordButton2Visible = MutableStateFlow(true)

    /** The notice about what connecting an account actually does has been read. */
    val onboardingCompleted = MutableStateFlow(false)
    val preferredGenres = MutableStateFlow<Set<String>>(emptySet())
    val preferredMoods = MutableStateFlow<Set<String>>(emptySet())
    val preferredLanguages = MutableStateFlow<Set<String>>(emptySet())
    val preferredMoments = MutableStateFlow<Set<String>>(emptySet())
    val preferredDiscovery = MutableStateFlow<Set<String>>(emptySet())
    val preferredContent = MutableStateFlow<Set<String>>(emptySet())
    val discordInfoDismissed = MutableStateFlow(false)

    /** Published by PlaybackService so the UI can open the system equalizer. */
    val audioSessionId = MutableStateFlow(0)

    /**
     * True only while a Automix transition that is actually *mixing* is
     * audible — one that beat-matched, cued the incoming track into its
     * arrangement, or rode a filter.
     *
     * Deliberately not "a crossfade is running". The fallback case, where
     * neither track was analysed in time and the incoming one starts from 0:00
     * under a plain equal-power fade, is exactly what this must stay dark for:
     * the whole point is that seeing it means the analysis landed and did
     * something a plain crossfade could not.
     */
    val smartMixInProgress = MutableStateFlow(false)

    /**
     * How much of the *upcoming* transition has been analysed, for stats for
     * nerds. Published by the crossfade controller, which is the only thing
     * that knows which two tracks the next transition is between.
     */
    val smartAnalysis = MutableStateFlow(SmartAnalysis())

    /**
     * Where on the *playing* track the next transition is planned to happen, as
     * fractions of its duration, or null when there is nothing worth drawing.
     *
     * Only published once both tracks are measured. Before that the planner is
     * still working from a fallback window that moves as evidence arrives, and
     * a marker that slides around the bar would be worse than no marker.
     */
    val smartTransitionWindow = MutableStateFlow<TransitionWindow?>(null)

    /** The ceiling that applies to a stream started right now. */
    val effectiveAudioQuality: AudioQuality
        get() = if (isMetered) {
            audioQualityCellular.value
        } else {
            audioQualityWifi.value
        }

    /**
     * Whether a download may start on the connection in hand.
     */
    val downloadsAllowedNow: Boolean
        get() = !wifiOnlyDownloads.value || !isMetered

    fun init(context: Context) {
        prefs = context.getSharedPreferences("nuvia_settings", Context.MODE_PRIVATE)
        migrateSingleQuality()
        audioQualityWifi.value = readQuality(KEY_QUALITY_WIFI)
        audioQualityCellular.value = readQuality(KEY_QUALITY_CELLULAR)
        migrateDownloadQuality()
        downloadQuality.value = readDownloadQuality()
        wifiOnlyDownloads.value = prefs.getBoolean(KEY_WIFI_ONLY_DOWNLOADS, true)
        losslessAudio.value = prefs.getBoolean(KEY_LOSSLESS, true)
        crossfadeSeconds.value = prefs.getInt(KEY_CROSSFADE, 0)
        smartFadeEnabled.value = prefs.getBoolean(KEY_SMART_FADE, false)
        skipSilence.value = prefs.getBoolean(KEY_SKIP_SILENCE, false)
        spatialAudio.value = prefs.getBoolean(KEY_SPATIAL_AUDIO, false)
        equalizerEnabled.value = prefs.getBoolean(KEY_EQ_ENABLED, false)
        equalizerMode.value = runCatching {
            EqualizerMode.valueOf(prefs.getString(KEY_EQ_MODE, null) ?: EqualizerMode.DYNAMIC.name)
        }.getOrDefault(EqualizerMode.DYNAMIC)
        equalizerToneX.value = prefs.getInt(KEY_EQ_TONE_X, 0).coerceIn(-EqLayout.TONE_STEPS, EqLayout.TONE_STEPS)
        equalizerToneY.value = prefs.getInt(KEY_EQ_TONE_Y, 0).coerceIn(-EqLayout.TONE_STEPS, EqLayout.TONE_STEPS)
        equalizerFocused.value = prefs.getBoolean(KEY_EQ_FOCUSED, false)
        equalizerBalance.value = prefs.getFloat(KEY_EQ_BALANCE, 0f).coerceIn(-1f, 1f)
        equalizerBands.value = readEqualizerBands()
        equalizerPreset.value = EqualizerPreset.matching(equalizerBands.value)
        playbackSpeed.value = prefs.getFloat(KEY_SPEED, 1.0f)
        themeMode.value = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "DARK")
        }.getOrDefault(ThemeMode.DARK)
        appLanguage.value = prefs.getString(KEY_APP_LANGUAGE, "").orEmpty()
        applyAppLanguage(appLanguage.value)
        autoplay.value = prefs.getBoolean(KEY_AUTOPLAY, true)
        showNerdStats.value = prefs.getBoolean(KEY_NERD_STATS, false)
        reduceAnimation.value = prefs.getBoolean(KEY_REDUCE_ANIMATION, false)
        stopOnTaskRemoved.value = prefs.getBoolean(KEY_STOP_ON_TASK_REMOVED, false)
        hideVolumeBar.value = prefs.getBoolean(KEY_HIDE_VOLUME_BAR, false)
        swipeToPlayNext.value = prefs.getBoolean(KEY_SWIPE_TO_PLAY_NEXT, false)
        reduceDynamicBlur.value = prefs.getBoolean(KEY_REDUCE_BLUR, false)
        dynamicLighting.value = prefs.getBoolean(KEY_DYNAMIC_LIGHTING, true)
        minimalUi.value = prefs.getBoolean(KEY_MINIMAL_UI, false)
        dynamicArtworkColors.value = prefs.getBoolean(KEY_DYNAMIC_ARTWORK_COLORS, prefs.getBoolean(KEY_DYNAMIC_LIGHTING, true))
        customThemeColor.value = prefs.getInt(KEY_CUSTOM_THEME_COLOR, 0xFFE85D04.toInt())
        ambientLighting.value = prefs.getBoolean(KEY_AMBIENT_LIGHTING, prefs.getBoolean(KEY_DYNAMIC_LIGHTING, false))
        uiGlow.value = prefs.getBoolean(KEY_UI_GLOW, false)
        glassEffects.value = prefs.getBoolean(KEY_GLASS_EFFECTS, !prefs.getBoolean(KEY_REDUCE_BLUR, false))
        liquidGlassBlur.value = prefs.getBoolean(KEY_LIQUID_GLASS_BLUR, true)
        animatedCanvas.value = prefs.getBoolean(KEY_ANIMATED_CANVAS, true)
        spotifySpdcToken.value = prefs.getString(KEY_SPOTIFY_SPDC_TOKEN, "").orEmpty()
        outputPcmMode.value = runCatching {
            OutputPcmMode.valueOf(prefs.getString(KEY_OUTPUT_PCM_MODE, null) ?: OutputPcmMode.PCM_16.name)
        }.getOrDefault(OutputPcmMode.PCM_16)
        fullBleedArtwork.value = prefs.getBoolean(KEY_FULL_BLEED_ARTWORK, true)
        syncedLyrics.value = prefs.getBoolean(KEY_SYNCED_LYRICS, true)
        lyricsSources.value = readLyricsSources()
        lyricsSourceOrder.value = readLyricsSourceOrder()
        prioritizeSyllableSync.value = prefs.getBoolean(KEY_PRIORITIZE_SYLLABLE_SYNC, false)
        lyricsSyncOffsetMs.value = prefs.getInt(KEY_LYRICS_SYNC_OFFSET, 0)
        audioCacheLimitBytes.value = prefs.getLong(KEY_CACHE_LIMIT, DEFAULT_CACHE_LIMIT_BYTES)
            .coerceIn(DEFAULT_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        audioCacheLimitMb.value = (audioCacheLimitBytes.value / (1024L * 1024L)).toInt()
        searchHistoryEnabled.value = prefs.getBoolean(KEY_SEARCH_HISTORY_ENABLED, true)
        forceMeteredConnection.value = prefs.getBoolean(KEY_FORCE_METERED, false)
        replayGenres.value = prefs.getBoolean(KEY_REPLAY_GENRES, true)
        filterNonMusicAudio.value = prefs.getBoolean(KEY_FILTER_NON_MUSIC_AUDIO, true)
        localMusicFolderUri.value = prefs.getString(KEY_LOCAL_MUSIC_FOLDER_URI, "").orEmpty()
        lastfmEnabled.value = prefs.getBoolean(KEY_LASTFM_ENABLED, false)
        lastfmUsername.value = prefs.getString(KEY_LASTFM_USERNAME, "").orEmpty()
        lastfmSessionKey.value = prefs.getString(KEY_LASTFM_SESSION_KEY, "").orEmpty()
        lastfmApiKey.value = prefs.getString(KEY_LASTFM_API_KEY, "").orEmpty()
        lastfmSecret.value = prefs.getString(KEY_LASTFM_SECRET, "").orEmpty()
        lastfmEndpoint.value = prefs.getString(KEY_LASTFM_ENDPOINT, "").orEmpty()
        lastfmScrobbleEnabled.value = prefs.getBoolean(KEY_LASTFM_SCROBBLE_ENABLED, false)
        lastfmNowPlaying.value = prefs.getBoolean(KEY_LASTFM_NOW_PLAYING, false)
        scrobbleMinDuration.value = prefs.getInt(KEY_SCROBBLE_MIN_DURATION, 30)
        scrobbleDelayPercent.value = prefs.getFloat(KEY_SCROBBLE_DELAY_PERCENT, 0.5f)
        scrobbleDelaySeconds.value = prefs.getInt(KEY_SCROBBLE_DELAY_SECONDS, 180)
        listenBrainzEnabled.value = prefs.getBoolean(KEY_LISTENBRAINZ_ENABLED, false)
        listenBrainzToken.value = prefs.getString(KEY_LISTENBRAINZ_TOKEN, "").orEmpty()
        authStore = AuthStore(context)
        discordToken.value = authStore.discordToken.orEmpty()
        discordUsername.value = prefs.getString(KEY_DISCORD_USERNAME, "").orEmpty()
        discordName.value = prefs.getString(KEY_DISCORD_NAME, "").orEmpty()
        discordAvatar.value = prefs.getString(KEY_DISCORD_AVATAR, "").orEmpty()
        discordRpcEnabled.value = prefs.getBoolean(KEY_DISCORD_RPC_ENABLED, true)
        discordUseDetails.value = prefs.getBoolean(KEY_DISCORD_USE_DETAILS, false)
        discordAdvancedMode.value = prefs.getBoolean(KEY_DISCORD_ADVANCED_MODE, false)
        discordStatus.value = prefs.getString(KEY_DISCORD_STATUS, "online").orEmpty()
        discordActivityType.value = prefs.getString(KEY_DISCORD_ACTIVITY_TYPE, "listening").orEmpty()
        discordActivityName.value = prefs.getString(KEY_DISCORD_ACTIVITY_NAME, "").orEmpty()
        discordButton1Text.value = prefs.getString(KEY_DISCORD_BUTTON_1_TEXT, "").orEmpty()
        discordButton1Visible.value = prefs.getBoolean(KEY_DISCORD_BUTTON_1_VISIBLE, true)
        discordButton2Text.value = prefs.getString(KEY_DISCORD_BUTTON_2_TEXT, "").orEmpty()
        discordButton2Visible.value = prefs.getBoolean(KEY_DISCORD_BUTTON_2_VISIBLE, true)
        discordInfoDismissed.value = prefs.getBoolean(KEY_DISCORD_INFO_DISMISSED, false)
        onboardingCompleted.value = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        preferredGenres.value = prefs.getStringSet(KEY_PREFERRED_GENRES, emptySet()) ?: emptySet()
        preferredMoods.value = prefs.getStringSet(KEY_PREFERRED_MOODS, emptySet()) ?: emptySet()
        preferredLanguages.value = prefs.getStringSet(KEY_PREFERRED_LANGUAGES, emptySet()) ?: emptySet()
        preferredMoments.value = prefs.getStringSet(KEY_PREFERRED_MOMENTS, emptySet()) ?: emptySet()
        preferredDiscovery.value = prefs.getStringSet(KEY_PREFERRED_DISCOVERY, emptySet()) ?: emptySet()
        preferredContent.value = prefs.getStringSet(KEY_PREFERRED_CONTENT, emptySet()) ?: emptySet()
        pinnedPlaylists.value = readPinnedPlaylists()
        watchConnection(context)
    }

    /**
     * True the first time this is called after [currentVersionCode] rises above
     * whatever was last recorded — i.e. once per update, on the first launch
     * after it installs. A fresh install has nothing to compare against, so
     * the very first call seeds the stored value from [currentVersionCode]
     * rather than reporting an update.
     *
     * NUViA ships sideloaded (see [com.music.nuvia.data.AppUpdateChecker]),
     * so installing a new APK over the old one is the only "update" there is —
     * app data, this pref included, survives it exactly like a Play Store
     * update. Call once per process start, before anything reads a cache that
     * an update should invalidate.
     */
    fun consumeVersionUpdate(currentVersionCode: Int): Boolean {
        val last = prefs.getInt(KEY_LAST_VERSION_CODE, currentVersionCode)
        if (last != currentVersionCode) {
            prefs.edit().putInt(KEY_LAST_VERSION_CODE, currentVersionCode).apply()
        }
        return currentVersionCode > last
    }

    /**
     * A ceiling saved when there was only one applies to both connections.
     * Someone who picked Low to protect a data plan would not thank us for
     * quietly putting Wi-Fi *and* mobile back on High.
     */
    private fun migrateSingleQuality() {
        val legacy = prefs.getString(KEY_QUALITY_LEGACY, null) ?: return
        prefs.edit()
            .putString(KEY_QUALITY_WIFI, legacy)
            .putString(KEY_QUALITY_CELLULAR, legacy)
            .remove(KEY_QUALITY_LEGACY)
            .apply()
    }

    private fun readQuality(key: String): AudioQuality {
        val stored = prefs.getString(key, null) ?: return AudioQuality.HIGH
        return runCatching { AudioQuality.valueOf(stored) }.getOrDefault(AudioQuality.HIGH)
    }

    private fun migrateDownloadQuality() {
        if (prefs.contains(KEY_QUALITY_DOWNLOAD)) return
        prefs.edit().putString(KEY_QUALITY_DOWNLOAD, DownloadQuality.LOSSLESS.name).apply()
    }

    private fun readDownloadQuality(): DownloadQuality {
        val stored = prefs.getString(KEY_QUALITY_DOWNLOAD, null) ?: return DownloadQuality.LOSSLESS
        return runCatching { DownloadQuality.valueOf(stored) }.getOrDefault(DownloadQuality.LOSSLESS)
    }

    /**
     * Track the active network so [effectiveAudioQuality] can answer without
     * touching ConnectivityManager. Stream resolution happens off the main
     * thread mid-playback; a callback keeps that lookup off the hot path and
     * lets the settings page show which ceiling is currently in force.
     */
    private fun watchConnection(context: Context) {
        val manager = context.getSystemService(ConnectivityManager::class.java) ?: return
        val refresh = {
            meteredConnection.value = runCatching {
                if (manager.activeNetwork == null) null else manager.isActiveNetworkMetered
            }.getOrNull()
        }
        refresh()
        runCatching {
            manager.registerDefaultNetworkCallback(
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) = refresh()
                    override fun onLost(network: Network) = refresh()
                    override fun onCapabilitiesChanged(
                        network: Network,
                        capabilities: NetworkCapabilities,
                    ) = refresh()
                },
            )
        }
    }

    fun setAutoplay(value: Boolean) {
        autoplay.value = value
        prefs.edit().putBoolean(KEY_AUTOPLAY, value).apply()
    }

    fun setAudioQualityWifi(value: AudioQuality) {
        audioQualityWifi.value = value
        prefs.edit().putString(KEY_QUALITY_WIFI, value.name).apply()
    }

    fun setAudioQualityCellular(value: AudioQuality) {
        audioQualityCellular.value = value
        prefs.edit().putString(KEY_QUALITY_CELLULAR, value.name).apply()
    }

    fun setDownloadQuality(value: DownloadQuality) {
        downloadQuality.value = value
        prefs.edit().putString(KEY_QUALITY_DOWNLOAD, value.name).apply()
    }

    fun setWifiOnlyDownloads(value: Boolean) {
        wifiOnlyDownloads.value = value
        prefs.edit().putBoolean(KEY_WIFI_ONLY_DOWNLOADS, value).apply()
    }

    fun setLosslessAudio(value: Boolean) {
        losslessAudio.value = value
        prefs.edit().putBoolean(KEY_LOSSLESS, value).apply()
    }

    fun setCrossfadeSeconds(value: Int) {
        crossfadeSeconds.value = value
        prefs.edit().putInt(KEY_CROSSFADE, value).apply()
    }

    fun setSmartFadeEnabled(value: Boolean) {
        smartFadeEnabled.value = value
        prefs.edit().putBoolean(KEY_SMART_FADE, value).apply()
    }

    fun setSkipSilence(value: Boolean) {
        skipSilence.value = value
        prefs.edit().putBoolean(KEY_SKIP_SILENCE, value).apply()
    }

    fun setSpatialAudio(value: Boolean) {
        spatialAudio.value = value
        prefs.edit().putBoolean(KEY_SPATIAL_AUDIO, value).apply()
    }

    fun setEqualizerEnabled(value: Boolean) {
        equalizerEnabled.value = value
        prefs.edit().putBoolean(KEY_EQ_ENABLED, value).apply()
    }

    fun setEqualizerMode(value: EqualizerMode) {
        equalizerMode.value = value
        prefs.edit().putString(KEY_EQ_MODE, value.name).apply()
    }

    fun setEqualizerTone(x: Int, y: Int) {
        val steps = EqLayout.TONE_STEPS
        val clampedX = x.coerceIn(-steps, steps)
        val clampedY = y.coerceIn(-steps, steps)
        equalizerToneX.value = clampedX
        equalizerToneY.value = clampedY
        prefs.edit().putInt(KEY_EQ_TONE_X, clampedX).putInt(KEY_EQ_TONE_Y, clampedY).apply()
    }

    fun setEqualizerFocused(value: Boolean) {
        equalizerFocused.value = value
        prefs.edit().putBoolean(KEY_EQ_FOCUSED, value).apply()
    }

    fun setEqualizerBalance(value: Float) {
        val clamped = value.coerceIn(-1f, 1f)
        equalizerBalance.value = clamped
        prefs.edit().putFloat(KEY_EQ_BALANCE, clamped).apply()
    }

    fun setEqualizerBands(values: List<Float>) {
        val clamped = List(EqLayout.MANUAL_COUNT) {
            values.getOrElse(it) { 0f }.coerceIn(-EqLayout.MANUAL_RANGE_DB, EqLayout.MANUAL_RANGE_DB)
        }
        equalizerBands.value = clamped
        equalizerPreset.value = EqualizerPreset.matching(clamped)
        prefs.edit().putString(KEY_EQ_BANDS, clamped.joinToString(",")).apply()
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return
        setEqualizerBands(preset.bands)
    }

    private fun readEqualizerBands(): List<Float> {
        val raw = prefs.getString(KEY_EQ_BANDS, null) ?: return EqualizerPreset.FLAT.bands
        val stored = raw.split(",").mapNotNull { it.toFloatOrNull() }
        if (stored.size != EqLayout.MANUAL_COUNT) return EqualizerPreset.FLAT.bands
        return List(EqLayout.MANUAL_COUNT) {
            stored.getOrElse(it) { 0f }.coerceIn(-EqLayout.MANUAL_RANGE_DB, EqLayout.MANUAL_RANGE_DB)
        }
    }

    fun setPlaybackSpeed(value: Float) {
        playbackSpeed.value = value
        prefs.edit().putFloat(KEY_SPEED, value).apply()
    }

    fun setShowNerdStats(value: Boolean) {
        showNerdStats.value = value
        prefs.edit().putBoolean(KEY_NERD_STATS, value).apply()
    }

    fun setThemeMode(value: ThemeMode) {
        themeMode.value = value
        prefs.edit().putString(KEY_THEME, value.name).apply()
    }

    fun setAppLanguage(tag: String) {
        appLanguage.value = tag
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_APP_LANGUAGE, tag).apply()
        }
        applyAppLanguage(tag)
    }

    fun applyAppLanguage(tag: String) {
        runCatching {
            val locales = if (tag.isEmpty() || tag.equals("system", ignoreCase = true)) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun setReduceAnimation(value: Boolean) {
        reduceAnimation.value = value
        prefs.edit().putBoolean(KEY_REDUCE_ANIMATION, value).apply()
    }

    fun setStopOnTaskRemoved(value: Boolean) {
        stopOnTaskRemoved.value = value
        prefs.edit().putBoolean(KEY_STOP_ON_TASK_REMOVED, value).apply()
    }

    fun setHideVolumeBar(value: Boolean) {
        hideVolumeBar.value = value
        prefs.edit().putBoolean(KEY_HIDE_VOLUME_BAR, value).apply()
    }

    fun setSwipeToPlayNext(value: Boolean) {
        swipeToPlayNext.value = value
        prefs.edit().putBoolean(KEY_SWIPE_TO_PLAY_NEXT, value).apply()
    }

    fun setReduceDynamicBlur(value: Boolean) {
        reduceDynamicBlur.value = value
        prefs.edit().putBoolean(KEY_REDUCE_BLUR, value).apply()
    }

    fun setDynamicLighting(value: Boolean) {
        dynamicLighting.value = value
        prefs.edit().putBoolean(KEY_DYNAMIC_LIGHTING, value).apply()
    }

    fun setMinimalUi(value: Boolean) {
        minimalUi.value = value
        prefs.edit().putBoolean(KEY_MINIMAL_UI, value).apply()
    }

    fun setDynamicArtworkColors(value: Boolean) {
        dynamicArtworkColors.value = value
        prefs.edit().putBoolean(KEY_DYNAMIC_ARTWORK_COLORS, value).apply()
    }

    fun setCustomThemeColor(value: Int) {
        customThemeColor.value = value
        prefs.edit().putInt(KEY_CUSTOM_THEME_COLOR, value).apply()
    }

    fun setAmbientLighting(value: Boolean) {
        ambientLighting.value = value
        prefs.edit().putBoolean(KEY_AMBIENT_LIGHTING, value).apply()
    }

    fun setUiGlow(value: Boolean) {
        uiGlow.value = value
        prefs.edit().putBoolean(KEY_UI_GLOW, value).apply()
    }

    fun setGlassEffects(value: Boolean) {
        glassEffects.value = value
        prefs.edit().putBoolean(KEY_GLASS_EFFECTS, value).apply()
    }

    fun setLiquidGlass(value: Boolean) = setGlassEffects(value)

    fun setLiquidGlassBlur(value: Boolean) {
        if (!glassEffects.value) return
        liquidGlassBlur.value = value
        prefs.edit().putBoolean(KEY_LIQUID_GLASS_BLUR, value).apply()
    }

    fun setSyncedLyrics(value: Boolean) {
        syncedLyrics.value = value
        prefs.edit().putBoolean(KEY_SYNCED_LYRICS, value).apply()
    }

    fun setLyricsSources(value: Set<LyricsSource>) {
        lyricsSources.value = value
        prefs.edit().putString(KEY_LYRICS_SOURCES, value.joinToString(",") { it.name }).apply()
    }

    fun setLyricsSourceOrder(value: List<LyricsSource>) {
        lyricsSourceOrder.value = value
        prefs.edit().putString(KEY_LYRICS_SOURCE_ORDER, value.joinToString(",") { it.name }).apply()
    }

    /**
     * Stored as a joined list of names rather than a string set: a name that
     * no longer exists — a source dropped in a later build — has to fall out
     * quietly, and the default when nothing has been saved is "all of them",
     * which a missing key and an empty set would otherwise be unable to tell
     * apart.
     */
    private fun readLyricsSources(): Set<LyricsSource> {
        val stored = prefs.getString(KEY_LYRICS_SOURCES, null)
            ?: return LyricsSource.entries.toSet()
        return stored.split(",")
            .mapNotNull { name -> LyricsSource.entries.firstOrNull { it.name == name } }
            .toSet()
    }

    /**
     * A named source dropped from the stored order — an upgrade reordered
     * since it was saved — falls out on read; one added since is appended, in
     * [LyricsSource]'s own declared order, so a fresh install and an upgraded
     * one agree on where a new source lands until the user says otherwise.
     */
    private fun readLyricsSourceOrder(): List<LyricsSource> {
        val stored = prefs.getString(KEY_LYRICS_SOURCE_ORDER, null)
            ?: return LyricsSource.entries
        val saved = stored.split(",")
            .mapNotNull { name -> LyricsSource.entries.firstOrNull { it.name == name } }
        return saved + LyricsSource.entries.filter { it !in saved }
    }

    fun setPrioritizeSyllableSync(value: Boolean) {
        prioritizeSyllableSync.value = value
        prefs.edit().putBoolean(KEY_PRIORITIZE_SYLLABLE_SYNC, value).apply()
    }

    /**
     * Puts the source list, its order and [prioritizeSyllableSync] back the
     * way a fresh install finds them. [syncedLyrics] itself is left alone —
     * this is "start over on *which* lyrics", not "turn lyrics off".
     */
    fun resetLyricsSourceSettings() {
        setLyricsSources(LyricsSource.entries.toSet())
        setLyricsSourceOrder(LyricsSource.entries)
        setPrioritizeSyllableSync(false)
    }

    fun setLyricsSyncOffsetMs(value: Int) {
        lyricsSyncOffsetMs.value = value
        prefs.edit().putInt(KEY_LYRICS_SYNC_OFFSET, value).apply()
    }

    fun setAnimatedCanvas(value: Boolean) {
        animatedCanvas.value = value
        prefs.edit().putBoolean(KEY_ANIMATED_CANVAS, value).apply()
    }

    fun setSpotifySpdcToken(value: String) {
        spotifySpdcToken.value = value
        prefs.edit().putString(KEY_SPOTIFY_SPDC_TOKEN, value).apply()
    }

    fun setOutputPcmMode(value: OutputPcmMode) {
        outputPcmMode.value = value
        prefs.edit().putString(KEY_OUTPUT_PCM_MODE, value.name).apply()
    }

    fun setFullBleedArtwork(value: Boolean) {
        fullBleedArtwork.value = value
        prefs.edit().putBoolean(KEY_FULL_BLEED_ARTWORK, value).apply()
    }

    /** Clamped to [DEFAULT_CACHE_LIMIT_BYTES]..[MAX_CACHE_LIMIT_BYTES] — the floor is the default, not zero. */
    fun setAudioCacheLimitBytes(value: Long) {
        val clamped = value.coerceIn(DEFAULT_CACHE_LIMIT_BYTES, MAX_CACHE_LIMIT_BYTES)
        audioCacheLimitBytes.value = clamped
        audioCacheLimitMb.value = (clamped / (1024L * 1024L)).toInt()
        prefs.edit().putLong(KEY_CACHE_LIMIT, clamped).apply()
    }

    fun setAudioCacheLimitMb(mb: Int) {
        setAudioCacheLimitBytes(mb.toLong() * 1024L * 1024L)
    }

    fun setMeteredConnection(value: Boolean) {
        forceMeteredConnection.value = value
        prefs.edit().putBoolean(KEY_FORCE_METERED, value).apply()
    }

    fun setSearchHistoryEnabled(value: Boolean) {
        searchHistoryEnabled.value = value
        prefs.edit().putBoolean(KEY_SEARCH_HISTORY_ENABLED, value).apply()
    }

    fun setReplayGenres(value: Boolean) {
        replayGenres.value = value
        prefs.edit().putBoolean(KEY_REPLAY_GENRES, value).apply()
    }

    fun setFilterNonMusicAudio(value: Boolean) {
        filterNonMusicAudio.value = value
        prefs.edit().putBoolean(KEY_FILTER_NON_MUSIC_AUDIO, value).apply()
    }

    fun setLocalMusicFolderUri(value: String) {
        localMusicFolderUri.value = value
        prefs.edit().putString(KEY_LOCAL_MUSIC_FOLDER_URI, value).apply()
    }

    fun resetToDefaults() {
        prefs.edit().clear().apply()
        audioQualityWifi.value = AudioQuality.HIGH
        audioQualityCellular.value = AudioQuality.HIGH
        downloadQuality.value = DownloadQuality.LOSSLESS
        wifiOnlyDownloads.value = true
        losslessAudio.value = true
        crossfadeSeconds.value = 0
        smartFadeEnabled.value = false
        skipSilence.value = false
        spatialAudio.value = false
        playbackSpeed.value = 1.0f
        showNerdStats.value = false
        themeMode.value = ThemeMode.DARK
        minimalUi.value = false
        dynamicArtworkColors.value = true
        customThemeColor.value = 0xFFE85D04.toInt()
        ambientLighting.value = false
        uiGlow.value = false
        glassEffects.value = true
        liquidGlassBlur.value = true
        reduceAnimation.value = false
        reduceDynamicBlur.value = false
        swipeToPlayNext.value = false
        stopOnTaskRemoved.value = false
        autoplay.value = true
        animatedCanvas.value = true
        fullBleedArtwork.value = true
        syncedLyrics.value = true
        lyricsSyncOffsetMs.value = 0
        prioritizeSyllableSync.value = false
        audioCacheLimitBytes.value = DEFAULT_CACHE_LIMIT_BYTES
        audioCacheLimitMb.value = (DEFAULT_CACHE_LIMIT_BYTES / (1024 * 1024)).toInt()
        searchHistoryEnabled.value = true
        forceMeteredConnection.value = false
        replayGenres.value = true
        filterNonMusicAudio.value = true
        localMusicFolderUri.value = ""
        outputPcmMode.value = OutputPcmMode.PCM_16
    }

    fun setLastfmEnabled(value: Boolean) {
        lastfmEnabled.value = value
        prefs.edit().putBoolean(KEY_LASTFM_ENABLED, value).apply()
    }

    fun setLastfmUsername(value: String) {
        lastfmUsername.value = value
        prefs.edit().putString(KEY_LASTFM_USERNAME, value).apply()
    }

    fun setLastfmSessionKey(value: String) {
        lastfmSessionKey.value = value
        prefs.edit().putString(KEY_LASTFM_SESSION_KEY, value).apply()
    }

    fun setLastfmApiKey(value: String) {
        lastfmApiKey.value = value
        prefs.edit().putString(KEY_LASTFM_API_KEY, value).apply()
    }

    fun setLastfmSecret(value: String) {
        lastfmSecret.value = value
        prefs.edit().putString(KEY_LASTFM_SECRET, value).apply()
    }

    fun setLastfmEndpoint(value: String) {
        lastfmEndpoint.value = value
        prefs.edit().putString(KEY_LASTFM_ENDPOINT, value).apply()
    }

    fun setLastfmScrobbleEnabled(value: Boolean) {
        lastfmScrobbleEnabled.value = value
        prefs.edit().putBoolean(KEY_LASTFM_SCROBBLE_ENABLED, value).apply()
    }

    fun setLastfmNowPlaying(value: Boolean) {
        lastfmNowPlaying.value = value
        prefs.edit().putBoolean(KEY_LASTFM_NOW_PLAYING, value).apply()
    }

    fun setScrobbleMinDuration(value: Int) {
        scrobbleMinDuration.value = value
        prefs.edit().putInt(KEY_SCROBBLE_MIN_DURATION, value).apply()
    }

    fun setScrobbleDelayPercent(value: Float) {
        scrobbleDelayPercent.value = value
        prefs.edit().putFloat(KEY_SCROBBLE_DELAY_PERCENT, value).apply()
    }

    fun setScrobbleDelaySeconds(value: Int) {
        scrobbleDelaySeconds.value = value
        prefs.edit().putInt(KEY_SCROBBLE_DELAY_SECONDS, value).apply()
    }

    fun setListenBrainzEnabled(value: Boolean) {
        listenBrainzEnabled.value = value
        prefs.edit().putBoolean(KEY_LISTENBRAINZ_ENABLED, value).apply()
    }

    fun setListenBrainzToken(value: String) {
        listenBrainzToken.value = value
        prefs.edit().putString(KEY_LISTENBRAINZ_TOKEN, value).apply()
    }

    /** Writes through to the encrypted store; pass "" to disconnect. */
    fun setDiscordToken(value: String) {
        discordToken.value = value
        authStore.discordToken = value.ifEmpty { null }
    }

    fun setDiscordAccount(username: String, name: String, avatar: String?) {
        discordUsername.value = username
        discordName.value = name
        discordAvatar.value = avatar.orEmpty()
        prefs.edit()
            .putString(KEY_DISCORD_USERNAME, username)
            .putString(KEY_DISCORD_NAME, name)
            .putString(KEY_DISCORD_AVATAR, avatar.orEmpty())
            .apply()
    }

    fun setDiscordRpcEnabled(value: Boolean) {
        discordRpcEnabled.value = value
        prefs.edit().putBoolean(KEY_DISCORD_RPC_ENABLED, value).apply()
    }

    fun setDiscordUseDetails(value: Boolean) {
        discordUseDetails.value = value
        prefs.edit().putBoolean(KEY_DISCORD_USE_DETAILS, value).apply()
    }

    fun setDiscordAdvancedMode(value: Boolean) {
        discordAdvancedMode.value = value
        prefs.edit().putBoolean(KEY_DISCORD_ADVANCED_MODE, value).apply()
    }

    fun setDiscordStatus(value: String) {
        discordStatus.value = value
        prefs.edit().putString(KEY_DISCORD_STATUS, value).apply()
    }

    fun setDiscordActivityType(value: String) {
        discordActivityType.value = value
        prefs.edit().putString(KEY_DISCORD_ACTIVITY_TYPE, value).apply()
    }

    fun setDiscordActivityName(value: String) {
        discordActivityName.value = value
        prefs.edit().putString(KEY_DISCORD_ACTIVITY_NAME, value).apply()
    }

    fun setOnboardingCompleted(value: Boolean) {
        onboardingCompleted.value = value
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()
    }

    fun setPreferredMusicPreferences(
        genres: Set<String>,
        moods: Set<String>,
        languages: Set<String> = emptySet(),
        moments: Set<String> = emptySet(),
        discovery: Set<String> = emptySet(),
        content: Set<String> = emptySet(),
    ) {
        preferredGenres.value = genres
        preferredMoods.value = moods
        preferredLanguages.value = languages
        preferredMoments.value = moments
        preferredDiscovery.value = discovery
        preferredContent.value = content
        prefs.edit()
            .putStringSet(KEY_PREFERRED_GENRES, genres)
            .putStringSet(KEY_PREFERRED_MOODS, moods)
            .putStringSet(KEY_PREFERRED_LANGUAGES, languages)
            .putStringSet(KEY_PREFERRED_MOMENTS, moments)
            .putStringSet(KEY_PREFERRED_DISCOVERY, discovery)
            .putStringSet(KEY_PREFERRED_CONTENT, content)
            .apply()
    }

    fun setDiscordButton1Text(value: String) {
        discordButton1Text.value = value
        prefs.edit().putString(KEY_DISCORD_BUTTON_1_TEXT, value).apply()
    }

    fun setDiscordButton1Visible(value: Boolean) {
        discordButton1Visible.value = value
        prefs.edit().putBoolean(KEY_DISCORD_BUTTON_1_VISIBLE, value).apply()
    }

    fun setDiscordButton2Text(value: String) {
        discordButton2Text.value = value
        prefs.edit().putString(KEY_DISCORD_BUTTON_2_TEXT, value).apply()
    }

    fun setDiscordButton2Visible(value: Boolean) {
        discordButton2Visible.value = value
        prefs.edit().putBoolean(KEY_DISCORD_BUTTON_2_VISIBLE, value).apply()
    }

    fun setDiscordInfoDismissed(value: Boolean) {
        discordInfoDismissed.value = value
        prefs.edit().putBoolean(KEY_DISCORD_INFO_DISMISSED, value).apply()
    }

    /**
     * Pins or unpins [browseId], returning whether it is pinned afterwards.
     *
     * Pinning past [MAX_PINNED_PLAYLISTS] is refused rather than evicting the
     * oldest pin. Unpinning always succeeds.
     */
    fun togglePinnedPlaylist(browseId: String): Boolean {
        val current = pinnedPlaylists.value
        val isCurrentlyPinned = browseId in current || browseId.removePrefix("VL") in current || "VL$browseId" in current
        val updated = if (isCurrentlyPinned) {
            val normalized = listOf(browseId, browseId.removePrefix("VL"), "VL$browseId")
            current.filterNot { it in normalized }
        } else {
            if (current.size >= MAX_PINNED_PLAYLISTS) return false
            current + browseId
        }
        pinnedPlaylists.value = updated
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PINNED_PLAYLISTS, updated.joinToString(",")).apply()
        }
        return !isCurrentlyPinned
    }

    /**
     * Unpins [browseId] if it is currently pinned.
     */
    fun unpinPlaylist(browseId: String) {
        val current = pinnedPlaylists.value
        val normalized = listOf(browseId, browseId.removePrefix("VL"), "VL$browseId")
        val updated = current.filterNot { it in normalized }
        if (updated.size != current.size) {
            pinnedPlaylists.value = updated
            if (::prefs.isInitialized) {
                prefs.edit().putString(KEY_PINNED_PLAYLISTS, updated.joinToString(",")).apply()
            }
        }
    }

    /**
     * Sets the pinned playlists directly (e.g. during backup restore),
     * sanitizing and respecting [MAX_PINNED_PLAYLISTS].
     */
    fun setPinnedPlaylists(list: List<String>) {
        val sanitized = list.filter { it.isNotBlank() }.distinct().take(MAX_PINNED_PLAYLISTS)
        pinnedPlaylists.value = sanitized
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_PINNED_PLAYLISTS, sanitized.joinToString(",")).apply()
        }
    }

    private fun readPinnedPlaylists(): List<String> {
        val stored = prefs.getString(KEY_PINNED_PLAYLISTS, null) ?: return emptyList()
        return stored.split(",").filter { it.isNotBlank() }
    }

    /** Forgets the account: token and cached profile. */
    fun clearDiscordAccount() {
        setDiscordToken("")
        setDiscordAccount("", "", null)
    }

    const val DEFAULT_CACHE_LIMIT_BYTES = 512L * 1024 * 1024
    const val MAX_CACHE_LIMIT_BYTES = 10L * 1024 * 1024 * 1024

    private const val KEY_QUALITY_LEGACY = "audio_quality"
    private const val KEY_QUALITY_WIFI = "audio_quality_wifi"
    private const val KEY_QUALITY_CELLULAR = "audio_quality_cellular"
    private const val KEY_QUALITY_DOWNLOAD = "download_quality"
    private const val KEY_WIFI_ONLY_DOWNLOADS = "wifi_only_downloads"
    private const val KEY_LOSSLESS = "lossless_audio"
    private const val KEY_CROSSFADE = "crossfade_seconds"
    private const val KEY_SMART_FADE = "smart_fade_enabled"
    private const val KEY_SKIP_SILENCE = "skip_silence"
    private const val KEY_SPATIAL_AUDIO = "spatial_audio"
    private const val KEY_EQ_ENABLED = "equalizer_enabled"
    private const val KEY_EQ_MODE = "equalizer_mode"
    private const val KEY_EQ_TONE_X = "equalizer_tone_x"
    private const val KEY_EQ_TONE_Y = "equalizer_tone_y"
    private const val KEY_EQ_FOCUSED = "equalizer_focused"
    private const val KEY_EQ_BALANCE = "equalizer_balance"
    private const val KEY_EQ_BANDS = "equalizer_bands"
    private const val KEY_SPEED = "playback_speed"
    private const val KEY_THEME = "theme_mode"
    const val KEY_APP_LANGUAGE = "app_language"
    private const val KEY_AUTOPLAY = "autoplay"
    private const val KEY_NERD_STATS = "show_nerd_stats"
    private const val KEY_CACHE_LIMIT = "audio_cache_limit_bytes"
    private const val KEY_REDUCE_ANIMATION = "reduce_animation"
    private const val KEY_STOP_ON_TASK_REMOVED = "stop_on_task_removed"
    private const val KEY_HIDE_VOLUME_BAR = "hide_volume_bar"
    private const val KEY_SWIPE_TO_PLAY_NEXT = "swipe_to_play_next"
    private const val KEY_REDUCE_BLUR = "reduce_dynamic_blur"
    private const val KEY_DYNAMIC_LIGHTING = "dynamic_lighting"
    private const val KEY_MINIMAL_UI = "pref_minimal_ui"
    private const val KEY_DYNAMIC_ARTWORK_COLORS = "pref_dynamic_artwork_colors"
    private const val KEY_CUSTOM_THEME_COLOR = "custom_theme_color"
    private const val KEY_AMBIENT_LIGHTING = "pref_ambient_lighting"
    private const val KEY_UI_GLOW = "pref_ui_glow"
    private const val KEY_GLASS_EFFECTS = "pref_glass_effects"
    private const val KEY_LIQUID_GLASS_BLUR = "pref_liquid_glass_blur"
    private const val KEY_ANIMATED_CANVAS = "animated_canvas"
    private const val KEY_SPOTIFY_SPDC_TOKEN = "spotify_spdc_token"
    private const val KEY_OUTPUT_PCM_MODE = "output_pcm_mode"
    private const val KEY_FULL_BLEED_ARTWORK = "full_bleed_artwork"
    private const val KEY_SYNCED_LYRICS = "synced_lyrics"
    private const val KEY_LYRICS_SOURCES = "lyrics_sources"
    private const val KEY_LYRICS_SOURCE_ORDER = "lyrics_source_order"
    private const val KEY_PRIORITIZE_SYLLABLE_SYNC = "prioritize_syllable_sync"
    private const val KEY_LYRICS_SYNC_OFFSET = "lyrics_sync_offset_ms"
    private const val KEY_REPLAY_GENRES = "replay_genres"
    private const val KEY_FILTER_NON_MUSIC_AUDIO = "filter_non_music_audio"
    private const val KEY_LOCAL_MUSIC_FOLDER_URI = "local_music_folder_uri"
    private const val KEY_FORCE_METERED = "force_metered_connection"
    private const val KEY_SEARCH_HISTORY_ENABLED = "search_history_enabled"

    private const val KEY_LASTFM_ENABLED = "lastfm_enabled"
    private const val KEY_LASTFM_USERNAME = "lastfm_username"
    private const val KEY_LASTFM_SESSION_KEY = "lastfm_session_key"
    private const val KEY_LASTFM_API_KEY = "lastfm_api_key"
    private const val KEY_LASTFM_SECRET = "lastfm_secret"
    private const val KEY_LASTFM_ENDPOINT = "lastfm_endpoint"
    private const val KEY_LASTFM_SCROBBLE_ENABLED = "lastfm_scrobble_enabled"
    private const val KEY_LASTFM_NOW_PLAYING = "lastfm_now_playing"
    private const val KEY_SCROBBLE_MIN_DURATION = "scrobble_min_duration"
    private const val KEY_SCROBBLE_DELAY_PERCENT = "scrobble_delay_percent"
    private const val KEY_SCROBBLE_DELAY_SECONDS = "scrobble_delay_seconds"
    private const val KEY_LISTENBRAINZ_ENABLED = "listenbrainz_enabled"
    private const val KEY_LISTENBRAINZ_TOKEN = "listenbrainz_token"

    private const val KEY_DISCORD_USERNAME = "discord_username"
    private const val KEY_DISCORD_NAME = "discord_name"
    private const val KEY_DISCORD_AVATAR = "discord_avatar"
    private const val KEY_DISCORD_RPC_ENABLED = "discord_rpc_enabled"
    private const val KEY_DISCORD_USE_DETAILS = "discord_use_details"
    private const val KEY_DISCORD_ADVANCED_MODE = "discord_advanced_mode"
    private const val KEY_DISCORD_STATUS = "discord_status"
    private const val KEY_DISCORD_ACTIVITY_TYPE = "discord_activity_type"
    private const val KEY_DISCORD_ACTIVITY_NAME = "discord_activity_name"
    private const val KEY_DISCORD_BUTTON_1_TEXT = "discord_button_1_text"
    private const val KEY_DISCORD_BUTTON_1_VISIBLE = "discord_button_1_visible"
    private const val KEY_DISCORD_BUTTON_2_TEXT = "discord_button_2_text"
    private const val KEY_DISCORD_BUTTON_2_VISIBLE = "discord_button_2_visible"
    private const val KEY_DISCORD_INFO_DISMISSED = "discord_info_dismissed"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    private const val KEY_PREFERRED_GENRES = "preferred_genres"
    private const val KEY_PREFERRED_MOODS = "preferred_moods"
    private const val KEY_PREFERRED_LANGUAGES = "preferred_languages"
    private const val KEY_PREFERRED_MOMENTS = "preferred_moments"
    private const val KEY_PREFERRED_DISCOVERY = "preferred_discovery"
    private const val KEY_PREFERRED_CONTENT = "preferred_content"
    private const val KEY_PINNED_PLAYLISTS = "pinned_playlists"
    private const val KEY_LAST_VERSION_CODE = "last_version_code"
}

/**
 * Where one track stands in Automix's analysis.
 *
 * The three no-result states are kept apart because they call for different
 * reactions: [WAITING] resolves itself once bytes arrive, [ANALYSING] resolves
 * itself in a few seconds, and [FAILED] never resolves at all. From outside
 * they look identical, which is precisely why the line has to say which.
 */
enum class TrackAnalysisState {
    /** Nothing in flight and no result — usually waiting on bytes to arrive. */
    WAITING,

    /** Decode and inference running now; a result is a few seconds away. */
    ANALYSING,

    /** Measured, with a tempo the planner can actually use. */
    ANALYSED,

    /**
     * Measured off the track's opening, with the whole-track pass running now to
     * replace those numbers with better ones.
     *
     * Its own state rather than either neighbour, because it is genuinely both:
     * reporting [ANALYSING] made a track that was already usable look like it
     * had gone backwards, and reporting [ANALYSED] would hide that the cue and
     * the tempo are about to move.
     */
    REFINING,

    /**
     * Tried and came back with nothing usable — a decode error, or audio that
     * yielded no tempo. Distinct from [WAITING] because nothing further will
     * happen on its own: waiting is a matter of time, this is not.
     */
    FAILED,
}

/**
 * Both sides of the next transition, for stats for nerds.
 *
 * A transition needs *both* tracks measured before it can beat-match or cue the
 * incoming one into its arrangement, so reporting them separately is what makes
 * a plain crossfade explicable rather than mysterious.
 */
data class SmartAnalysis(
    val current: TrackAnalysisState = TrackAnalysisState.WAITING,
    val next: TrackAnalysisState = TrackAnalysisState.WAITING,
)

/**
 * A span of the playing track, in fractions of its duration, that the next
 * transition is planned to occupy.
 */
data class TransitionWindow(val start: Float, val end: Float)
