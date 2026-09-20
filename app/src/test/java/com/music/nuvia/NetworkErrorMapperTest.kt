package com.music.nuvia

import com.music.nuvia.data.network.NetworkErrorMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class NetworkErrorMapperTest {

    @Test
    fun `suppresses raw Google 502 HTML error page`() {
        val rawHtmlError = RuntimeException(
            "<!DOCTYPE html><html lang=en><title>Error 502 (Server Error)!!1</title>" +
                "<p><b>502.</b> <ins>That’s an error.</ins><p>The server encountered a temporary error."
        )
        val userMsg = NetworkErrorMapper.toUserMessage(rawHtmlError)
        assertFalse(userMsg.contains("<!DOCTYPE", ignoreCase = true))
        assertFalse(userMsg.contains("<html", ignoreCase = true))
        assertFalse(userMsg.contains("502", ignoreCase = true))
        assertEquals("The music service is temporarily unavailable. Please try again.", userMsg)
    }

    @Test
    fun `suppresses server stack traces and technical jargon`() {
        val stackTraceError = RuntimeException(
            "NullPointerException at com.music.nuvia.data.innertube.Innertube.browse(Innertube.kt:412)"
        )
        val userMsg = NetworkErrorMapper.toUserMessage(stackTraceError)
        assertFalse(userMsg.contains("NullPointerException"))
        assertFalse(userMsg.contains("Innertube.kt"))
        assertEquals("Something went wrong while loading this content. Please try again.", userMsg)
    }

    @Test
    fun `maps connection drops and host resolution failures`() {
        val hostError = UnknownHostException("Unable to resolve host \"music.youtube.com\": No address associated with hostname")
        val userMsg1 = NetworkErrorMapper.toUserMessage(hostError)
        assertEquals("No internet connection. Please check your network.", userMsg1)

        val connectError = ConnectException("Failed to connect to music.youtube.com")
        val userMsg2 = NetworkErrorMapper.toUserMessage(connectError)
        assertEquals("No internet connection. Please check your network.", userMsg2)
    }

    @Test
    fun `maps timeouts properly`() {
        val timeoutError = SocketTimeoutException("Read timed out")
        val userMsg = NetworkErrorMapper.toUserMessage(timeoutError)
        assertEquals("Connection timed out. Please check your network and try again.", userMsg)
    }

    @Test
    fun `maps generic IO interruption`() {
        val ioError = IOException("Stream closed unexpectedly")
        val userMsg = NetworkErrorMapper.toUserMessage(ioError)
        assertEquals("Network connection interrupted. Please try again.", userMsg)
    }

    @Test
    fun `handles null throwable gracefully`() {
        val userMsg = NetworkErrorMapper.toUserMessage(null)
        assertEquals("Something went wrong. Please try again.", userMsg)
    }

    @Test
    fun `correctly identifies raw HTML patterns`() {
        assertTrue(NetworkErrorMapper.containsRawServerHtml("<html><body>Error</body></html>"))
        assertTrue(NetworkErrorMapper.containsRawServerHtml("<!doctype html>"))
        assertTrue(NetworkErrorMapper.containsRawServerHtml("502 Bad Gateway"))
        assertTrue(NetworkErrorMapper.containsRawServerHtml("<title>Error 502</title>"))
        assertFalse(NetworkErrorMapper.containsRawServerHtml("Network disconnected"))
    }
}

