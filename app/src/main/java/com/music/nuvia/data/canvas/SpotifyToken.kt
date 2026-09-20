package com.music.nuvia.data.canvas

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import com.music.nuvia.data.DebugLog as Log
import com.music.nuvia.data.Http
import com.music.nuvia.data.settings.AppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

/**
 * Mint Spotify bearer token from user session cookie.
 */
internal object SpotifyToken {

    private const val TAG = "SpotifyToken"
    private const val BRIDGE_NAME = "NUViASpotifyTokenBridge"
    private const val HARVEST_TIMEOUT_MS = 20_000L
    private const val DEFAULT_TOKEN_LIFETIME_MS = 3_600_000L

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val harvestMutex = Mutex()

    @Volatile private var appContext: Context? = null

    @Volatile private var cachedAccessToken: String? = null
    @Volatile private var accessTokenExpiresAtMs = 0L
    @Volatile private var cachedClientId: String? = null

    @Volatile private var cachedSession: SessionInfo? = null
    @Volatile private var cachedClientToken: String? = null
    @Volatile private var clientTokenExpiresAtMs = 0L

    private data class SessionInfo(val clientVersion: String, val deviceId: String)
    private data class HarvestedToken(val token: String, val expiresAt: Long, val clientId: String?)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    suspend fun accessToken(): String? {
        val cookie = AppSettings.spotifySpdcToken.value
        if (cookie.isBlank()) return null

        val now = System.currentTimeMillis()
        cachedAccessToken?.let { if (now < accessTokenExpiresAtMs - 30_000) return it }

        return harvestMutex.withLock {
            val stillNow = System.currentTimeMillis()
            cachedAccessToken?.let { if (stillNow < accessTokenExpiresAtMs - 30_000) return@withLock it }

            val context = appContext
            if (context == null) {
                Log.w(TAG, "SpotifyToken.init was never called; no context for the harvest")
                return@withLock null
            }

            val harvested = withContext(Dispatchers.Main) { harvestViaWebView(context, cookie) }
            if (harvested == null) {
                Log.w(TAG, "token harvest failed or timed out")
                return@withLock null
            }

            cachedAccessToken = harvested.token
            accessTokenExpiresAtMs = harvested.expiresAt
            harvested.clientId?.let { cachedClientId = it }
            Log.d(TAG, "harvested access token, good until ${java.util.Date(harvested.expiresAt)}")
            harvested.token
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun harvestViaWebView(context: Context, cookie: String): HarvestedToken? {
        val deferred = CompletableDeferred<HarvestedToken?>()

        val cookieManager = CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setCookie("https://open.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            setCookie("https://accounts.spotify.com/", "sp_dc=$cookie; Domain=.spotify.com; Path=/; Secure")
            flush()
        }
        runCatching { WebStorage.getInstance().deleteAllData() }

        var webView: WebView? = null
        return try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = CANVAS_UA
                cookieManager.setAcceptThirdPartyCookies(this, true)
                addJavascriptInterface(TokenBridge(deferred), BRIDGE_NAME)

                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        super.onPageFinished(view, url)
                        view.evaluateJavascript(HOOK_SCRIPT, null)
                    }
                }
                loadUrl("https://open.spotify.com/")
            }

