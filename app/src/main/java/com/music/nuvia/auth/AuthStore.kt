package com.music.nuvia.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.music.nuvia.data.DebugLog as Log

/**
 * Encrypted-at-rest storage for credentials.
 *
 * Supports multi-account sessions, brand channel profiles, and Discord tokens.
 * Keystore init fails on a handful of OEM builds, so it degrades to plain
 * prefs rather than crashing on launch.
 */
class AuthStore(context: Context) {

    private val prefs: SharedPreferences = runCatching {
        EncryptedSharedPreferences.create(
            context,
            "nuvia_auth",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse {
        Log.w("NUViA", "EncryptedSharedPreferences unavailable, falling back: ${it.message}")
        context.getSharedPreferences("nuvia_auth_plain", Context.MODE_PRIVATE)
    }

    var cookie: String?
        get() = prefs.getString(KEY_COOKIE, null)
        set(value) = prefs.edit().putString(KEY_COOKIE, value).apply()

    /**
     * The durable account registry. Credentials remain in this encrypted store;
     * the old single-cookie entry is migrated lazily so an update never logs a
     * listener out.
     */
    var sessions: List<GoogleAccountSession>
        get() {
            val saved = sessionsFromJson(prefs.getString(KEY_SESSIONS, null))
            if (saved.isNotEmpty()) return saved
            val legacy = cookie ?: return emptyList()
            val profile = YouTubeProfile(
                profileId = profileId(channelPageId, channelDataSyncId, channelName ?: "Personal"),
                name = channelName ?: "Personal",
                pageId = channelPageId,
                dataSyncId = channelDataSyncId,
                authUser = channelAuthUser,
                isBrandAccount = channelPageId != null,
            )
            return listOf(GoogleAccountSession(
                accountId = sessionId(legacy, channelDataSyncId),
                cookie = legacy,
                profiles = listOf(profile),
                activeProfileId = profile.profileId,
            )).also { replaceSessions(it) }
        }
        set(value) = replaceSessions(value)

    var activeAccountId: String?
        get() = prefs.getString(KEY_ACTIVE_ACCOUNT, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_ACCOUNT, value).apply()

    var activeProfileId: String?
        get() = prefs.getString(KEY_ACTIVE_PROFILE, null)
        set(value) = prefs.edit().putString(KEY_ACTIVE_PROFILE, value).apply()

    val activeSession: GoogleAccountSession?
        get() = sessions.firstOrNull { it.accountId == activeAccountId }
            ?: sessions.firstOrNull()

    fun replaceSessions(value: List<GoogleAccountSession>) {
        prefs.edit().putString(KEY_SESSIONS, value.toJson()).apply()
    }

    fun upsertSession(session: GoogleAccountSession, activate: Boolean = true) {
        val next = sessions.filterNot { it.accountId == session.accountId } + session
        replaceSessions(next)
        if (activate) select(session.accountId, session.activeProfileId)
    }

    fun select(accountId: String, profileId: String?) {
        activeAccountId = accountId
        activeProfileId = profileId
        val session = sessions.firstOrNull { it.accountId == accountId }
        if (session != null) {
            cookie = session.cookie
            val profile = session.profiles.firstOrNull { it.profileId == profileId }
            if (profile != null) {
                selectChannel(profile.pageId, profile.dataSyncId, profile.name, profile.authUser)
            }
        }
    }

    fun removeAccount(accountId: String): GoogleAccountSession? {
        val remaining = sessions.filterNot { it.accountId == accountId }
        replaceSessions(remaining)
        val fallback = remaining.firstOrNull()
        select(fallback?.accountId.orEmpty(), fallback?.activeProfileId)
        prefs.edit().putString(KEY_COOKIE, fallback?.cookie).apply()
        return fallback
    }

    val isSignedIn: Boolean
        get() = (activeSession?.cookie ?: cookie)?.let { hasApiSid(it) } == true

    /** The Discord account's bearer token. See DiscordRPC for why a user token. */
    var discordToken: String?
        get() = prefs.getString(KEY_DISCORD_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_DISCORD_TOKEN, value).apply()

    val channelPageId: String? get() = prefs.getString(KEY_CHANNEL_PAGE_ID, null)
    val channelDataSyncId: String? get() = prefs.getString(KEY_CHANNEL_DATASYNC_ID, null)
    val channelName: String? get() = prefs.getString(KEY_CHANNEL_NAME, null)
    val channelAuthUser: String? get() = prefs.getString(KEY_CHANNEL_AUTH_USER, null)

    fun selectChannel(
        pageId: String?,
        dataSyncId: String?,
        name: String?,
        authUser: String? = null,
    ) = prefs.edit()
        .putString(KEY_CHANNEL_PAGE_ID, pageId)
        .putString(KEY_CHANNEL_DATASYNC_ID, dataSyncId)
        .putString(KEY_CHANNEL_NAME, name)
        .putString(KEY_CHANNEL_AUTH_USER, authUser)
        .apply()

    fun setChannelName(name: String?) =
        prefs.edit().putString(KEY_CHANNEL_NAME, name).apply()

    fun clearChannel() = prefs.edit()
        .remove(KEY_CHANNEL_PAGE_ID)
        .remove(KEY_CHANNEL_DATASYNC_ID)
        .remove(KEY_CHANNEL_NAME)
        .remove(KEY_CHANNEL_AUTH_USER)
        .apply()

    fun onNewSession(cookie: String) {
        this.cookie = cookie
        clearChannel()
    }

    fun signOut() {
        prefs.edit().remove(KEY_COOKIE).remove(KEY_SESSIONS)
            .remove(KEY_ACTIVE_ACCOUNT).remove(KEY_ACTIVE_PROFILE).apply()
        clearChannel()
        runCatching {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        }
    }

    companion object {
        fun hasApiSid(cookieHeader: String): Boolean =
            cookieHeader.split(';').any { entry ->
                val name = entry.substringBefore('=').trim()
                val value = entry.substringAfter('=', "").trim()
                name in API_SID_NAMES && value.isNotEmpty()
            }

        private val API_SID_NAMES =
            setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")

        private const val KEY_COOKIE = "cookie"
        private const val KEY_SESSIONS = "google_account_sessions_v2"
        private const val KEY_ACTIVE_ACCOUNT = "active_google_account_id_v2"
        private const val KEY_ACTIVE_PROFILE = "active_youtube_profile_id_v2"
        private const val KEY_CHANNEL_PAGE_ID = "channel_page_id"
        private const val KEY_CHANNEL_DATASYNC_ID = "channel_datasync_id"
        private const val KEY_CHANNEL_NAME = "channel_name"
        private const val KEY_CHANNEL_AUTH_USER = "channel_auth_user"
        private const val KEY_DISCORD_TOKEN = "discord_token"
    }
}
