package com.music.nuvia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.music.nuvia.R
import com.music.nuvia.auth.GoogleAccountSession
import com.music.nuvia.auth.YouTubeProfile
import com.music.nuvia.ui.haptics.Haptic
import com.music.nuvia.ui.haptics.rememberHaptics
import com.music.nuvia.ui.theme.LocalNUViAColors
import com.music.nuvia.ui.theme.Manrope
import com.music.nuvia.ui.theme.Sora
import dev.chrisbanes.haze.HazeState

/**
 * NUViA Liquid Glass multi-account profile selector modal sheet.
 */
@Composable
fun AccountProfileSelector(
    accounts: List<GoogleAccountSession>,
    activeAccountId: String?,
    activeProfileId: String?,
    onSelect: (GoogleAccountSession, YouTubeProfile) -> Unit,
    onAddAccount: () -> Unit,
    onRemoveAccount: (GoogleAccountSession) -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
) {
    var managing by remember { mutableStateOf(false) }
    val nuviaColors = LocalNUViAColors.current
    val haptics = rememberHaptics()
    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 64.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth()
                .nuviaGlass(
                    tier = NUViAGlassTier.Elevated,
                    shape = shape,
                    hazeState = hazeState,
                )
                .clickable(onClick = {}), // Consume clicks inside dialog
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
            ) {
                item {
                    Text(
                        text = stringResource(R.string.switch_account),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = Sora,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = nuviaColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }

                accounts.forEach { account ->
                    item {
                        Text(
                            text = account.email.ifBlank {
                                account.name.ifBlank { stringResource(R.string.accounts) }
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = Manrope,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = nuviaColors.textSecondary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                        )
                    }
                    items(account.profiles.size) { index ->
                        val profile = account.profiles[index]
                        val isSelected = account.accountId == activeAccountId &&
                            profile.profileId == activeProfileId
                        ProfileRow(
                            profile = profile,
                            selected = isSelected,
                            managing = managing,
                            onClick = {
                                haptics.play(Haptic.Select)
                                onSelect(account, profile)
                                onDismiss()
                            },
                            onRemove = {
                                haptics.play(Haptic.Tap)
                                onRemoveAccount(account)
                            },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    SelectorAction(
                        icon = Icons.Rounded.Add,
                        label = stringResource(R.string.add_account),
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onAddAccount()
                        },
                    )
                }
                item {
                    SelectorAction(
                        icon = Icons.Rounded.ManageAccounts,
                        label = stringResource(R.string.manage_accounts),
                        onClick = {
                            haptics.play(Haptic.Tap)
                            managing = !managing
                        },
                    )
                }
                item {
                    SelectorAction(
                        icon = Icons.Rounded.Settings,
                        label = stringResource(R.string.settings),
                        onClick = {
                            haptics.play(Haptic.Tap)
                            onOpenSettings()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(
    profile: YouTubeProfile,
    selected: Boolean,
    managing: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    val description = stringResource(if (selected) R.string.selected_account else R.string.switch_account)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(
                role = Role.RadioButton,
                onClick = if (managing) onRemove else onClick,
            )
            .semantics { contentDescription = description }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (profile.avatar != null) {
            AsyncImage(
                model = profile.avatar,
                contentDescription = null,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .nuviaSpecularBorder(CircleShape, 0.75.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(nuviaColors.surfaceCard),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = nuviaColors.textSecondary,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Sora,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                ),
                color = if (selected) nuviaColors.primary else nuviaColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = profile.handle.ifBlank {
                    stringResource(if (profile.isBrandAccount) R.string.brand_account else R.string.personal)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = Manrope),
                color = nuviaColors.textSecondary,
            )
        }
        if (selected && !managing) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.selected_account),
                tint = nuviaColors.primary,
                modifier = Modifier.size(20.dp),
            )
        }
        if (managing) {
            Text(
                text = stringResource(R.string.sign_out),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = Sora,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = Color(0xFFEF4444),
            )
        }
    }
}

@Composable
private fun SelectorAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val nuviaColors = LocalNUViAColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = nuviaColors.textSecondary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Manrope,
                fontWeight = FontWeight.Medium,
            ),
            color = nuviaColors.textPrimary,
        )
    }
}
