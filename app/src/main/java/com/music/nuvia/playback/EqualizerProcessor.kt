package com.music.nuvia.playback

import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * 10-band cascaded Cytomic SVF equalizer running in Media3's audio processor chain.
 */
@UnstableApi
class EqualizerProcessor : BaseAudioProcessor() {

    private class Tuning(val curve: EqCurve, val balance: Float) {
        companion object {
            val OFF = Tuning(EqCurve.FLAT, 0f)
        }
    }

    @Volatile
    private var target: Tuning = Tuning.OFF

    private var channelCount = 0
    private var sampleRate = 0

    private val currentGainDb = FloatArray(EqLayout.SLOTS)
    private val currentQ = FloatArray(EqLayout.SLOTS) { 0.707f }
    private var currentPreampDb = 0f
    private var currentBalance = 0f

    private val coeffA1 = FloatArray(EqLayout.SLOTS)
    private val coeffA2 = FloatArray(EqLayout.SLOTS)
    private val coeffA3 = FloatArray(EqLayout.SLOTS)
    private val mixInput = FloatArray(EqLayout.SLOTS)
    private val mixBand = FloatArray(EqLayout.SLOTS)
    private val mixLow = FloatArray(EqLayout.SLOTS)

    private val activeSlots = IntArray(EqLayout.SLOTS)
    private val running = BooleanArray(EqLayout.SLOTS)

    private var state = FloatArray(0)
    private var channelGain = FloatArray(0)

