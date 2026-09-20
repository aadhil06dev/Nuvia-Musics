package com.music.nuvia.data

import com.music.nuvia.data.DebugLog as Log
import com.music.nuvia.data.innertube.Innertube
import com.music.nuvia.data.innertube.InnertubeParser
import com.music.nuvia.data.model.Account
import com.music.nuvia.data.model.ArtistPage
import com.music.nuvia.data.model.HomeFeed
import com.music.nuvia.data.model.HomeShelf
import com.music.nuvia.data.model.LibraryPage
import com.music.nuvia.data.model.LibraryState
import com.music.nuvia.data.model.LikeStatus
import com.music.nuvia.data.model.PlaylistPrivacy
import com.music.nuvia.data.model.SearchFilter
import com.music.nuvia.data.model.SearchResult
import com.music.nuvia.data.model.ShelfItem
import com.music.nuvia.data.model.Song
import com.music.nuvia.data.model.SongMenu
import com.music.nuvia.data.model.UserPlaylist
import com.music.nuvia.data.sources.TrackMatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/** Suspend API over Innertube. Every call returns a Result so the UI can show a real error. */
object YtMusicRepository {

    private const val TAG = "NUViA"

    /**
     * The personalised feed, led by what was actually just played and padded
     * out with new releases.
     *
     * FEmusic_home alone is thin when signed out (three shelves), so extra
     * rows are pulled from FEmusic_new_releases, which carries genuinely
     * different content. Charts (Daily/Weekly, Trending) live under Explore
     * in the real app — see [explore] — not here. Titles are de-duped in
     * case the home feed already surfaced the same shelf.
     *
     * FEmusic_home's own continuation token comes back, for [moreHome] —
     * signed in, it keeps paging into mood mixes and more personalised
     * shelves the same way the official app does as you scroll; signed out
     * it's empty and there's nothing more to fetch.
     */
    suspend fun home(): Result<HomeFeed> = call("home") {
        coroutineScope {
            val recent = async { runCatching { recentlyPlayed() }.getOrNull() }
            val homeRaw = async { Innertube.browse("FEmusic_home") }
            val newReleases = async { runCatching { shelvesOf("FEmusic_new_releases") }.getOrDefault(emptyList()) }
            val home = homeRaw.await()
            val shelves = listOfNotNull(recent.await()) +
                InnertubeParser.parseHome(home) +
                newReleases.await()
            HomeFeed(shelves, InnertubeParser.continuationToken(home))
        }
    }

    /**
     * More Home shelves past [home]'s first page, following FEmusic_home's
     * own continuation — the lever the official app pulls as you scroll
     * rather than a fixed one-shot page. "Recently played" and
     * FEmusic_new_releases are one-shot and don't participate.
     */
    suspend fun moreHome(token: String): Result<HomeFeed> = call("home:more") {
        val response = Innertube.browseContinuation(token)
        HomeFeed(
            shelves = InnertubeParser.parseHomeContinuation(response),
            continuation = InnertubeParser.continuationToken(response),
        )
    }

    /**
     * The lead shelf: the account's listening history, newest first.
     *
     * YouTube's home already carries a "Listen again", but it ranks by how
     * *often* something has been played rather than how recently — so it keeps
     * leading with last month's favourites for days after a change of mood,
     * which reads as the feed being broken. The history feed reflects a play
     * the moment it's registered, so it's what the top of the page is built
     * from. YouTube's own shelf stays below, where its ranking is a feature.
     *
     * Signed-in only; there is no history to read as a guest.
     */
    private suspend fun recentlyPlayed(): HomeShelf? {
        if (Innertube.cookie == null) return null
        val songs = InnertubeParser.collectSongsDeep(Innertube.browse(HISTORY))
            // A track played three times today is three rows in the feed.
            .distinctBy { it.videoId }
            .take(RECENT_LIMIT)
        if (songs.isEmpty()) return null
        return HomeShelf(
            title = RECENT_TITLE,
            items = songs.map {
                ShelfItem(
                    title = it.title,
                    subtitle = it.artist,
                    thumbnailUrl = it.thumbnailUrl,
                    videoId = it.videoId,
                    browseId = null,
                )
            },
        )
    }

    private suspend fun fetchHistory(): List<Song> =
        InnertubeParser.collectSongsDeep(Innertube.browse(HISTORY)).distinctBy { it.videoId }

