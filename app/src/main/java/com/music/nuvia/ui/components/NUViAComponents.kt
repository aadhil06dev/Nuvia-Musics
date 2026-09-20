package com.music.nuvia.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.data.settings.AppSettings
import com.music.nuvia.ui.components.backdrop.Backdrop
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import dev.chrisbanes.haze.HazeState

/**
 * Ambient background lighting that dynamically illuminates the OLED-black canvas
 * with the active track's extracted ambient colors.
 */
@Composable
fun NUViAAmbientBackground(
    modifier: Modifier = Modifier,
    alpha: Float = 0.40f,
    content: (@Composable BoxScope.() -> Unit)? = null,
) {
    val colors = LocalNUViAColors.current
    val minimalUi by AppSettings.minimalUi.collectAsStateWithLifecycle()
    val ambientLighting by AppSettings.ambientLighting.collectAsStateWithLifecycle()
    val effectiveAmbient = !minimalUi && ambientLighting && colors.ambientGlow != Color.Transparent

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surfaceOled)
    ) {
        if (effectiveAmbient) {
            // Soft atmospheric top-right aurora glow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.ambientGlow.copy(alpha = alpha),
                                colors.secondary.copy(alpha = alpha * 0.4f),
                                Color.Transparent,
                            ),
                            center = Offset(1000f, -100f),
                            radius = 1200f,
                        )
                    )
            )

            // Subtle bottom-left glow for depth
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colors.tertiary.copy(alpha = alpha * 0.35f),
                                Color.Transparent,
                            ),
                            center = Offset(0f, 800f),
                            radius = 800f,
                        )
                    )
            )
        }

        if (content != null) {
            content()
        }
    }
}

/**
 * Premium NUViA Liquid Glass Card container.
 */
@Composable
fun NUViAGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = NUViAGlassShapes.Card,
    elevated: Boolean = false,
    ambientGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    tier: NUViAGlassTier = if (elevated) NUViAGlassTier.Elevated else NUViAGlassTier.Elevated,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = LocalNUViAColors.current

    Box(
        modifier = modifier
            .then(
                if (ambientGlow) Modifier.nuviaAmbientBacklight(colors.ambientGlow, radiusDp = 24.dp, alpha = 0.25f)
                else Modifier
            )
            .nuviaGlass(
                tier = tier,
                shape = shape,
                hazeState = hazeState,
                backdrop = backdrop,
            )
            .then(
                if (onClick != null) Modifier.nuviaTactilePress(onClick = onClick)
                else Modifier
            ),
        content = content
    )
}

/**
 * Standard Liquid Glass Card conforming to the global material system.
 */
@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    shape: Shape = NUViAGlassShapes.Card,
    elevated: Boolean = false,
    ambientGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    tier: NUViAGlassTier = if (elevated) NUViAGlassTier.Elevated else NUViAGlassTier.Elevated,
    backdrop: Backdrop? = null,
    content: @Composable BoxScope.() -> Unit,
) = NUViAGlassCard(
    modifier = modifier,
    hazeState = hazeState,
    shape = shape,
    elevated = elevated,
    ambientGlow = ambientGlow,
    onClick = onClick,
    tier = tier,
    backdrop = backdrop,
    content = content,
)

/**
 * Tactile glass icon button with dynamic highlight and active accent state.
 */
@Composable
fun NUViAIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    hazeState: HazeState? = null,
    isActive: Boolean = false,
    tint: Color? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
    backdrop: Backdrop? = null,
) {
    val colors = LocalNUViAColors.current
    val effectiveTint = tint ?: if (isActive) colors.primary else colors.textPrimary

    val bgTint = if (isActive) colors.primary.copy(alpha = 0.18f) else null

    Box(
        modifier = modifier
            .size(size)
            .nuviaTactilePress(onClick = onClick)
            .nuviaGlass(
                tier = tier,
                shape = NUViAGlassShapes.Pill,
                tintColor = bgTint,
                borderWidth = if (isActive) 1.dp else GLASS_EDGE_WIDTH,
                hazeState = hazeState,
                backdrop = backdrop,
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = effectiveTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Standard Liquid Glass Icon Button conforming to the global material system.
 */
@Composable
fun LiquidGlassIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    hazeState: HazeState? = null,
    isActive: Boolean = false,
    tint: Color? = null,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
    backdrop: Backdrop? = null,
) = NUViAIconButton(
    icon = icon,
    onClick = onClick,
    modifier = modifier,
    contentDescription = contentDescription,
    hazeState = hazeState,
    isActive = isActive,
    tint = tint,
    size = size,
    iconSize = iconSize,
    tier = tier,
    backdrop = backdrop,
)

/**
 * High-craft primary or secondary glass action button.
 */
@Composable
fun NUViAGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = true,
    enabled: Boolean = true,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
) {
    val colors = LocalNUViAColors.current

    val containerColor = if (isPrimary) colors.primary else colors.glassSurfaceElevated
    val contentColor = if (isPrimary) colors.onPrimary else colors.textPrimary

    Box(
        modifier = modifier
            .then(if (enabled) Modifier.nuviaTactilePress(onClick = onClick) else Modifier)
            .clip(NUViAGlassShapes.Pill)
            .then(
                if (isPrimary) {
                    Modifier
                        .background(containerColor)
                        .border(1.dp, Color.White.copy(alpha = 0.35f), NUViAGlassShapes.Pill)
                        .nuviaAmbientBacklight(colors.primary, radiusDp = 20.dp, alpha = 0.35f)
                } else {
                    Modifier.nuviaGlass(
                        tier = tier,
                        shape = NUViAGlassShapes.Pill,
                        hazeState = hazeState,
                        backdrop = backdrop,
                    )
                }
            )
            .then(if (enabled) Modifier else Modifier.alpha(0.45f))
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = contentColor
            )
        }
    }
}

