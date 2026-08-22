package vn.edu.vgu.smartlocker.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import vn.edu.vgu.smartlocker.NavClearance
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.parcels.SectionLabel
import vn.edu.vgu.smartlocker.BuildConfig
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.AppLanguage
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.LockerToggle
import vn.edu.vgu.smartlocker.ui.Recess
import vn.edu.vgu.smartlocker.ui.ThemeSwitch
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens
import vn.edu.vgu.smartlocker.ui.theme.NumberFace

/**
 * The third tab.
 *
 * Account card, then grouped rows: account, notices, app. The settings rows
 * are one material — a card — carrying an icon well, a title and a value;
 * the switches are the only controls here that are not a row.
 */
@Composable
fun SettingsScreen(
    name: String = "Minh Nguyễn",
    phone: String = "0912 345 678",
    dark: Boolean = false,
    onToggleDark: () -> Unit = {},
    language: AppLanguage = AppLanguage.ENGLISH,
    onLanguage: (AppLanguage) -> Unit = {},
    onPin: () -> Unit = {},
    onPassword: () -> Unit = {},
    onLogOut: () -> Unit = {},
) {
    val t = LocalLockerTokens.current
    var parcelNotices by remember { mutableStateOf(true) }
    var smsBackup by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

    if (picking) {
        LanguagePicker(
            current = language,
            onPick = { onLanguage(it) },
            onClose = { picking = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        // Who is signed in.
        CardMaterial(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(t.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = name.split(" ").map { it.firstOrNull() ?: "" }.joinToString("").take(2).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = t.accentInk,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleLarge,
                        color = t.ink,
                    )
                    Text(
                        text = phone,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = NumberFace,
                        ),
                        color = t.ink2,
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.set_account))
        Stack {
            SettingsRow(
                icon = AppIcons.Lock,
                title = stringResource(R.string.set_pin),
                value = stringResource(R.string.set_pin_value),
                onClick = onPin,
            )
            SettingsRow(
                icon = AppIcons.Shield,
                title = stringResource(R.string.set_password),
                onClick = onPassword,
            )
        }

        SectionLabel(stringResource(R.string.set_notices))
        Stack {
            SettingsRow(icon = AppIcons.Bell, title = stringResource(R.string.set_parcel_arrived)) {
                LockerToggle(checked = parcelNotices, onCheckedChange = { parcelNotices = it })
            }
            SettingsRow(icon = AppIcons.Mail, title = stringResource(R.string.set_sms_backup)) {
                LockerToggle(checked = smsBackup, onCheckedChange = { smsBackup = it })
            }
        }

        SectionLabel(stringResource(R.string.set_app))
        Stack {
            SettingsRow(
                icon = AppIcons.Globe,
                title = stringResource(R.string.set_language),
                // The row shows the language you are IN, which is also the
                // one written in itself in the picker below it.
                value = stringResource(language.labelRes),
                onClick = { picking = true },
            )
            // Not a LockerToggle. Dark mode gets the day/night switch from
            // the mock-up — the sun, the moon crossing it, the clouds and
            // the stars. The other two rows are plain on/off and keep the
            // plain control.
            SettingsRow(icon = AppIcons.Moon, title = stringResource(R.string.set_dark_mode)) {
                ThemeSwitch(checked = dark, onCheckedChange = { onToggleDark() })
            }
            // Which build this is. Not decoration: without it nobody holding
            // the phone can tell a build that failed to install from a fix
            // that failed to work, and every report about it is ambiguous.
            // Six releases went out on one version number before anyone
            // noticed. The build file has claimed the app shows this for a
            // while; until now it did not.
            SettingsRow(
                icon = AppIcons.Badge,
                title = stringResource(R.string.set_version),
                value = BuildConfig.VERSION_NAME,
            )
            // Last, and in the same stack rather than shouting in red at the
            // bottom of the screen. Logging out of a locker app is a small
            // act - the parcels stay where they are - and a row that looks
            // dangerous invites the question of what it will destroy.
            SettingsRow(
                icon = AppIcons.LogOut,
                title = stringResource(R.string.set_log_out),
                onClick = onLogOut,
            )
        }

        // Room to scroll the last row out from under the floating nav.
        Spacer(Modifier.height(NavClearance))
    }
}

@Composable
private fun Stack(content: @Composable ColumnScope.() -> Unit) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/** One settings row: an icon well, a title, a value and a chevron — or a
 * control in place of the chevron. */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val t = LocalLockerTokens.current
    CardMaterial(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Recess(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = t.ink2,
                    )
                }
            }
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = t.ink,
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelLarge,
                    color = t.ink3,
                )
            }
            if (trailing != null) {
                trailing()
            } else if (onClick != null) {
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = t.ink3,
                )
            }
        }
    }
}