    /**
     * The account's listening history, in the order YouTube Music keeps it.
     */
    suspend fun history(): Result<List<Song>> = call("history") {
        if (Innertube.cookie != null) {
            val remote = runCatching { fetchHistory() }.getOrDefault(emptyList())
            if (remote.isNotEmpty()) return@call remote
        }
        // Fallback to local on-device listening history if offline or signed out
        com.music.nuvia.data.history.LocalHistoryStore.history.value
    }

    /** Account listening history for the Recents feed. */
    suspend fun recents(): Result<List<Song>> = call("recents") {
        if (Innertube.cookie != null) {
            val history = runCatching { fetchHistory() }.getOrDefault(emptyList())
            if (history.isNotEmpty()) return@call history
        }
        com.music.nuvia.data.history.LocalHistoryStore.history.value
    }

    private const val HISTORY = "FEmusic_history"
    private const val RECENT_TITLE = "Recently played"

    /** Enough to scroll through, short of turning the shelf into the history page. */
    private const val RECENT_LIMIT = 20

    private suspend fun shelvesOf(browseId: String): List<HomeShelf> =
        InnertubeParser.parseHome(Innertube.browse(browseId))

    /**
     * Explore: charts & trending, new releases, and curated discovery feeds from YouTube Music.
     * Merged and deduplicated to deliver a discovery experience.
     */
    suspend fun explore(): Result<List<HomeShelf>> = call("explore") {
        coroutineScope {
            val feedIds = listOf(
                "FEmusic_explore",
                "FEmusic_charts",
                "FEmusic_new_releases_albums",
            )
            val feeds = feedIds.map { id ->
                async {
                    runCatching { shelvesOf(id) }.getOrDefault(emptyList())
                }
            }.awaitAll()

            val seen = mutableSetOf<String>()
            val combined = feeds.flatten().filter { shelf ->
                val titleLower = shelf.title.lowercase().trim()
                val isMoodOrGenre = titleLower.contains("mood") || titleLower.contains("genre")
                shelf.items.isNotEmpty() &&
                    !isMoodOrGenre &&
                    seen.add(titleLower)
            }.map { shelf ->
                val cleanItems = shelf.items.filterNot { item ->
                    item.browseId?.startsWith("FEmusic_moods_and_genres") == true
                }
                shelf.copy(items = cleanItems)
            }.filter { it.items.isNotEmpty() }

            // If charts was empty or minimal, fallback to home feed discovery sections
            if (combined.size < 3) {
                val homeShelves = runCatching { home().getOrNull()?.shelves.orEmpty() }.getOrDefault(emptyList())
                val additional = homeShelves.filter { shelf ->
                    val titleLower = shelf.title.lowercase().trim()
                    val isMoodOrGenre = titleLower.contains("mood") || titleLower.contains("genre")
                    shelf.items.isNotEmpty() &&
                        !isMoodOrGenre &&
                        seen.add(titleLower)
                }
                combined + additional
            } else {
                combined
            }
        }
    }

    suspend fun search(query: String, filter: SearchFilter): Result<List<SearchResult>> =
        call("search:${filter.name}") {
            InnertubeParser.parseSearch(Innertube.search(query, filter.params))
        }

    /**
     * What YouTube Music would suggest completing [input] to, for the search
     * field's typeahead. Unfiltered on purpose: a suggestion is a query, and
     * which tab it is then run against is the user's to pick afterwards.
     */
    suspend fun searchSuggestions(input: String): Result<List<String>> =
        call("suggest") {
            InnertubeParser.parseSearchSuggestions(Innertube.searchSuggestions(input))
        }

