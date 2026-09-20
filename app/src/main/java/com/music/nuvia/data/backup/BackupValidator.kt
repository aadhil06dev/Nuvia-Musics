package com.music.nuvia.data.backup

import com.music.nuvia.data.backup.model.NuviaBackup
import com.music.nuvia.data.backup.model.PlaylistBackup
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class ValidationResult {
    data class Success(
        val backup: NuviaBackup,
        val summary: BackupSummary,
        val warnings: List<String> = emptyList(),
        val duplicatePlaylists: List<DuplicatePlaylistInfo> = emptyList(),
    ) : ValidationResult()

    data class Error(
        val message: String,
        val cause: Throwable? = null,
    ) : ValidationResult()
}

data class BackupSummary(
    val formatVersion: Int,
    val appName: String,
    val appVersion: String,
    val exportedAtMillis: Long,
    val exportDateFormatted: String,
    val playlistCount: Int,
    val totalTrackCount: Int,
    val pinnedPlaylistCount: Int,
    val searchHistoryCount: Int,
    val hasSettings: Boolean,
    val hasQueue: Boolean,
)

data class DuplicatePlaylistInfo(
    val id: String,
    val title: String,
    val reason: String,
)

object BackupValidator {

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        prettyPrint = true
        coerceInputValues = true
    }

    /**
     * Validates raw JSON string content against schema, app identity, version, and integrity rules.
     * Safely handles unknown fields from future versions, checks for internal duplicates,
     * and identifies conflicts with existing playlists.
     */
    fun validate(
        rawJson: String,
        existingPlaylistIds: Set<String> = emptySet(),
        existingPlaylistTitles: Set<String> = emptySet(),
    ): ValidationResult {
        if (rawJson.isBlank()) {
            return ValidationResult.Error("Backup file is empty")
        }

        val backup: NuviaBackup = try {
            json.decodeFromString(NuviaBackup.serializer(), rawJson)
        } catch (e: Exception) {
            return ValidationResult.Error("Malformed or corrupted JSON backup file: ${e.message}", e)
        }

        // 1. App identity check
        if (backup.app !in NuviaBackup.SUPPORTED_APPS) {
            return ValidationResult.Error(
                "Incompatible backup: expected ${NuviaBackup.APP_NAME} format, found '${backup.app}'",
            )
        }

        // 2. Format version check
        if (backup.formatVersion <= 0) {
            return ValidationResult.Error("Invalid format version: ${backup.formatVersion}")
        }
        if (backup.formatVersion > NuviaBackup.CURRENT_FORMAT_VERSION) {
            return ValidationResult.Error(
                "This backup was created with a newer version of NUViA (format v${backup.formatVersion}, current supported is v${NuviaBackup.CURRENT_FORMAT_VERSION}). Please update NUViA to restore it.",
            )
        }

        val warnings = mutableListOf<String>()

        // 3. Validate & sanitize playlists
        val validPlaylists = mutableListOf<PlaylistBackup>()
        val seenInternalIds = mutableSetOf<String>()
        val duplicates = mutableListOf<DuplicatePlaylistInfo>()

        for (playlist in backup.playlists) {
            val id = playlist.id.trim()
            val title = playlist.title.trim()

            if (id.isEmpty() && title.isEmpty()) {
                warnings.add("Skipped an unidentifiable empty playlist")
                continue
            }

            val validTracks = playlist.tracks.filter { track ->
                track.videoId.isNotBlank() && track.title.isNotBlank()
            }

            val sanitizedPlaylist = playlist.copy(
                id = id.ifEmpty { "pl_${System.currentTimeMillis()}_${validPlaylists.size}" },
                title = title.ifEmpty { "Restored Playlist" },
                tracks = validTracks,
            )

            // Check internal duplicates within the backup file
            if (!seenInternalIds.add(sanitizedPlaylist.id)) {
                duplicates.add(
                    DuplicatePlaylistInfo(
                        id = sanitizedPlaylist.id,
                        title = sanitizedPlaylist.title,
                        reason = "Duplicate playlist ID '${sanitizedPlaylist.id}' found inside backup file",
                    )
                )
            }

            // Check conflicts with existing user data
            if (sanitizedPlaylist.id in existingPlaylistIds || sanitizedPlaylist.id.removePrefix("VL") in existingPlaylistIds) {
                duplicates.add(
                    DuplicatePlaylistInfo(
                        id = sanitizedPlaylist.id,
                        title = sanitizedPlaylist.title,
                        reason = "Playlist with ID '${sanitizedPlaylist.id}' already exists in your library",
                    )
                )
            } else if (existingPlaylistTitles.any { it.equals(sanitizedPlaylist.title, ignoreCase = true) }) {
                duplicates.add(
                    DuplicatePlaylistInfo(
                        id = sanitizedPlaylist.id,
                        title = sanitizedPlaylist.title,
                        reason = "Playlist titled '${sanitizedPlaylist.title}' already exists in your library",
                    )
                )
            }

            validPlaylists.add(sanitizedPlaylist)
        }

        val sanitizedPinned = backup.pinnedPlaylists.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val sanitizedHistory = backup.searchHistory.map { it.trim() }.filter { it.isNotEmpty() }.distinct()

        val sanitizedBackup = backup.copy(
            playlists = validPlaylists,
            pinnedPlaylists = sanitizedPinned,
            searchHistory = sanitizedHistory,
        )

        val totalTracks = validPlaylists.sumOf { it.tracks.size }
        val dateFormatted = try {
            SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(backup.exportedAt))
        } catch (_: Exception) {
            "Unknown date"
        }

        val summary = BackupSummary(
            formatVersion = backup.formatVersion,
            appName = backup.app,
            appVersion = backup.appVersion,
            exportedAtMillis = backup.exportedAt,
            exportDateFormatted = dateFormatted,
            playlistCount = validPlaylists.size,
            totalTrackCount = totalTracks,
            pinnedPlaylistCount = sanitizedPinned.size,
            searchHistoryCount = sanitizedHistory.size,
            hasSettings = backup.settings != null,
            hasQueue = backup.queueSnapshot?.tracks?.isNotEmpty() == true,
        )

        return ValidationResult.Success(
            backup = sanitizedBackup,
            summary = summary,
            warnings = warnings,
            duplicatePlaylists = duplicates,
        )
    }
}
