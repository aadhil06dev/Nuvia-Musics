package com.music.nuvia.ui.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.music.nuvia.R
import com.music.nuvia.data.settings.AppSettings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials

/**
 * Language tag paired with its string resource name and native script name.
 * Empty tag represents System Default.
 */
data class AppLanguage(
    val tag: String,
    val nameRes: Int,
    val nativeName: String,
)

val SUPPORTED_LANGUAGES = listOf(
    AppLanguage("", R.string.system_default, "System Default"),
    AppLanguage("en", R.string.english, "English"),
    AppLanguage("es", R.string.spanish, "Español"),
    AppLanguage("fr", R.string.french, "Français"),
    AppLanguage("de", R.string.german, "Deutsch"),
    AppLanguage("hi", R.string.hindi, "हिन्दी"),
    AppLanguage("ja", R.string.japanese, "日本語"),
)

fun languageDisplayNameRes(languageTag: String): Int =
    SUPPORTED_LANGUAGES.firstOrNull { it.tag.equals(languageTag, ignoreCase = true) }?.nameRes ?: R.string.system_default

fun languageNativeName(languageTag: String): String =
    SUPPORTED_LANGUAGES.firstOrNull { it.tag.equals(languageTag, ignoreCase = true) }?.nativeName ?: "System Default"

/**
 * Frosted liquid glass language picker dialog for NUViA.
 * Single-select list of supported languages with instant application.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun AppLanguageDialog(
    hazeState: HazeState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduceDynamicBlur by AppSettings.reduceDynamicBlur.collectAsStateWithLifecycle()
    val savedLanguage by AppSettings.appLanguage.collectAsStateWithLifecycle()
    val shape = RoundedCornerShape(ALERT_CORNER)

    // Current selection: if saved is empty, we check AppCompatDelegate to see if one was set externally
    val activeTag = savedLanguage.ifEmpty {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        if (appLocales.isEmpty) "" else (appLocales.get(0)?.language ?: "")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SCRIM_COLOR)
            .testTag("language_picker_dialog")
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(ALERT_WIDTH)
                .clip(shape)
                .then(
                    if (reduceDynamicBlur) {
                        Modifier.background(MaterialTheme.colorScheme.surface)
                    } else {
                        Modifier.hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.regular(MaterialTheme.colorScheme.surface),
                        )
                    },
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.app_language),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.W600,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = stringResource(R.string.app_language_description),
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                SUPPORTED_LANGUAGES.forEach { language ->
                    AlertRule()
                    val isSelected = if (language.tag.isEmpty()) {
                        activeTag.isEmpty()
                    } else {
                        activeTag.equals(language.tag, ignoreCase = true)
                    }

                    LanguageRow(
                        language = language,
                        selected = isSelected,
                        onClick = {
                            AppSettings.setAppLanguage(language.tag)
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val tagSlug = language.tag.ifEmpty { "system" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ACTION_HEIGHT)
            .testTag("language_item_$tagSlug")
            .background(
                if (pressed) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)
                } else if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(
                indication = null,
                interactionSource = interactionSource,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            if (language.tag.isNotEmpty()) {
                val localizedName = stringResource(language.nameRes)
                if (!localizedName.equals(language.nativeName, ignoreCase = true)) {
                    Text(
                        text = localizedName,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
        }

        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