    /**
     * The catalogue (audio-only) release of a music-video upload, found the
     * same way the "Switch to audio" toggle in the real app would land on
     * it: searching the title and artist and taking the closest song match.
     * Called before a video-tagged [Song] ever reaches the queue, so
     * playback, the mini player/notification, and YouTube's own history all
     * see the audio track — never the video upload's title, art or id.
     *
     * Matched through [TrackMatcher] rather than a bare title compare, for
     * the same reason [SourceResolver][com.music.nuvia.data.sources.SourceResolver]
     * does: a query for a niche title can come back with nothing that is
     * really the recording, and taking the first row regardless was landing
     * on a same-language, wrong-song hit — a Telugu folk video resolving to
     * an unrelated devotional track was reported from exactly this path.
     * [TrackMatcher.best] returning null is a normal answer, not a failure to
     * work around.
     *
     * Returns [song] unchanged when it isn't a video, or when nothing better
     * turns up — playing the video's own audio track beats guessing at a
     * substitute, and [song] is what a queue restore or offline retry falls
     * back to as well.
     *
     * [search] already drops video rows from its results (see
     * [InnertubeParser.parseSearch]), so every candidate here is audio-only
     * without a second check.
     */
    private val resolvedAudioCache = android.util.LruCache<String, Song>(500)

    /** Returns the cached catalogue match for [song] if already resolved, or [song] itself if not a video. */
    fun getCachedResolvedAudio(song: Song): Song? {
        if (!song.isVideo) return song
        return resolvedAudioCache.get(song.videoId)
    }

    suspend fun resolveAudio(song: Song): Song {
        if (!song.isVideo) return song
        resolvedAudioCache.get(song.videoId)?.let { return it }
        val target = TrackMatcher.targetOf(song)
        for (query in TrackMatcher.queries(target)) {
            val candidates = search(query, SearchFilter.SONGS)
                .getOrNull()
                ?.filterIsInstance<SearchResult.Track>()
                ?.map { it.song }
                .orEmpty()
            TrackMatcher.best(candidates, target)?.let { matched ->
                resolvedAudioCache.put(song.videoId, matched)
                return matched
            }
        }
        resolvedAudioCache.put(song.videoId, song)
        return song
    }

    /** Signed-in profile for the settings header. Null when signed out. */
    suspend fun account(): Result<Account> = call("account") {
        InnertubeParser.parseAccount(Innertube.accountMenu())
            ?: error("No account details")
    }

    /**
     * The whole library in one shot — requires a signed-in session.
     *
     * YouTube Music has no single "my library" feed: Liked Music is the `LM`
     * auto-playlist, the songs added to the library are a separate feed, and
     * every saved collection has its own browse id. They're fetched in
     * parallel and a feed that fails or is simply empty (a fresh account has
     * no saved albums) is dropped rather than failing the whole page.
     */
    suspend fun library(): Result<LibraryPage> = call("library") {
        coroutineScope {
            val liked = async { runCatching { songsPaged(LIKED_MUSIC) }.getOrDefault(emptyList()) }
            val added = async { runCatching { songsPaged(LIBRARY_SONGS) }.getOrDefault(emptyList()) }
            val shelves = LIBRARY_FEEDS
                .map { (title, browseId) ->
                    async {
                        val items = runCatching {
                            InnertubeParser.parseLibraryItems(Innertube.browse(browseId))
                        }.getOrDefault(emptyList())
                        HomeShelf(title, items)
                    }
                }
                .awaitAll()
                .filter { it.items.isNotEmpty() }

            val likedSongs = liked.await()
            val likedIds = likedSongs.mapTo(HashSet()) { it.videoId }
            LibraryPage(
                likedSongs = likedSongs,
                // Thumbs-up'd tracks are also in the library feed; only what
                // the "Liked Music" list doesn't already cover is worth a
                // second section.
                librarySongs = added.await().filterNot { it.videoId in likedIds },
                shelves = shelves,
            )
        }
    }

    /**
     * What YouTube Music would play on after [videoId]. Feeds AutoPlay; the
     * seed track itself comes back first, so callers filter what they have.
     */
    suspend fun radio(videoId: String): Result<List<Song>> = call("radio:$videoId") {
        InnertubeParser.parseWatchQueue(Innertube.next(videoId))
    }

    /**
     * The artist and album pages a track links out to.
     *
     * Search rows carry them, but home cards and anything already sitting in a
     * queue often don't — and the credits in the player have to lead somewhere
     * either way. A track's own watch queue entry always names both.
     */
    suspend fun trackLinks(videoId: String): Result<Song> = call("links:$videoId") {
        InnertubeParser.parseWatchQueue(Innertube.next(videoId))
            .firstOrNull { it.videoId == videoId }
            ?: error("no watch entry for $videoId")
    }

