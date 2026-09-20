package com.music.nuvia.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.music.nuvia.R
import com.music.nuvia.data.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while the download queue drains, and says so.
 *
 * A download is the one thing this app does that a user starts and then leaves:
 * they tap it and put the phone in a pocket. A coroutine on a ViewModel scope
 * would be killed the moment the activity goes, and a plain background service
 * on a modern Android is killed almost as fast — so this is a foreground
 * service, which is also the only honest arrangement, since a notification is
 * exactly what the user should get for work happening out of sight.
 *
 * It owns no state. The queue and everything known about it live in
 * [Downloads]; this drives that queue and reflects it into a notification, and
 * stops itself the moment there is nothing left to do.
 */
class DownloadService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    private var drain: Job? = null
    private var notifier: Job? = null

    /** What the notification is currently about. */
    @Volatile
    private var current: Song? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must happen within a few seconds of the start request whatever the
        // intent turns out to be, the cancel below included — a service started
        // with startForegroundService and never promoted takes the app down
        // with it.
        promote()

        if (intent?.action == ACTION_CANCEL_ALL) {
            Downloads.active.value.keys.toList().forEach(Downloads::cancel)
            shutdown(stopWork = true)
            return START_NOT_STICKY
        }

        if (drain == null) {
            drain = scope.launch {
                drainQueue()
                shutdown(stopWork = false)
            }
            notifier = scope.launch { reflectProgress() }
        }
        return START_NOT_STICKY
    }

    /**
     * [WORKERS] tracks at a time, each pulling from the same queue.
     */
    private suspend fun drainQueue() = coroutineScope {
        repeat(WORKERS) { launch { work() } }
    }

    private suspend fun work() {
        var idleFor = 0L
        while (true) {
            val song = Downloads.takeNext()
            if (song == null) {
                if (idleFor >= IDLE_GRACE_MS && !Downloads.busy()) return
                delay(IDLE_POLL_MS)
                idleFor += IDLE_POLL_MS
                continue
            }
            idleFor = 0L
            current = song
            postNotification()

            // Its own job, so one track can be cancelled out from under the
            // loop without taking the rest of the queue with it.
            val job = scope.launch { Downloads.run(this@DownloadService, song) }
            Downloads.onRunning(song.videoId, job)
            job.join()
            Downloads.onIdle(song.videoId)
        }
    }

    /** Repost as the running track advances, slowly enough not to thrash the shade. */
    private suspend fun reflectProgress() {
        Downloads.active.collect {
            postNotification()
            delay(PROGRESS_REFRESH_MS)
        }
    }

    private fun shutdown(stopWork: Boolean) {
        if (stopWork) drain?.cancel()
        notifier?.cancel()
        drain = null
        notifier = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        Downloads.onStopped()
        super.onDestroy()
    }

    // ---- Notification -------------------------------------------------------

    private fun promote() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun postNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
    }

    private fun buildNotification(): Notification {
        val active = Downloads.active.value
        val runningStates = active.values.filterIsInstance<DownloadState.Running>()
        val waiting = active.count { it.value is DownloadState.Queued }

        val percent = runningStates
            .takeIf { it.isNotEmpty() }
            ?.let { states -> states.sumOf { it.fraction.toDouble() } / states.size }
            ?.times(100)?.toInt()
            ?: 0

        val song = current
        val title = when {
            runningStates.size > 1 -> "Downloading ${runningStates.size} songs"
            else -> song?.title ?: "Downloading"
        }
        val text = when {
            runningStates.size > 1 && waiting > 0 -> "$waiting more queued"
            runningStates.size > 1 -> song?.title.orEmpty()
            song == null -> "Starting"
            waiting > 0 -> "${song.artist} · $waiting more queued"
            else -> song.artist
        }

        val cancel = PendingIntent.getService(
            this,
            0,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL_ALL),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setProgress(100, percent, runningStates.isEmpty())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Cancel", cancel)
            .build()
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Songs being saved to your Music folder"
                setShowBadge(false)
            },
        )
    }

    private companion object {
        const val CHANNEL_ID = "downloads"

        /** Distinct from playback's, which Media3 owns. */
        const val NOTIFICATION_ID = 0x8175

        const val ACTION_CANCEL_ALL = "com.music.nuvia.download.CANCEL_ALL"

        /** Four updates a second is smooth; the shade coalesces anything faster anyway. */
        const val PROGRESS_REFRESH_MS = 250L

        /**
         * How many tracks are fetched at once.
         */
        const val WORKERS = 4

        /**
         * How long a worker keeps looking at an empty queue before it accepts
         * the queue is empty.
         */
        const val IDLE_GRACE_MS = 2_000L

        const val IDLE_POLL_MS = 100L
    }
}
