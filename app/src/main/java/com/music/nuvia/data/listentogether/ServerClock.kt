package com.music.nuvia.data.listentogether

import android.os.SystemClock

/**
 * This device's offset from the party server's clock.
 *
 * Implements NTP-style clock offset estimation using monotonic time.
 */
class ServerClock {

    private data class Sample(val offsetMs: Long, val roundTripMs: Long, val takenAtMs: Long)

    private val samples = ArrayDeque<Sample>()

    @Volatile
    var offsetMs: Long? = null
        private set

    @Volatile
    var roundTripMs: Long = 0
        private set

    /** True once at least one round trip has completed. */
    val synced: Boolean get() = offsetMs != null

    @Synchronized
    fun record(sentAtLocalMs: Long, serverMs: Long, receivedAtLocalMs: Long) {
        val roundTrip = (receivedAtLocalMs - sentAtLocalMs).coerceAtLeast(0)
        val midpoint = sentAtLocalMs + roundTrip / 2
        samples.addLast(Sample(serverMs - midpoint, roundTrip, receivedAtLocalMs))
        while (samples.size > WINDOW) samples.removeFirst()

        val cutoff = receivedAtLocalMs - SAMPLE_TTL_MS
        val usable = samples.filter { it.takenAtMs >= cutoff }.ifEmpty { samples.toList() }
        val best = usable.minBy { it.roundTripMs }
        offsetMs = best.offsetMs
        roundTripMs = best.roundTripMs
    }

    /** The server's clock, read from here. Null until the first pong lands. */
    fun serverNowMs(): Long? = offsetMs?.let { SystemClock.elapsedRealtime() + it }

    @Synchronized
    fun reset() {
        samples.clear()
        offsetMs = null
        roundTripMs = 0
    }

    companion object {
        fun localNowMs(): Long = SystemClock.elapsedRealtime()

        private const val WINDOW = 12
        private const val SAMPLE_TTL_MS = 120_000L
    }
}