    /**
     * One page of a browse feed's tracks, and the token for the page after
     * it — null once there is nothing more. [suggested] is only ever
     * non-empty for a playlist page — see [InnertubeParser.parsePlaylistShelf].
     */
    data class SongPage(
        val songs: List<Song>,
        val continuation: String?,
        val suggested: List<Song> = emptyList(),
        /**
         * Whether the release this page describes is in the library. Only the
         * first page can answer — a continuation carries rows and nothing else
         * — so it is null from [moreSongs] and must not overwrite what
         * [browseSongs] already established.
         */
        val library: LibraryState? = null,
    )

    /**
     * The first page of an album/playlist's tracks, and nothing more.
     *
     * Deliberately not the whole list. Following every continuation before
     * returning meant a long playlist spent up to ten round trips showing a
     * spinner, when every row needed to fill the first screenful was in the
     * first response. The rest arrives behind a page that is by then already
     * being read — see [moreSongs].
     */
    suspend fun browseSongs(browseId: String): Result<SongPage> = call("browse:$browseId") {
        pageOf(Innertube.browse(browseId))
    }

    /** The page [SongPage.continuation] points at. */
    suspend fun moreSongs(token: String): Result<SongPage> = call("browse:more") {
        pageOf(Innertube.browseContinuation(token))
    }

    private fun pageOf(response: JsonObject): SongPage {
        val library = InnertubeParser.parseLibraryState(response)
        // A playlist page is scoped to its own shelf so its "Suggested
        // tracks" never read as songs the user added — see
        // parsePlaylistShelf. Anything else (album, library, history) has no
        // such shelf, and falls back to the layout-agnostic walk.
        InnertubeParser.parsePlaylistShelf(response)?.let { shelf ->
            return SongPage(shelf.songs, shelf.continuation, shelf.suggested, library)
        }
        return SongPage(
            // One response can name the same track twice — an album page that
            // also carries a "you might also like" shelf, say. Collecting into a
            // map used to take care of that; paging by hand means saying so.
            songs = InnertubeParser.collectSongsDeep(response).distinctBy { it.videoId },
            continuation = InnertubeParser.continuationToken(response),
            library = library,
        )
    }

    suspend fun allSongs(browseId: String): Result<List<Song>> = call("all:$browseId") {
        songsPaged(browseId).ifEmpty { error("No tracks here") }
    }

    /**
     * Every track behind a browse id, following continuations.
     *
     * A playlist page returns its first ~100 rows and a token for the rest, so
     * a long list otherwise arrives silently truncated. Capped at
     * [MAX_PAGES] so a runaway feed can't hold the UI open forever, and a
     * failed page keeps whatever was already collected.
     *
     * Holds its caller until the last page lands, so it belongs behind things
     * nobody is watching — the library sync, an artist's back catalogue. For
     * anything a screen is waiting on, use [browseSongs] and [moreSongs].
     */
    private suspend fun songsPaged(browseId: String): List<Song> {
        val out = LinkedHashMap<String, Song>()
        var response = Innertube.browse(browseId)
        var page = 1
        while (true) {
            // Same shelf-scoping as pageOf: a playlist (Liked Music and the
            // Library Songs auto-playlist included) is read from its own
            // shelf so a trailing "Suggested tracks" shelf never joins in.
            val shelf = InnertubeParser.parsePlaylistShelf(response)
            (shelf?.songs ?: InnertubeParser.collectSongsDeep(response)).forEach { out[it.videoId] = it }
            val token = shelf?.continuation ?: InnertubeParser.continuationToken(response)
            if (token == null || page++ >= MAX_PAGES) break
            response = runCatching { Innertube.browseContinuation(token) }.getOrNull() ?: break
        }
        return out.values.toList()
    }

    const val MAX_PAGES = 10

    /**
     * Liked Music: the `LM` auto-playlist, addressed as a playlist browse id.
     * Public because it is also the page a track has to disappear from the
     * moment it stops being liked — see MainViewModel's `dropFromLikedLists`.
     */
    const val LIKED_MUSIC = "VLLM"

    /** Songs explicitly added to the library — distinct from Liked Music. */
    private const val LIBRARY_SONGS = "FEmusic_liked_videos"

    /** Saved and own playlists; also what the "add to playlist" picker lists. */
    private const val LIBRARY_PLAYLISTS = "FEmusic_liked_playlists"

    const val PLAYLISTS_SHELF = "Playlists"

