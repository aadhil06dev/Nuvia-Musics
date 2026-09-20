package com.music.nuvia.ui.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.playback.AudioRouting

@Composable
internal fun rememberAudioOutputs(): List<AudioRouting.Device> {
    val context = LocalContext.current
    val manager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    val selectedId by AudioRouting.selectedId.collectAsStateWithLifecycle()
    var outputs by remember(manager) { mutableStateOf(AudioRouting.outputs(manager)) }

    DisposableEffect(manager, selectedId) {
        fun refresh() {
            outputs = AudioRouting.outputs(manager)
        }
        refresh()

        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        fun refreshSoon() {
            refresh()
            SETTLE_MS.forEach { delay -> handler.postDelayed(::refresh, delay) }
        }

        val deviceCallback = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = refreshSoon()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = refreshSoon()
        }
        manager.registerAudioDeviceCallback(deviceCallback, handler)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) = refreshSoon()
        }
        val filter = IntentFilter().apply {
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
            addAction(AudioManager.ACTION_HDMI_AUDIO_PLUG)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            manager.unregisterAudioDeviceCallback(deviceCallback)
            runCatching { context.unregisterReceiver(receiver) }
            handler.removeCallbacksAndMessages(null)
        }
    }
    return outputs
}

@Composable
internal fun outputLabel(device: AudioRouting.Device, accountName: String?): String {
    val context = LocalContext.current
    val firstName = accountName?.trim()?.split(Regex("\\s+"))?.firstOrNull()?.takeIf { it.isNotBlank() }
    return when {
        device.name.isNotBlank() -> device.name
        device.kind == AudioRouting.Kind.WIRED -> stringResource(R.string.wired_headphones)
        device.kind == AudioRouting.Kind.USB -> stringResource(R.string.usb_audio)
        device.kind == AudioRouting.Kind.HDMI -> stringResource(R.string.hdmi_output)
        firstName != null -> context.getString(R.string.personal_phone, firstName)
        else -> stringResource(R.string.this_phone)
    }
}

@Composable
internal fun rememberAudioOutputName(accountName: String?): String {
    val outputs = rememberAudioOutputs()
    val active = outputs.firstOrNull { it.isActive }
        ?: return if (accountName.isNullOrBlank()) {
            stringResource(R.string.this_phone)
        } else {
            LocalContext.current.getString(
                R.string.personal_phone,
                accountName.trim().split(Regex("\\s+")).first(),
            )
        }
    return outputLabel(active, accountName)
}

private val SETTLE_MS = longArrayOf(350L, 1_200L, 2_500L)

@Composable
internal fun rememberOutputPicker(onOpen: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val ask = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) onOpen() }
    return {
        if (bluetoothNamesAllowed(context)) onOpen() else ask.launch(BLUETOOTH_CONNECT)
    }
}

private fun bluetoothNamesAllowed(context: Context): Boolean =
    Build.VERSION.SDK_INT < 31 ||
        ContextCompat.checkSelfPermission(context, BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

private const val BLUETOOTH_CONNECT = "android.permission.BLUETOOTH_CONNECT"

