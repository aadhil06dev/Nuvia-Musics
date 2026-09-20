package com.music.nuvia.playback

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A request from outside the app to open the full player.
 *
 * The activity relays incoming intents here because the Compose UI owns
 * the now-playing state and watches this flow for external player requests.
 *
 * Because the activity is `singleTask`, intents can arrive through both
 * `onCreate` and `onNewIntent`, so both paths relay through this object.
 */
object PlayerDeepLink {

    /** Marks an incoming request to open the full player. */
    const val EXTRA_OPEN_PLAYER = "nuvia.openPlayer"

    private val _pending = MutableStateFlow(false)

    /** Whether a request is outstanding. Cleared by [handled]. */
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    /** Reads an incoming intent, and reports whether it asked for the player. */
    fun consume(intent: Intent?): Boolean {
        if (intent == null) return false
        val matched = intent.getBooleanExtra(EXTRA_OPEN_PLAYER, false)
        if (!matched) return false
        intent.removeExtra(EXTRA_OPEN_PLAYER)
        _pending.value = true
        return true
    }

    /**
     * Called once the player has actually been opened.
     *
     * The flag has to be cleared by whoever acts on it, not by whoever set it:
     * this object outlives the composition, so a request left standing would be
     * served again by the next composition — which is to say, the sheet would
     * spring back open the first time the activity was recreated after the user
     * dismissed it.
     */
    fun handled() {
        _pending.value = false
    }
}