            withTimeoutOrNull(HARVEST_TIMEOUT_MS) { deferred.await() }
        } catch (e: Exception) {
            Log.w(TAG, "token harvest threw: ${e.message}")
            null
        } finally {
            runCatching {
                webView?.removeJavascriptInterface(BRIDGE_NAME)
                webView?.stopLoading()
                webView?.destroy()
            }
        }
    }

    private class TokenBridge(private val deferred: CompletableDeferred<HarvestedToken?>) {
        @JavascriptInterface
        fun onTokenPayload(payload: String?) {
            if (payload.isNullOrBlank() || deferred.isCompleted) return
            runCatching {
                val root = json.parseToJsonElement(payload).jsonObject
                val token = root["accessToken"]?.jsonPrimitive?.contentOrNull
                val anonymous = root["isAnonymous"]?.jsonPrimitive?.contentOrNull
                    ?.toBooleanStrictOrNull() ?: false
                if (token.isNullOrBlank() || anonymous) return
                val expiresAt = root["accessTokenExpirationTimestampMs"]?.jsonPrimitive?.contentOrNull
                    ?.toLongOrNull()?.takeIf { it > System.currentTimeMillis() }
                    ?: (System.currentTimeMillis() + DEFAULT_TOKEN_LIFETIME_MS)
                val clientId = root["clientId"]?.jsonPrimitive?.contentOrNull
                deferred.complete(HarvestedToken(token, expiresAt, clientId))
            }
        }
    }

    private val HOOK_SCRIPT = """
        (function () {
          if (window.__nuviaTokenHook) return;
          window.__nuviaTokenHook = true;
          var report = function (body) {
            try { $BRIDGE_NAME.onTokenPayload(body); } catch (e) {}
          };
          var isToken = function (u) {
            try { return String(u).indexOf('/api/token') !== -1; } catch (e) { return false; }
          };
          var origFetch = window.fetch;
          if (origFetch) {
            window.fetch = function (input, init) {
              var url = (input && input.url) ? input.url : input;
              var result = origFetch.apply(this, arguments);
              if (isToken(url)) {
                try {
                  result.then(function (res) {
                    res.clone().text().then(report).catch(function () {});
                  }).catch(function () {});
                } catch (e) {}
              }
              return result;
            };
          }
          var origOpen = XMLHttpRequest.prototype.open;
          XMLHttpRequest.prototype.open = function (method, url) {
            this.__nuviaUrl = url;
            return origOpen.apply(this, arguments);
          };
          var origSend = XMLHttpRequest.prototype.send;
          XMLHttpRequest.prototype.send = function () {
            var xhr = this;
            try {
              xhr.addEventListener('load', function () {
                if (isToken(xhr.__nuviaUrl)) {
                  try { report(xhr.responseText); } catch (e) {}
                }
              });
            } catch (e) {}
            return origSend.apply(this, arguments);
          };
        })();
    """.trimIndent()

    @Synchronized
    fun clientToken(): String? {
        val now = System.currentTimeMillis()
        cachedClientToken?.let { if (now < clientTokenExpiresAtMs - 30_000) return it }

        val clientId = cachedClientId
        if (clientId == null) {
            Log.w(TAG, "no client id yet (access token not minted); skipping client token")
            return null
        }
        val session = session() ?: return null

        val payload = buildJsonObject {
            putJsonObject("client_data") {
                put("client_version", session.clientVersion)
                put("client_id", clientId)
                putJsonObject("js_sdk_data") {
                    put("device_brand", "unknown")
                    put("device_model", "unknown")
                    put("os", "android")
                    put("os_version", android.os.Build.VERSION.RELEASE.orEmpty())
                    put("device_id", session.deviceId)
                    put("device_type", "smartphone")
                }
            }
        }

        val request = Request.Builder()
            .url("https://clienttoken.spotify.com/v1/clienttoken")
            .post(payload.toString().toByteArray(Charsets.UTF_8).toRequestBody("application/json".toMediaType()))
            .header("Accept", "application/json")
            .header("User-Agent", CANVAS_UA)
            .build()

        var lastCode = -1
        val body = runCatching {
            Http.client.newCall(request).execute().use { response ->
                lastCode = response.code
                if (response.isSuccessful) response.body?.string() else null
            }
        }.onFailure { Log.w(TAG, "client-token request threw: ${it.message}") }.getOrNull()
        if (body == null) {
            Log.w(TAG, "client-token request failed, http $lastCode")
            return null
        }

        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (root == null) {
            Log.w(TAG, "client-token response wasn't JSON")
            return null
        }
        val responseType = root["response_type"]?.jsonPrimitive?.contentOrNull
        if (responseType != "RESPONSE_GRANTED_TOKEN_RESPONSE") {
            Log.w(TAG, "client-token request rejected: $responseType")
            return null
        }
        val granted = root["granted_token"]?.jsonObject
        val token = granted?.get("token")?.jsonPrimitive?.contentOrNull
        if (token == null) {
            Log.w(TAG, "client-token response had no granted_token.token")
            return null
        }
        val ttlSeconds = granted["expires_after_seconds"]?.jsonPrimitive?.contentOrNull
            ?.toLongOrNull() ?: 3600L

        cachedClientToken = token
        clientTokenExpiresAtMs = now + ttlSeconds * 1000
        Log.d(TAG, "minted client token, good for ${ttlSeconds}s")
        return token
    }

    private fun session(): SessionInfo? {
        cachedSession?.let { return it }

        val request = Request.Builder()
            .url("https://open.spotify.com")
            .header("User-Agent", CANVAS_UA)
            .build()
        val (html, deviceId) = runCatching {
            Http.client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                val spT = response.headers("Set-Cookie").firstNotNullOfOrNull { header ->
                    Regex("""sp_t=([^;]+)""").find(header)?.groupValues?.get(1)
                }
                body to spT
            }
        }.getOrNull() ?: (null to null)
        if (html == null) {
            Log.w(TAG, "couldn't load the web player page for session info")
            return null
        }

        val configB64 = Regex("""<script id="appServerConfig" type="text/plain">([^<]+)</script>""")
            .find(html)?.groupValues?.get(1)
        if (configB64 == null) {
            Log.w(TAG, "web player page had no appServerConfig block")
            return null
        }
        val clientVersion = runCatching {
            val configJson = String(Base64.getDecoder().decode(configB64), Charsets.UTF_8)
            json.parseToJsonElement(configJson).jsonObject["clientVersion"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
        if (clientVersion == null) {
            Log.w(TAG, "appServerConfig had no clientVersion")
            return null
        }

        val session = SessionInfo(clientVersion, deviceId ?: java.util.UUID.randomUUID().toString())
        cachedSession = session
        return session
    }
}

