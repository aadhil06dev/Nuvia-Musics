package com.music.nuvia.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.data.model.Account
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import kotlin.math.roundToInt

@Composable
fun AccountAndScrobblingScreen(
    signedIn: Boolean,
    account: Account?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onOpenListenBrainzLogin: () -> Unit,
    onOpenLastfmLogin: () -> Unit,
    onOpenDiscord: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current

    val lastfmEnabled by AppSettings.lastfmEnabled.collectAsStateWithLifecycle()
    val lastfmUsername by AppSettings.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmSessionKey by AppSettings.lastfmSessionKey.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by AppSettings.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val lastfmNowPlayingEnabled by AppSettings.lastfmNowPlaying.collectAsStateWithLifecycle()
    val scrobbleMinDuration by AppSettings.scrobbleMinDuration.collectAsStateWithLifecycle()
    val scrobbleDelayPercent by AppSettings.scrobbleDelayPercent.collectAsStateWithLifecycle()
    val scrobbleDelaySeconds by AppSettings.scrobbleDelaySeconds.collectAsStateWithLifecycle()
    val listenBrainzEnabled by AppSettings.listenBrainzEnabled.collectAsStateWithLifecycle()
    val listenBrainzToken by AppSettings.listenBrainzToken.collectAsStateWithLifecycle()
    val discordToken by AppSettings.discordToken.collectAsStateWithLifecycle()
    val discordUsername by AppSettings.discordUsername.collectAsStateWithLifecycle()
    val discordRpcEnabled by AppSettings.discordRpcEnabled.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Spacer(Modifier.height(12.dp))

        AccountCard(signedIn = signedIn, account = account, onSignIn = onSignIn)

        if (signedIn) {
            SettingsGroup {
                DestructiveRow(label = "Sign out", onClick = onSignOut)
            }
        }

        SettingsGroup(
            header = "Rich presence",
            footer = "Show what you're playing on your Discord profile, updating as " +
                "the track does.",
        ) {
            SettingsRow(
                icon = ImageVector.vectorResource(R.drawable.ic_discord),
                title = "Discord",
                subtitle = when {
                    discordToken.isEmpty() -> "Tap to connect"
                    !discordRpcEnabled -> "Connected, presence off"
                    discordUsername.isNotEmpty() -> "Sharing as @$discordUsername"
                    else -> "Sharing your listens"
                },
                onClick = onOpenDiscord,
            )
        }

        if (AppSettings.scrobblingAvailable) {
            SettingsGroup(
                header = "Scrobbling",
                footer = "Scrobble your listens to Last.fm and ListenBrainz.",
            ) {
                val isListenBrainzConfigured = listenBrainzToken.isNotBlank()
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = "ListenBrainz",
                    subtitle = if (isListenBrainzConfigured) {
                        if (listenBrainzEnabled) "Connected & scrobbling" else "Connected (scrobbling disabled)"
                    } else "Enter a token to enable",
                    trailing = {
                        NUViASwitch(
                            checked = listenBrainzEnabled && isListenBrainzConfigured,
                            enabled = isListenBrainzConfigured,
                            onCheckedChange = {
                                if (isListenBrainzConfigured) {
                                    AppSettings.setListenBrainzEnabled(it)
                                } else {
                                    onOpenListenBrainzLogin()
                                }
                            },
                        )
                    },
                    onClick = onOpenListenBrainzLogin,
                )
                RowDivider()
                val isLastfmConfigured = lastfmSessionKey.isNotBlank()
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = "Last.fm",
                    subtitle = if (isLastfmConfigured) {
                        if (lastfmEnabled) "Signed in as $lastfmUsername" else "Signed in as $lastfmUsername (scrobbling disabled)"
                    } else "Tap to sign in",
                    trailing = {
                        NUViASwitch(
                            checked = lastfmEnabled && isLastfmConfigured,
                            enabled = isLastfmConfigured,
                            onCheckedChange = {
                                if (isLastfmConfigured) {
                                    AppSettings.setLastfmEnabled(it)
                                } else {
                                    onOpenLastfmLogin()
                                }
                            },
                        )
                    },
                    onClick = {
                        if (isLastfmConfigured) {
                            AppSettings.setLastfmSessionKey("")
                            AppSettings.setLastfmUsername("")
                            AppSettings.setLastfmEnabled(false)
                            AppSettings.setLastfmScrobbleEnabled(false)
                            AppSettings.setLastfmNowPlaying(false)
                        } else {
                            onOpenLastfmLogin()
                        }
                    },
                )
                if (lastfmEnabled && lastfmSessionKey.isNotBlank()) {
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Scrobble tracks",
                        subtitle = "Log plays to your Last.fm timeline",
                        trailing = {
                            NUViASwitch(
                                checked = lastfmScrobbleEnabled,
                                onCheckedChange = AppSettings::setLastfmScrobbleEnabled,
                            )
                        },
                        onClick = { AppSettings.setLastfmScrobbleEnabled(!lastfmScrobbleEnabled) },
                    )
                    RowDivider()
                    SettingsRow(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Now playing",
                        subtitle = "Update Last.fm with what you're listening to",
                        trailing = {
                            NUViASwitch(
                                checked = lastfmNowPlayingEnabled,
                                onCheckedChange = AppSettings::setLastfmNowPlaying,
                            )
                        },
                        onClick = { AppSettings.setLastfmNowPlaying(!lastfmNowPlayingEnabled) },
                    )
                }
            }

            if (lastfmEnabled && lastfmSessionKey.isNotBlank()) {
                SettingsGroup(header = "Scrobble timing") {
                    SliderRow(
                        icon = Icons.Rounded.Tune,
                        title = "Min song duration",
                        subtitle = "Songs shorter than this won't scrobble",
                        value = "${scrobbleMinDuration}s",
                        sliderValue = scrobbleMinDuration.toFloat(),
                        onSliderValue = { AppSettings.setScrobbleMinDuration(it.roundToInt()) },
                        valueRange = 15f..120f,
                        steps = 20,
                    )
                    RowDivider()
                    SliderRow(
                        icon = Icons.Rounded.Tune,
                        title = "Scrobble delay",
                        subtitle = "How far into a song before scrobbling",
                        value = "${(scrobbleDelayPercent * 100).roundToInt()}%",
                        sliderValue = scrobbleDelayPercent,
                        onSliderValue = { AppSettings.setScrobbleDelayPercent(it) },
                        valueRange = 0.1f..1.0f,
                        steps = 8,
                    )
                    RowDivider()
                    SliderRow(
                        icon = Icons.Rounded.Tune,
                        title = "Max delay",
                        subtitle = "Cap on scrobble delay in seconds",
                        value = "${scrobbleDelaySeconds}s",
                        sliderValue = scrobbleDelaySeconds.toFloat(),
                        onSliderValue = { AppSettings.setScrobbleDelaySeconds(it.roundToInt()) },
                        valueRange = 30f..300f,
                        steps = 26,
                    )
                }
            }
        } else {
            SettingsGroup(
                header = "Scrobbling",
                footer = "Last.fm and ListenBrainz are paused for this release and " +
                    "will return in a future version. Anything you have already " +
                    "connected stays saved.",
            ) {
                SettingsRow(
                    icon = Icons.Rounded.Cloud,
                    title = "ListenBrainz",
                    subtitle = "Back in a future version",
                    enabled = false,
                )
                RowDivider()
                SettingsRow(
                    icon = Icons.Rounded.History,
                    title = "Last.fm",
                    subtitle = "Back in a future version",
                    enabled = false,
                )
            }
        }

        Spacer(Modifier.height(36.dp))
    }
}
