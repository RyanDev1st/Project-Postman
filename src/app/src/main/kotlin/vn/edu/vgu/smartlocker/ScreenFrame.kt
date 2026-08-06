package vn.edu.vgu.smartlocker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import vn.edu.vgu.smartlocker.ui.LockerBackdrop

/**
 * The ground every screen stands on, and the edges it keeps clear.
 *
 * Content stays out from under the status bar, the gesture bar and the
 * keyboard, and off the edge of the glass. App chrome, not feature code,
 * which is why it sits beside MainActivity rather than in a feature folder.
 */
@Composable
fun ScreenFrame(
    verticalArrangement: Arrangement.Vertical = Arrangement.Center,
    content: @Composable ColumnScope.() -> Unit,
) {
    LockerBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}