    private val LIBRARY_FEEDS = listOf(
        PLAYLISTS_SHELF to LIBRARY_PLAYLISTS,
        "Albums" to "FEmusic_liked_albums",
        "Artists" to "FEmusic_library_corpus_track_artists",
        "Subscriptions" to "FEmusic_library_corpus_artists",
        "Podcasts" to "FEmusic_library_non_music_audio_list",
    )

    // ---- Writes -------------------------------------------------------------

    /**
     * The account's own state for one track — rating and library membership.
     *
     * Deliberately a lookup rather than something cached with the [Song]: a
     * track reaching the player through the queue has been round-tripped
     * through a MediaItem, which carries an id and little else, and the
     * feedback tokens are per-row anyway. Fetched when a menu is opened, which
     * is the only moment the answer is looked at.
     */
    suspend fun songMenu(videoId: String): Result<SongMenu> = call("menu:$videoId") {
        InnertubeParser.parseSongMenu(Innertube.next(videoId), videoId)
            ?: error("no menu for $videoId")
    }

    suspend fun rate(videoId: String, status: LikeStatus): Result<Unit> =
        call("rate:$videoId") { Innertube.rate(videoId, status) }

    /** Adds or removes a track from the library; [token] says which. */
    suspend fun setLibraryStatus(token: String): Result<Unit> =
        call("library:feedback") { Innertube.sendFeedback(token) }

    /**
     * Saves an album or playlist to the library, or removes it. [playlistId] is
     * the one the page named — see [LibraryState].
     */
    suspend fun setSaved(playlistId: String, saved: Boolean): Result<Unit> =
        call("library:$playlistId") { Innertube.ratePlaylist(playlistId, saved) }

    /**
     * The playlists a track can be added to. Not paged: an account with more
     * than one page of playlists is rare, and the picker is a list to scroll
     * rather than a feed to follow.
     */
    suspend fun userPlaylists(): Result<List<UserPlaylist>> = call("playlists") {
        InnertubeParser.parseUserPlaylists(Innertube.browse(LIBRARY_PLAYLISTS))
    }

    /** Creates a playlist, optionally seeded with [videoIds]; returns its id. */
    suspend fun createPlaylist(
        title: String,
        privacy: PlaylistPrivacy,
        videoIds: List<String> = emptyList(),
    ): Result<String> = call("playlist:create") {
        Innertube.createPlaylist(title, privacy, videoIds = videoIds)
    }

    suspend fun addToPlaylist(playlistId: String, videoIds: List<String>): Result<Unit> =
        call("playlist:add") { Innertube.addToPlaylist(playlistId, videoIds) }

    /** [entries] are (setVideoId, videoId) pairs — see [Song.setVideoId]. */
    suspend fun removeFromPlaylist(
        playlistId: String,
        entries: List<Pair<String, String>>,
    ): Result<Unit> = call("playlist:remove") {
        Innertube.removeFromPlaylist(playlistId, entries)
    }

    suspend fun renamePlaylist(playlistId: String, title: String): Result<Unit> =
        call("playlist:rename") { Innertube.renamePlaylist(playlistId, title) }

    suspend fun deletePlaylist(playlistId: String): Result<Unit> =
        call("playlist:delete") { Innertube.deletePlaylist(playlistId) }

    /**
     * Artist page. The landing page only lists ~5 songs, so the linked
     * "Top songs" playlist is fetched to fill the list out.
     */
    suspend fun artistPage(browseId: String): Result<ArtistPage> = call("artist:$browseId") {
        val page = InnertubeParser.parseArtistPage(Innertube.browse(browseId))
        val fullSongs = page.moreSongsBrowseId?.let { playlistId ->
            runCatching { songsPaged(playlistId) }.getOrNull()
        }
        if (!fullSongs.isNullOrEmpty()) page.copy(songs = fullSongs) else page
    }

    private suspend fun <T> call(label: String, block: suspend () -> T): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching { block() }
                // runCatching catches Throwable, cancellation included, which
                // would turn "the user typed another letter" into a failed
                // Result and put the abandoned request's error on screen.
                // Cancellation isn't this call's to answer for.
                .onFailure { if (it is CancellationException) throw it }
                .onSuccess { Log.d(TAG, "$label ok") }
                .onFailure { Log.w(TAG, "$label failed: ${it.message}") }
        }
}
