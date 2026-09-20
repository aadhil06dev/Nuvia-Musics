package com.music.nuvia.ui.screens

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.music.nuvia.R
import com.music.nuvia.data.listentogether.JamInviteLink
import com.music.nuvia.data.listentogether.ListenTogether
import com.music.nuvia.data.listentogether.PartyMember
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.NUViAPillTextField
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CODE_CELL_SHAPE = RoundedCornerShape(14.dp)
private val LT_ICON_SIZE = 22.dp
private val LT_ICON_GAP = 14.dp
private val LT_ROW_INSET = 16.dp
private val LT_GROUP_INSET = 16.dp

@Composable
fun ListenTogetherScreen(
    signedIn: Boolean,
    inviteCode: String? = null,
    onInviteJoined: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onBack: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()

    val state by ListenTogether.state.collectAsStateWithLifecycle()
    val customServer by ListenTogether.customServerUrl.collectAsStateWithLifecycle()
    val serverStatus by ListenTogether.serverStatus.collectAsStateWithLifecycle()

    var serverInput by remember(customServer) { mutableStateOf(customServer) }
    var codeInput by remember(inviteCode) { mutableStateOf(inviteCode.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { ListenTogether.ensureConnected() }
    LaunchedEffect(customServer) { ListenTogether.refreshServerHealth() }

    LaunchedEffect(inviteCode, signedIn) {
        val code = inviteCode ?: return@LaunchedEffect
        if (!signedIn) return@LaunchedEffect
        if (state.code.equals(code, ignoreCase = true)) {
            onInviteJoined()
            return@LaunchedEffect
        }

        busy = true
        failure = null
        val result = ListenTogether.joinParty(code)
        failure = result.exceptionOrNull()?.message
        if (result.isSuccess) {
            codeInput = ""
            onInviteJoined()
        }
        busy = false
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                LiquidGlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    onClick = {
                        haptics.play(Haptic.Tap)
                        onBack()
                    },
                    contentDescription = "Back",
                    size = 38.dp,
                    iconSize = 20.dp,
                    tier = NUViAGlassTier.Subtle,
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp,
                ),
                color = nuviaColors.textPrimary,
            )
        }

        if (!signedIn) {
            SettingsGroup(footer = stringResource(R.string.listen_together_sign_in_footer)) {
                SettingsRow(
                    icon = Icons.Rounded.Login,
                    title = stringResource(R.string.sign_in),
                    subtitle = stringResource(R.string.not_signed_in),
                    onClick = onSignIn,
                )
            }
        }

        ServerHealthRow(status = serverStatus, onRecheck = ListenTogether::refreshServerHealth)

        if (!state.inParty) {
            NotInAParty(
                signedIn = signedIn,
                hasServer = ListenTogether.hasServer,
                codeInput = codeInput,
                onCodeInput = { typed ->
                    codeInput = typed.filter(Char::isLetterOrDigit)
                        .uppercase()
                        .take(ListenTogether.CODE_LENGTH)
                },
                busy = busy,
                onCreate = {
                    busy = true
                    failure = null
                    scope.launch {
                        failure = ListenTogether.createParty().exceptionOrNull()?.message
                        busy = false
                    }
                },
                onJoin = {
                    busy = true
                    failure = null
                    scope.launch {
                        failure = ListenTogether.joinParty(codeInput).exceptionOrNull()?.message
                        if (failure == null) codeInput = ""
                        busy = false
                    }
                },
            )
        } else {
            InAParty(
                state = state,
                onCopy = {
                    clipboard.setText(AnnotatedString(state.code.orEmpty()))
                    haptics.play(Haptic.Tap)
                },
                onShare = {
                    val code = state.code ?: return@InAParty
                    val link = JamInviteLink.url(code)
                    val message = "$link\n\n${context.getString(R.string.listen_together_share_text, code)}"
                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, message)
                            },
                            null,
                        ),
                    )
                },
                onLeave = {
                    scope.launch { ListenTogether.leaveParty() }
                },
            )
        }

        (failure ?: state.error)?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Manrope),
                color = Color(0xFFEF4444),
                modifier = Modifier.padding(start = LT_GROUP_INSET + 4.dp, end = LT_GROUP_INSET + 4.dp, top = 12.dp),
            )
        }

        SettingsGroup(
            header = stringResource(R.string.listen_together_custom_server),
            footer = stringResource(R.string.listen_together_custom_server_footer),
        ) {
            Column(Modifier.padding(horizontal = LT_ROW_INSET, vertical = 14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Dns,
                        contentDescription = null,
                        tint = nuviaColors.textSecondary,
                        modifier = Modifier.size(LT_ICON_SIZE),
                    )
                    Spacer(Modifier.width(LT_ICON_GAP))
                    NUViAPillTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        placeholder = stringResource(R.string.listen_together_using_builtin),
                        container = nuviaColors.surfaceOled,
                        enabled = !state.inParty,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { ListenTogether.setCustomServerUrl(serverInput) },
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }
                if (serverInput.trim().trimEnd('/') != customServer) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = {
                            haptics.play(Haptic.Select)
                            ListenTogether.setCustomServerUrl(serverInput)
                        }) {
                            Text(
                                stringResource(R.string.save),
                                color = nuviaColors.primary,
                                fontFamily = Sora,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ServerHealthRow(
    status: ListenTogether.ServerStatus,
    onRecheck: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    SettingsGroup {
        SettingsRow(
            icon = when (status.health) {
                ListenTogether.Health.ONLINE -> Icons.Rounded.CloudDone
                ListenTogether.Health.OFFLINE -> Icons.Rounded.CloudOff
                else -> Icons.Rounded.Cloud
            },
            title = stringResource(R.string.listen_together_server),
            subtitle = when (status.health) {
                ListenTogether.Health.ONLINE ->
                    stringResource(R.string.listen_together_server_online, status.latencyMs)
                ListenTogether.Health.OFFLINE ->
                    stringResource(R.string.listen_together_server_offline)
                ListenTogether.Health.CHECKING ->
                    stringResource(R.string.listen_together_server_checking)
                ListenTogether.Health.UNKNOWN ->
                    stringResource(R.string.listen_together_server_unknown)
            },
            trailing = if (status.health == ListenTogether.Health.CHECKING) ({
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = nuviaColors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }) else null,
            onClick = onRecheck,
        )
    }
}

@Composable
private fun NotInAParty(
    signedIn: Boolean,
    hasServer: Boolean,
    codeInput: String,
    onCodeInput: (String) -> Unit,
    busy: Boolean,
    onCreate: () -> Unit,
    onJoin: () -> Unit,
) {
    val ready = signedIn && hasServer && !busy
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()

    SettingsGroup(footer = stringResource(R.string.listen_together_create_footer)) {
        SettingsRow(
            icon = Icons.Rounded.GroupAdd,
            title = stringResource(R.string.listen_together_create),
            subtitle = stringResource(R.string.listen_together_create_subtitle),
            enabled = ready,
            onClick = {
                haptics.play(Haptic.Select)
                onCreate()
            },
            trailing = if (busy) ({
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = nuviaColors.primary,
                    modifier = Modifier.size(18.dp),
                )
            }) else null,
        )
    }

    SettingsGroup(
        header = stringResource(R.string.listen_together_join),
        footer = stringResource(R.string.listen_together_code_hint),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = LT_ROW_INSET, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PartyCodeField(
                code = codeInput,
                onCodeChange = onCodeInput,
                enabled = ready,
                onSubmit = onJoin,
            )
            LiquidGlassButton(
                text = stringResource(R.string.listen_together_join_action),
                onClick = {
                    haptics.play(Haptic.Select)
                    onJoin()
                },
                enabled = ready && codeInput.length == ListenTogether.CODE_LENGTH,
                isPrimary = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            )
        }
    }
}

