package com.music.nuvia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import dev.chrisbanes.haze.HazeState
import com.music.nuvia.R
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.FrostedTopBar
import com.music.nuvia.ui.components.LiquidGlassButton
import com.music.nuvia.ui.components.LiquidGlassIconButton
import com.music.nuvia.ui.components.NUViAGlassTier
import com.music.nuvia.ui.components.NUViAPillTextField
import com.music.nuvia.ui.components.nuviaGlass
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora

/**
 * NUViA Liquid Glass screen for authenticating Spotify Canvas using the `sp_dc` cookie.
 */
@Composable
fun SpotifyCanvasAuthScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()
    val keyboardController = LocalSoftwareKeyboardController.current

    val currentToken by AppSettings.spotifySpdcToken.collectAsStateWithLifecycle()
    var tokenInput by remember(currentToken) { mutableStateOf(currentToken) }

    val shape = RoundedCornerShape(20.dp)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = nuviaColors.surfaceOled,
        topBar = {
            FrostedTopBar(
                title = stringResource(R.string.spotify_canvas_setup),
                hazeState = remember { HazeState() },
                scrolled = true,
                onBack = onNavigateUp,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Explanatory card with Liquid Glass container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .nuviaGlass(
                        tier = NUViAGlassTier.Elevated,
                        shape = shape,
                    )
                    .padding(20.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(nuviaColors.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VpnKey,
                            contentDescription = null,
                            tint = nuviaColors.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.spotify_canvas_setup),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = nuviaColors.textPrimary,
                    )
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = stringResource(R.string.spotify_canvas_setup_description),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = Manrope,
                        lineHeight = 22.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.spotify_canvas_setup_steps),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        lineHeight = 20.sp,
                    ),
                    color = nuviaColors.textMuted,
                )
            }

            // Input field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .nuviaGlass(
                        tier = NUViAGlassTier.Elevated,
                        shape = shape,
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.spdc_token),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = nuviaColors.textSecondary,
                )

                NUViAPillTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    placeholder = "sp_dc cookie value…",
                    container = nuviaColors.surfaceOled,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            AppSettings.setSpotifySpdcToken(tokenInput.trim())
                            haptics.play(Haptic.Select)
                            onNavigateUp()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(4.dp))

                LiquidGlassButton(
                    text = stringResource(R.string.save),
                    onClick = {
                        keyboardController?.hide()
                        AppSettings.setSpotifySpdcToken(tokenInput.trim())
                        haptics.play(Haptic.Select)
                        onNavigateUp()
                    },
                    isPrimary = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                )
            }

            Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 24.dp))
        }
    }
}