    fun setTuning(enabled: Boolean, curve: EqCurve, balance: Float) {
        target = if (enabled) Tuning(curve, balance.coerceIn(-1f, 1f)) else Tuning.OFF
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT || inputAudioFormat.channelCount < 1) {
            Log.w(
                TAG,
                "Equaliser inactive: encoding=${inputAudioFormat.encoding} " +
                    "channels=${inputAudioFormat.channelCount} is not 16-bit PCM",
            )
            return AudioProcessor.AudioFormat.NOT_SET
        }
        channelCount = inputAudioFormat.channelCount
        sampleRate = inputAudioFormat.sampleRate
        state = FloatArray(channelCount * EqLayout.SLOTS * 2)
        channelGain = FloatArray(channelCount) { 1f }
        running.fill(false)
        snapToTarget()
        return inputAudioFormat
    }

    override fun onFlush() {
        state.fill(0f)
        running.fill(false)
        snapToTarget()
    }

    override fun onReset() {
        state = FloatArray(0)
        channelGain = FloatArray(0)
        running.fill(false)
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val bytesPerFrame = BYTES_PER_SAMPLE * channelCount
        if (bytesPerFrame == 0) return
        val frameCount = inputBuffer.remaining() / bytesPerFrame
        if (frameCount == 0) return
        val outputBuffer = replaceOutputBuffer(frameCount * bytesPerFrame)

        val tuning = target
        if (isFlat(tuning) && isSettled(tuning)) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        inputBuffer.order(ByteOrder.nativeOrder())
        outputBuffer.order(ByteOrder.nativeOrder())

        var remaining = frameCount
        while (remaining > 0) {
            val block = min(remaining, GLIDE_FRAMES)
            glideTowards(tuning)
            val active = prepareSections()
            prepareChannelGains()

            repeat(block) {
                for (channel in 0 until channelCount) {
                    var sample = inputBuffer.short.toFloat()
                    for (index in 0 until active) {
                        sample = section(activeSlots[index], channel, sample)
                    }
                    outputBuffer.putShort(clampToShort(sample * channelGain[channel]))
                }
            }
            flushDenormals(active)
            remaining -= block
        }
        outputBuffer.flip()
    }

    private fun snapToTarget() {
        val tuning = target
        tuning.curve.gainsDb.copyInto(currentGainDb)
        tuning.curve.qs.copyInto(currentQ)
        currentPreampDb = tuning.curve.preampDb
        currentBalance = tuning.balance
    }

    private fun glideTowards(tuning: Tuning) {
        for (slot in 0 until EqLayout.SLOTS) {
            currentGainDb[slot] = linearGlide(currentGainDb[slot], tuning.curve.gainsDb[slot])
            currentQ[slot] = geometricGlide(currentQ[slot], tuning.curve.qs[slot])
        }
        currentPreampDb = linearGlide(currentPreampDb, tuning.curve.preampDb)
        currentBalance = linearGlide(currentBalance, tuning.balance)
    }

    private fun linearGlide(current: Float, target: Float): Float =
        current + (target - current) * GLIDE_RATE

    private fun geometricGlide(current: Float, target: Float): Float {
        val from = ln(current.coerceAtLeast(MIN_Q))
        val to = ln(target.coerceAtLeast(MIN_Q))
        return exp(from + (to - from) * GLIDE_RATE)
    }

    private fun isFlat(tuning: Tuning): Boolean =
        abs(tuning.balance) < SETTLED_BALANCE &&
            abs(tuning.curve.preampDb) < SETTLED_DB &&
            tuning.curve.gainsDb.all { abs(it) < SETTLED_DB }

    private fun isSettled(tuning: Tuning): Boolean {
        if (abs(currentBalance - tuning.balance) >= SETTLED_BALANCE) return false
        if (abs(currentPreampDb - tuning.curve.preampDb) >= SETTLED_DB) return false
        for (slot in 0 until EqLayout.SLOTS) {
            if (abs(currentGainDb[slot] - tuning.curve.gainsDb[slot]) >= SETTLED_DB) return false
        }
        return true
    }

    private fun prepareSections(): Int {
        var active = 0
        for (slot in 0 until EqLayout.SLOTS) {
            if (abs(currentGainDb[slot]) >= SETTLED_DB) {
                updateCoefficients(slot)
                activeSlots[active++] = slot
                running[slot] = true
            } else if (running[slot]) {
                clearState(slot)
                running[slot] = false
            }
        }
        return active
    }

    private fun updateCoefficients(slot: Int) {
        val spec = EqLayout.slots[slot]
        val a = 10f.pow(currentGainDb[slot] / 40f)
        val q = currentQ[slot].coerceAtLeast(MIN_Q)
        val base = tan(Math.PI * usableFrequency(spec.frequencyHz) / sampleRate).toFloat()
        val g: Float
        val k: Float
        when (spec.kind) {
            FilterKind.BELL -> {
                g = base
                k = 1f / (q * a)
                mixInput[slot] = 1f
                mixBand[slot] = k * (a * a - 1f)
                mixLow[slot] = 0f
            }
            FilterKind.LOW_SHELF -> {
                g = base / sqrt(a)
                k = 1f / q
                mixInput[slot] = 1f
                mixBand[slot] = k * (a - 1f)
                mixLow[slot] = a * a - 1f
            }
            FilterKind.HIGH_SHELF -> {
                g = base * sqrt(a)
                k = 1f / q
                mixInput[slot] = a * a
                mixBand[slot] = k * (1f - a) * a
                mixLow[slot] = 1f - a * a
            }
        }
        val d = 1f / (1f + g * (g + k))
        coeffA1[slot] = d
        coeffA2[slot] = g * d
        coeffA3[slot] = g * (g * d)
    }

    private fun usableFrequency(hz: Float): Float =
        hz.coerceIn(MIN_HZ, sampleRate * MAX_FREQUENCY_FRACTION)

    private fun prepareChannelGains() {
        val preamp = 10f.pow(currentPreampDb / 20f)
        if (channelCount == 2) {
            channelGain[0] = preamp * min(1f, 1f - currentBalance)
            channelGain[1] = preamp * min(1f, 1f + currentBalance)
        } else {
            channelGain.fill(preamp)
        }
    }

    private fun section(slot: Int, channel: Int, input: Float): Float {
        val i = (channel * EqLayout.SLOTS + slot) * 2
        val ic1 = state[i]
        val ic2 = state[i + 1]
        val v3 = input - ic2
        val v1 = coeffA1[slot] * ic1 + coeffA2[slot] * v3
        val v2 = ic2 + coeffA2[slot] * ic1 + coeffA3[slot] * v3
        state[i] = 2f * v1 - ic1
        state[i + 1] = 2f * v2 - ic2
        return mixInput[slot] * input + mixBand[slot] * v1 + mixLow[slot] * v2
    }

    private fun clearState(slot: Int) {
        for (channel in 0 until channelCount) {
            val i = (channel * EqLayout.SLOTS + slot) * 2
            state[i] = 0f
            state[i + 1] = 0f
        }
    }

    private fun flushDenormals(active: Int) {
        for (index in 0 until active) {
            val slot = activeSlots[index]
            for (channel in 0 until channelCount) {
                val i = (channel * EqLayout.SLOTS + slot) * 2
                if (abs(state[i]) < DENORMAL_FLOOR) state[i] = 0f
                if (abs(state[i + 1]) < DENORMAL_FLOOR) state[i + 1] = 0f
            }
        }
    }

    private fun clampToShort(value: Float): Short =
        value.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()

    companion object {
        private const val TAG = "NUViAEqualizer"
        private const val BYTES_PER_SAMPLE = 2
        private const val GLIDE_FRAMES = 64
        private const val GLIDE_RATE = 0.08f
        private const val SETTLED_DB = 0.01f
        private const val SETTLED_BALANCE = 0.0005f
        private const val MIN_Q = 0.05f
        private const val MIN_HZ = 10f
        private const val MAX_FREQUENCY_FRACTION = 0.45f
        private const val DENORMAL_FLOOR = 1e-12f
    }
}

