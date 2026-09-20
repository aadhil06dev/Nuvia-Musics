package com.music.nuvia.data.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.SerializationException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Robust network error mapper for NUViA.
 *
 * Guarantees that raw server payloads (e.g. Google HTTP 502 HTML error pages,
 * server stack traces, <!DOCTYPE html> tags, CSS styles, or internal exception traces)
 * NEVER reach the UI. All technical failures are translated into polite, domain-safe,
 * user-facing messages.
 */
object NetworkErrorMapper {

    /**
     * Maps any throwable to a sanitized, user-friendly error message.
     * Guaranteed free of raw HTML, response bodies, or technical jargon.
     */
    fun toUserMessage(throwable: Throwable?): String {
        if (throwable == null) return "Something went wrong. Please try again."

        // 1. Ktor Server Errors (5xx: 500, 502 Bad Gateway, 503, 504)
        if (throwable is ServerResponseException) {
            val code = throwable.response.status.value
            return when (code) {
                502, 504 -> "Couldn't reach the music service right now. Please check back shortly."
                503 -> "The music service is temporarily overloaded. Please try again in a moment."
                else -> "The service encountered a temporary problem ($code). Please try again."
            }
        }

        // 2. Ktor Client Errors (4xx)
        if (throwable is ClientRequestException) {
            val code = throwable.response.status.value
            return when (code) {
                HttpStatusCode.NotFound.value -> "This content is no longer available."
                HttpStatusCode.Unauthorized.value, HttpStatusCode.Forbidden.value ->
                    "Access restricted — please check your account connection."
                HttpStatusCode.TooManyRequests.value -> "Too many requests. Please wait a moment and try again."
                else -> "Unable to load this content ($code). Please try again."
            }
        }

        // 3. Timeouts
        if (throwable is HttpRequestTimeoutException ||
            throwable is SocketTimeoutException ||
            throwable.message?.contains("timeout", ignoreCase = true) == true
        ) {
            return "Connection timed out. Please check your network and try again."
        }

        // 4. Connectivity & Host Resolution Drops
        if (throwable is UnknownHostException ||
            throwable is ConnectException ||
            throwable.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
            throwable.message?.contains("No address associated with hostname", ignoreCase = true) == true ||
            throwable.message?.contains("Network is unreachable", ignoreCase = true) == true
        ) {
            return "No internet connection. Please check your network."
        }

        // 5. Serialization / Malformed Response (often when server returned unexpected HTML)
        if (throwable is SerializationException) {
            return "Received an unreadable response from the server. Please try again."
        }

        // 6. Inspect raw message for accidental HTML / 502 / Server Error leakage
        val rawMsg = throwable.message.orEmpty()
        if (containsRawServerHtml(rawMsg)) {
            return "The music service is temporarily unavailable. Please try again."
        }

        // 7. General I/O
        if (throwable is IOException) {
            return "Network connection interrupted. Please try again."
        }

        // 8. Safe Fallback — never echo raw message if it looks technical
        return if (isSafeGenericMessage(rawMsg)) {
            rawMsg
        } else {
            "Something went wrong while loading this content. Please try again."
        }
    }

    /**
     * Checks if the message string contains HTML tags, doctype, or server error headers.
     */
    fun containsRawServerHtml(text: String): Boolean {
        if (text.isBlank()) return false
        val lower = text.lowercase()
        return lower.contains("<!doctype") ||
            lower.contains("<html") ||
            lower.contains("<head>") ||
            lower.contains("<body>") ||
            lower.contains("<title>error 502") ||
            lower.contains("502 (server error)") ||
            lower.contains("502 bad gateway") ||
            lower.contains("500 internal server error") ||
            lower.contains("nginx") ||
            lower.contains("google.com/error") ||
            lower.contains("<style")
    }

    private fun isSafeGenericMessage(text: String): Boolean {
        if (text.isBlank() || text.length > 80) return false
        if (containsRawServerHtml(text)) return false
        val lower = text.lowercase()
        val technicalTerms = listOf(
            "exception", "nullpointer", "illegalstate", "stacktrace",
            "at com.", "at okhttp", "at io.ktor", "at android.", "at java.",
            "http/", "status code", "errno", "connection refused", "broken pipe"
        )
        return technicalTerms.none { lower.contains(it) }
    }
}