/**
 * Standard Liquid Glass Action Button conforming to the global material system.
 */
@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isPrimary: Boolean = false,
    enabled: Boolean = true,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tier: NUViAGlassTier = NUViAGlassTier.Elevated,
) = NUViAGlassButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    isPrimary = isPrimary,
    enabled = enabled,
    hazeState = hazeState,
    backdrop = backdrop,
    tier = tier,
)

/**
 * Filter chip / pill tag for category selection, sources, and queue sections.
 */
@Composable
fun NUViAPillTag(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tier: NUViAGlassTier = NUViAGlassTier.Subtle,
) {
    val colors = LocalNUViAColors.current

    val textTarget = if (selected) colors.onPrimary else colors.textSecondary
    val animatedText by animateColorAsState(textTarget, tween(200), label = "pill_text")

    Box(
        modifier = modifier
            .nuviaTactilePress(onClick = onClick)
            .clip(NUViAGlassShapes.Pill)
            .then(
                if (selected) {
                    Modifier
                        .background(colors.primary)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), NUViAGlassShapes.Pill)
                        .nuviaAmbientBacklight(colors.primary, radiusDp = 12.dp, alpha = 0.25f)
                } else {
                    Modifier.nuviaGlass(
                        tier = tier,
                        shape = NUViAGlassShapes.Pill,
                        hazeState = hazeState,
                        backdrop = backdrop,
                    )
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = animatedText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = animatedText
            )
        }
    }
}

/**
 * Standard Liquid Glass Filter Chip / Pill conforming to the global material system.
 */
@Composable
fun LiquidGlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    hazeState: HazeState? = null,
    backdrop: Backdrop? = null,
    tier: NUViAGlassTier = NUViAGlassTier.Subtle,
) = NUViAPillTag(
    label = label,
    selected = selected,
    onClick = onClick,
    modifier = modifier,
    icon = icon,
    hazeState = hazeState,
    backdrop = backdrop,
    tier = tier,
)

/**
 * Section Header with geometric display typography and optional action.
 */
@Composable
fun NUViASectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalNUViAColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = (-0.4).sp
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (actionText != null && onAction != null) {
            Box(
                modifier = Modifier
                    .nuviaTactilePress(onClick = onAction)
                    .clip(CircleShape)
                    .background(colors.glassSurface)
                    .border(0.75.dp, colors.glassBorder, CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = Sora,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    ),
                    color = colors.primary
                )
            }
        }
    }
}

/**
 * Audio Quality / Format badge (e.g. "LOSSLESS", "24-BIT / 96kHz", "DOLBY ATMOS", "FLAC").
 */
@Composable
fun NUViABadge(
    text: String,
    modifier: Modifier = Modifier,
    isHighlight: Boolean = false,
) {
    val colors = LocalNUViAColors.current

    val tint = if (isHighlight) colors.primary else colors.textMuted
    val bg = if (isHighlight) colors.primary.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.08f)
    val border = if (isHighlight) colors.primary.copy(alpha = 0.40f) else Color.White.copy(alpha = 0.12f)

    Box(
        modifier = modifier
            .clip(NUViAGlassShapes.Badge)
            .background(bg)
            .border(GLASS_EDGE_WIDTH, border, NUViAGlassShapes.Badge)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.8.sp
            ),
            color = tint
        )
    }
}

/**
 * Hairline specular divider.
 */
@Composable
fun NUViADivider(
    modifier: Modifier = Modifier,
    insetHorizontal: Dp = 20.dp,
) {
    val colors = LocalNUViAColors.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = insetHorizontal)
            .height(0.75.dp)
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        colors.divider,
                        colors.divider,
                        Color.Transparent
                    )
                )
            )
    )
}

/**
 * Sleek tactile pill text field with Liquid Glass styling.
 */
@Composable
fun NUViAPillTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    container: Color = Color.Transparent,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val nuviaColors = LocalNUViAColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(container, RoundedCornerShape(12.dp))
            .border(0.75.dp, nuviaColors.divider, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = Manrope,
                    fontSize = 14.sp,
                ),
                color = nuviaColors.textSecondary.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontSize = 14.sp,
                color = nuviaColors.textPrimary,
            ),
            cursorBrush = SolidColor(nuviaColors.primary),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

