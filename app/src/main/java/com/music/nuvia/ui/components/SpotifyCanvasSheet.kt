package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * Canonical Liquid Glass Modal Bottom Sheet for Spotify Canvas setup.
 * Provides a compact, non-disruptive sheet for configuring the `sp_dc` authorization token.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyCanvasSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentToken by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    var tokenInput by remember(currentToken) { mutableStateOf(currentToken) }

    val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = sheetShape,
        containerColor = nuviaColors.surfaceOled,
        contentColor = nuviaColors.textPrimary,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.20f))
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header with glowing icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .nuviaAmbientBacklight(nuviaColors.primary, radiusDp = 20.dp, alpha = 0.35f)
                    .clip(CircleShape)
                    .background(nuviaColors.glassSurfaceElevated)
                    .nuviaSpecularBorder(CircleShape, 0.75.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.VpnKey,
                    contentDescription = null,
                    tint = nuviaColors.primary,
                    modifier = Modifier.size(26.dp),
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Spotify Canvas",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                ),
                color = nuviaColors.textPrimary,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Enable dynamic, looping full-bleed video canvases from Spotify directly behind your cover art.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 13.5.sp,
                    lineHeight = 19.sp,
                ),
                color = nuviaColors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )

            Spacer(Modifier.height(18.dp))

            // Instructions Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(nuviaColors.glassSurface)
                    .nuviaSpecularBorder(RoundedCornerShape(16.dp), 0.5.dp)
                    .padding(14.dp),
            ) {
                Text(
                    text = "Log into open.spotify.com on desktop or browser → Open DevTools (F12) → Application → Cookies → Copy the 'sp_dc' cookie value.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                    ),
                    color = nuviaColors.textMuted,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Text input
            NUViAPillTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                placeholder = "Paste sp_dc token here…",
                container = nuviaColors.glassSurfaceElevated,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        AppSettings.setSpotifySpdcToken(tokenInput.trim())
                        haptics.play(Haptic.Select)
                        onDismissRequest()
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (currentToken.isNotBlank()) {
                    LiquidGlassButton(
                        text = "Disconnect",
                        onClick = {
                            AppSettings.setSpotifySpdcToken("")
                            tokenInput = ""
                            haptics.play(Haptic.Tap)
                            onDismissRequest()
                        },
                        isPrimary = false,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                    )
                }

                LiquidGlassButton(
                    text = if (currentToken.isNotBlank()) "Update Token" else "Save & Connect",
                    onClick = {
                        keyboardController?.hide()
                        AppSettings.setSpotifySpdcToken(tokenInput.trim())
                        haptics.play(Haptic.Select)
                        onDismissRequest()
                    },
                    isPrimary = true,
                    modifier = Modifier
                        .weight(if (currentToken.isNotBlank()) 1.4f else 1f)
                        .height(48.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
