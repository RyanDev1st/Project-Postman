package vn.edu.vgu.smartlocker.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import vn.edu.vgu.smartlocker.R
import vn.edu.vgu.smartlocker.ui.AppIcons
import vn.edu.vgu.smartlocker.ui.AppLanguage
import vn.edu.vgu.smartlocker.ui.CardMaterial
import vn.edu.vgu.smartlocker.ui.theme.LocalLockerTokens

/**
 * Pick a language.
 *
 * A list rather than a switch, even though there are two of them today. A
 * switch would have to be labelled in one language to offer the other, which
 * is exactly backwards for the person who cannot read the label — and the
 * university this is built for has a third language in its name.
 *
 * Each language is written **in itself**: `Tiếng Việt`, not "Vietnamese".
 * Someone looking for their own language is looking for the word they would
 * write, and if they could read the other one they would not need the row.
 *
 * The choice applies on the frame after the tap, with this sheet still open,
 * so the tick and the words around it change together and you can see that
 * the thing you pressed did what it said.
 */
@Composable
fun LanguagePicker(
    current: AppLanguage,
    onPick: (AppLanguage) -> Unit,
    onClose: () -> Unit,
) {
    val t = LocalLockerTokens.current
    Dialog(onDismissRequest = onClose) {
        CardMaterial(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = stringResource(R.string.set_language),
                    style = MaterialTheme.typography.labelMedium,
                    color = t.ink2,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                AppLanguage.entries.forEach { language ->
                    LanguageRow(
                        label = stringResource(language.labelRes),
                        selected = language == current,
                        onClick = { onPick(language) },
                    )
                }
            }
        }
    }
}

/** One language: its own name, and a tick when it is the one being drawn. */
@Composable
private fun LanguageRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val t = LocalLockerTokens.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            color = if (selected) t.ink else t.ink2,
            modifier = Modifier.weight(1f),
        )
        // The tick keeps its space when it is absent, so the two rows do not
        // shuffle sideways as the choice moves between them.
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) t.accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = t.accentInk,
                )
            }
        }
    }
}
