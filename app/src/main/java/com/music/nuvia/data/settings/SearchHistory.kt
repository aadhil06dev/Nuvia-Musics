package com.music.nuvia.data.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * What's been searched for lately, kept on this device only.
 *
 * YouTube Music has an account-level search history, but reading it back needs
 * a separate signed-in call on every keystroke's worth of empty query — and it
 * would be wrong for a signed-out user. A local list is instant, works either
 * way, and is what Spotify and Apple Music show in the same spot.
 *
 * Stored as JSON rather than a delimited string: a query can contain any
 * character, separators included.
 */
object SearchHistory {

    /** Deep enough to be useful, shallow enough that the list stays scannable. */
    private const val MAX_ENTRIES = 20

    private lateinit var prefs: SharedPreferences
    private val json = Json
    private val serializer = ListSerializer(String.serializer())

    private val _recent = MutableStateFlow<List<String>>(emptyList())

    /** Most recent first. */
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("bitchord_settings", Context.MODE_PRIVATE)
        _recent.value = runCatching {
            json.decodeFromString(serializer, prefs.getString(KEY_HISTORY, null) ?: "[]")
        }.getOrDefault(emptyList())
    }

    /** Records [query], or moves it back to the top if it's already there. */
    fun record(query: String) {
        if (!AppSettings.searchHistoryEnabled.value) return
        val term = query.trim()
        if (term.isEmpty()) return
        val deduped = _recent.value.filterNot { it.equals(term, ignoreCase = true) }
        save((listOf(term) + deduped).take(MAX_ENTRIES))
    }

    fun remove(query: String) {
        save(_recent.value.filterNot { it.equals(query, ignoreCase = true) })
    }

    fun clear() = save(emptyList())

    /** Restores search history from backup, either merging or overwriting. */
    fun restore(queries: List<String>, overwrite: Boolean) {
        val clean = queries.map { it.trim() }.filter { it.isNotBlank() }
        val finalQueries = if (overwrite) {
            val seen = mutableSetOf<String>()
            val result = mutableListOf<String>()
            for (q in clean) {
                if (seen.add(q.lowercase())) result.add(q)
            }
            result
        } else {
            val current = _recent.value
            val combined = (current + clean)
            val seen = mutableSetOf<String>()
            val result = mutableListOf<String>()
            for (q in combined) {
                if (seen.add(q.lowercase())) result.add(q)
            }
            result
        }
        save(finalQueries.take(MAX_ENTRIES))
    }

    private fun save(value: List<String>) {
        _recent.value = value
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_HISTORY, json.encodeToString(serializer, value)).apply()
        }
    }

    private const val KEY_HISTORY = "search_history"
}
