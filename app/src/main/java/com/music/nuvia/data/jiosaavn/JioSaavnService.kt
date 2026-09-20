package com.music.nuvia.data.jiosaavn

import android.util.Base64
import com.music.nuvia.data.TrackLog
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Serializable
data class RawArtistMapItem(
    val id: String = "",
    val name: String = "",
)

@Serializable
data class RawArtistMap(
    @SerialName("primary_artists") val primaryArtists: List<RawArtistMapItem> = emptyList(),
)

@Serializable
data class RawMoreInfo(
    val album_id: String = "",
    val album: String = "",
    @SerialName("encrypted_media_url") val encryptedMediaUrl: String = "",
    val duration: String = "",
    /**
     * Whether a 320kbps rendition exists, as `"true"`/`"false"`.
     *
     * The catalogue states this per track and it is frequently false. Asking
     * the CDN for `_320` anyway does not produce one.
     */
    @SerialName("320kbps") val has320: String = "",
    val artistMap: RawArtistMap = RawArtistMap(),
) {
    val supports320: Boolean get() = has320.equals("true", ignoreCase = true)
}

/** A decoded CDN URL and the bitrate it will really deliver. */
data class SaavnStream(val url: String, val kbps: Int?)

data class JioSaavnTestResult(
    val success: Boolean,
    val httpStatus: Int = 200,
    val latencyMs: Long = 0,
    val sampleTrack: String? = null,
    val songsCount: Int = 0,
    val streamDecryptVerified: Boolean = false,
    val streamBitrate: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class RawSongItem(
    val id: String = "",
    val title: String = "",
    val image: String = "",
    @SerialName("more_info") val moreInfo: RawMoreInfo = RawMoreInfo()
)

@Serializable
data class RawSearchResponse(
    val results: List<RawSongItem> = emptyList()
)

@Serializable
data class RawSongsResponse(
    val songs: List<RawSongItem> = emptyList()
)

object JioSaavnService {

    private const val TAG = "NUViA"
    
    // https://www.jiosaavn.com/api.php
    private val BASE_URL = String(Base64.decode("aHR0cHM6Ly93d3cuamlvc2Fhdm4uY29tL2FwaS5waHA=", Base64.DEFAULT), Charsets.UTF_8)

    private val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val client by lazy {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(json) }
            install(HttpTimeout) {
                requestTimeoutMillis = 6_000
                connectTimeoutMillis = 4_000
                socketTimeoutMillis = 6_000
            }
            defaultRequest {
                url(BASE_URL)
                headers.append(HttpHeaders.Accept, "application/json")
                headers.append(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36")
                headers.append("X-Forwarded-For", "49.36.0.1")
                headers.append("X-Real-IP", "49.36.0.1")
                headers.append("Accept-Language", "en-IN,en;q=0.9")
                headers.append(HttpHeaders.Cookie, "explicit_content=1")
            }
            expectSuccess = false
        }
    }

    private fun decryptUrl(encryptedUrl: String): String {
        if (encryptedUrl.isBlank()) return ""
        return try {
            val key = "38346591" // DES 8-byte key
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "DES")
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            val decodedBytes = Base64.decode(encryptedUrl, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (e: Exception) {
            TrackLog.e(TAG, "JioSaavn URL decryption failed", e)
            ""
        }
    }

    /**
     * The best CDN URL this track really has, and the bitrate it will deliver.
     */
    private fun bestStream(encryptedUrl: String, supports320: Boolean): SaavnStream? {
        val decryptedUrl = decryptUrl(encryptedUrl)
        if (decryptedUrl.isBlank()) return null
        val suffix = Regex("_(48|96|160|320)\\.(mp4|aac|mp3)$").find(decryptedUrl)
            ?: return SaavnStream(decryptedUrl, if (supports320) 320 else null)

        val offered = suffix.groupValues[1].toIntOrNull()
        val extension = suffix.groupValues[2]

        return if (supports320) {
            SaavnStream(decryptedUrl.replaceRange(suffix.range, "_320.$extension"), 320)
        } else {
            SaavnStream(decryptedUrl, offered)
        }
    }

    suspend fun searchSongs(query: String): List<RawSongItem> = runCatching {
        val response = client.get("") {
            parameter("__call", "search.getResults")
            parameter("_format", "json")
            parameter("_marker", "0")
            parameter("api_version", "4")
            parameter("ctx", "android")
            parameter("q", query)
            parameter("p", "1")
            parameter("n", "10")
        }

        if (response.status != HttpStatusCode.OK) {
            TrackLog.w(TAG, "Saavn search failed: HTTP ${response.status.value}")
            return@runCatching emptyList()
        }

        val body = json.decodeFromString<RawSearchResponse>(response.bodyAsText())
        body.results
    }.getOrElse {
        TrackLog.w(TAG, "Saavn search error: ${it.message}")
        emptyList()
    }

    suspend fun getStreamUrl(saavnSongId: String): SaavnStream? {
        val result = runCatching {
            val response = client.get("") {
                parameter("__call", "song.getDetails")
                parameter("_format", "json")
                parameter("_marker", "0")
                parameter("api_version", "4")
                parameter("ctx", "android")
                parameter("pids", saavnSongId)
            }

            if (response.status != HttpStatusCode.OK) {
                TrackLog.w(TAG, "Saavn getDetails failed: HTTP ${response.status.value}")
                return@runCatching null
            }

            val root = json.parseToJsonElement(response.bodyAsText()) as? JsonObject
                ?: return@runCatching null

            val songElement = (root["songs"] as? JsonArray)?.firstOrNull()
                ?: root.values.firstOrNull { it is JsonObject }
                ?: run {
                    TrackLog.w(TAG, "Saavn getDetails held no song for $saavnSongId")
                    return@runCatching null
                }

            val rawSong = json.decodeFromJsonElement(RawSongItem.serializer(), songElement)
            bestStream(rawSong.moreInfo.encryptedMediaUrl, rawSong.moreInfo.supports320)
        }

        return result.onFailure { TrackLog.w(TAG, "Saavn getDetails error: ${it.message}") }.getOrNull()
    }

    suspend fun testConnection(): JioSaavnTestResult {
        val start = System.currentTimeMillis()
        return try {
            val response = client.get("") {
                parameter("__call", "search.getResults")
                parameter("_format", "json")
                parameter("_marker", "0")
                parameter("api_version", "4")
                parameter("ctx", "android")
                parameter("q", "Starboy")
                parameter("p", "1")
                parameter("n", "5")
            }
            val latency = System.currentTimeMillis() - start
            if (response.status != HttpStatusCode.OK) {
                return JioSaavnTestResult(
                    success = false,
                    httpStatus = response.status.value,
                    latencyMs = latency,
                    errorMessage = "HTTP ${response.status.value} ${response.status.description}",
                )
            }
            val text = response.bodyAsText()
            val body = json.decodeFromString<RawSearchResponse>(text)
            if (body.results.isEmpty()) {
                return JioSaavnTestResult(
                    success = false,
                    httpStatus = 200,
                    latencyMs = latency,
                    errorMessage = "Received 0 search results from JioSaavn catalogue",
                )
            }
            val first = body.results.first()
            val artists = first.moreInfo.artistMap.primaryArtists.joinToString(", ") { it.name }
                .ifBlank { "Unknown Artist" }
            val sample = "${first.title} — $artists"
            val decrypted = decryptUrl(first.moreInfo.encryptedMediaUrl)
            val streamVerified = decrypted.isNotBlank()
            val bitrateStr = if (first.moreInfo.supports320) "320 kbps (HQ)" else "160/320 kbps"

            JioSaavnTestResult(
                success = true,
                httpStatus = 200,
                latencyMs = latency,
                sampleTrack = sample,
                songsCount = body.results.size,
                streamDecryptVerified = streamVerified,
                streamBitrate = bitrateStr,
            )
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - start
            TrackLog.e(TAG, "JioSaavn test connection failed", e)
            JioSaavnTestResult(
                success = false,
                httpStatus = 0,
                latencyMs = latency,
                errorMessage = e.localizedMessage ?: e.message ?: "Connection failed (Network/Timeout)",
            )
        }
    }
}