@Composable
private fun PartyCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    enabled: Boolean,
    onSubmit: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val field = remember(code) { TextFieldValue(code, TextRange(code.length)) }

    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(ListenTogether.CODE_LENGTH) { index ->
                CodeCell(
                    char = code.getOrNull(index),
                    active = focused && index == code.length.coerceAtMost(ListenTogether.CODE_LENGTH - 1),
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        BasicTextField(
            value = field,
            onValueChange = { onCodeChange(it.text) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (code.length == ListenTogether.CODE_LENGTH) onSubmit() },
            ),
            modifier = Modifier
                .matchParentSize()
                .onFocusChanged { focused = it.isFocused },
        )
    }
}

@Composable
private fun CodeCell(
    char: Char?,
    active: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val nuviaColors = LocalNUViAColors.current
    val ring by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            active -> nuviaColors.primary
            char != null -> nuviaColors.glassBorder
            else -> Color.Transparent
        },
        label = "party code cell ring",
    )
    Box(
        modifier = modifier
            .height(52.dp)
            .background(nuviaColors.surfaceOled, CODE_CELL_SHAPE)
            .border(1.5.dp, ring, CODE_CELL_SHAPE),
        contentAlignment = Alignment.Center,
    ) {
        if (char != null) {
            Text(
                text = char.toString(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                ),
                color = if (enabled) nuviaColors.textPrimary else nuviaColors.textSecondary,
            )
        } else if (active) {
            val blink = rememberInfiniteTransition(label = "party code caret")
            val alpha by blink.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "party code caret alpha",
            )
            Box(
                Modifier
                    .size(width = 2.dp, height = 22.dp)
                    .background(
                        nuviaColors.primary.copy(alpha = alpha),
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

@Composable
private fun InAParty(
    state: ListenTogether.State,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onLeave: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()

    SettingsGroup(
        header = stringResource(R.string.listen_together_code),
        footer = stringResource(R.string.listen_together_code_footer),
    ) {
        Text(
            text = state.code.orEmpty(),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
            ),
            color = nuviaColors.primary,
            textAlign = TextAlign.Center,
            letterSpacing = 8.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.ContentCopy,
            title = stringResource(R.string.copy_code),
            onClick = onCopy,
        )
        RowDivider()
        SettingsRow(
            icon = Icons.Rounded.Share,
            title = stringResource(R.string.share),
            onClick = onShare,
        )
    }

    NowPlayingInTheParty(state)

    SettingsGroup(
        header = stringResource(
            R.string.listen_together_listening,
            state.members.size,
            state.maxMembers,
        ),
        footer = stringResource(R.string.listen_together_members_footer, state.maxMembers),
    ) {
        state.members.forEachIndexed { index, member ->
            if (index > 0) RowDivider()
            MemberRow(member = member, isYou = member.memberId == state.you?.memberId)
        }
    }

    SettingsGroup {
        SettingsRow(
            icon = Icons.Rounded.Logout,
            title = stringResource(R.string.listen_together_leave),
            subtitle = stringResource(R.string.listen_together_leave_subtitle),
            onClick = {
                haptics.play(Haptic.Tap)
                onLeave()
            },
        )
    }
}

@Composable
private fun NowPlayingInTheParty(state: ListenTogether.State) {
    val track = state.playback.track
    var positionMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(state.playback.seq, state.playback.isPlaying) {
        while (true) {
            positionMs = ListenTogether.partyPositionMs() ?: 0L
            delay(250)
        }
    }

    val connection = connectionLine(state)
    val nothingPlaying = stringResource(R.string.listen_together_nothing_playing)

    SettingsGroup(header = stringResource(R.string.listen_together_now_playing)) {
        SettingsRow(
            icon = Icons.Rounded.MusicNote,
            title = track?.title?.takeIf { it.isNotBlank() } ?: nothingPlaying,
            subtitle = if (track == null) {
                connection
            } else {
                buildString {
                    if (track.artist.isNotBlank()) {
                        append(track.artist)
                        append(" · ")
                    }
                    append(elapsed(positionMs))
                    track.durationMs?.let {
                        append(" / ")
                        append(elapsed(it))
                    }
                    append("\n")
                    append(connection)
                }
            },
        )
    }
}

@Composable
private fun connectionLine(state: ListenTogether.State): String = when {
    state.connection != ListenTogether.Connection.LIVE ->
        stringResource(R.string.listen_together_reconnecting)
    !state.clockSynced -> stringResource(R.string.listen_together_syncing_clock)
    else -> stringResource(R.string.listen_together_in_sync, state.roundTripMs)
}

@Composable
private fun MemberRow(member: PartyMember, isYou: Boolean) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = LT_ROW_INSET, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(nuviaColors.surfaceCard),
            contentAlignment = Alignment.Center,
        ) {
            if (member.avatarUrl != null) {
                AsyncImage(
                    model = member.avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                )
            } else {
                Text(
                    text = member.displayName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = nuviaColors.textSecondary,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = member.displayName,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = nuviaColors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (isYou) {
                    Spacer(Modifier.width(8.dp))
                    Badge(stringResource(R.string.listen_together_you))
                }
            }
            if (!member.connected) {
                Text(
                    text = stringResource(R.string.listen_together_away),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Manrope),
                    color = nuviaColors.textSecondary,
                )
            }
        }
        if (member.isHost) {
            Spacer(Modifier.width(8.dp))
            Badge(stringResource(R.string.listen_together_host))
        }
    }
}



private fun elapsed(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val minutes = total / 60
    val seconds = total % 60
    return "%d:%02d".format(minutes, seconds)
}
