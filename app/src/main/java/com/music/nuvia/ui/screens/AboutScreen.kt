package com.music.nuvia.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.nuvia.BuildConfig
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import com.music.nuvia.ui.components.nuviaSpecularBorder

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.music.nuvia.R

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val context = LocalContext.current
    val nuviaColors = LocalNUViAColors.current
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(nuviaColors.surfaceOled)
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .verticalScroll(scrollState)
    ) {
        // Back Button & Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x1AFFFFFF)),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = nuviaColors.textPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Text(
                text = "About NUViA",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                ),
                color = nuviaColors.textPrimary,
            )
        }

        Spacer(Modifier.height(24.dp))

        // Hero Brand Badge
        // Hero Brand Badge with bglogo.png
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        listOf(nuviaColors.primary, nuviaColors.secondary)
                    )
                )
                .size(88.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(nuviaColors.glassSurfaceElevated)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(26.dp),
                    width = 1.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.bglogo),
                contentDescription = "NUViA",
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        // App Title & Version
        Text(
            text = "NUViA",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                fontSize = 28.sp,
            ),
            color = nuviaColors.textPrimary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            text = "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            ),
            color = nuviaColors.primary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(14.dp))

        // Craft Statement
        Text(
            text = "A high-craft, liquid-glass music streaming experience built with Android Jetpack Compose and ExoPlayer.",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Sora,
                fontSize = 14.sp,
                lineHeight = 22.sp,
            ),
            color = nuviaColors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(28.dp))

        // Creator Attribution Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(20.dp),
                    width = 0.75.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(nuviaColors.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = nuviaColors.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Crafted with passion",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = Sora,
                            fontSize = 11.sp,
                        ),
                        color = nuviaColors.textMuted,
                    )
                    Text(
                        text = "Created by Adhil CLT",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                        color = nuviaColors.textPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Support Development Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(20.dp),
                    width = 0.75.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .padding(20.dp),
        ) {
            Column {
                Text(
                    text = "Support development",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    ),
                    color = nuviaColors.textPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "If you'd like to help keep NUViA free and support future development, you can optionally contribute.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = Manrope,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    color = nuviaColors.textSecondary,
                )
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(nuviaColors.primary)
                        .clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://buymeatea.online/adhil")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "☕ Support development",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        ),
                        color = nuviaColors.onPrimary,
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Open Source Libraries & Credits
        Text(
            text = "OPEN SOURCE TECHNOLOGIES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                fontSize = 11.sp,
            ),
            color = nuviaColors.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(nuviaColors.glassSurface)
                .nuviaSpecularBorder(
                    shape = RoundedCornerShape(20.dp),
                    width = 0.75.dp,
                    topHighlight = nuviaColors.glassHighlight,
                    bottomBorder = nuviaColors.glassBorder,
                )
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                LicenseRow(name = "Jetpack Compose", license = "Apache 2.0")
                LicenseRow(name = "AndroidX Media3 & ExoPlayer", license = "Apache 2.0")
                LicenseRow(name = "Ktor HTTP Client", license = "Apache 2.0")
                LicenseRow(name = "Coil Image Loader", license = "Apache 2.0")
                LicenseRow(name = "Kotlinx Coroutines & Serialization", license = "Apache 2.0")
                LicenseRow(name = "Material 3 Design System", license = "Apache 2.0")
            }
        }

        Spacer(Modifier.height(32.dp))
        Spacer(Modifier.height(contentPadding.calculateBottomPadding() + 32.dp))
    }
}

@Composable
private fun LicenseRow(name: String, license: String) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Sora,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            ),
            color = nuviaColors.textPrimary,
        )
        Text(
            text = license,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = Sora,
                fontSize = 12.sp,
            ),
            color = nuviaColors.textMuted,
        )
    }
}

