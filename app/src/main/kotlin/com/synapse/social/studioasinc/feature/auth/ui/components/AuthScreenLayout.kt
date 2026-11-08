package com.synapse.social.studioasinc.feature.auth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.synapse.social.studioasinc.feature.auth.ui.util.WindowWidthSizeClass
import com.synapse.social.studioasinc.feature.auth.ui.util.calculateWindowSizeClass
import com.synapse.social.studioasinc.feature.shared.theme.Spacing

/**
 * Shared shell for the authentication screens.
 *
 * - [background] paints the page canvas; callers that want the branded gradient pass a [Brush] and
 *   everything else keeps the plain scheme background (null).
 * - The content column always scrolls, so long forms, large font scales and compact heights never
 *   clip a control; [imePadding] keeps the focused field above the keyboard.
 * - On anything wider than the compact width class the header and the form sit side by side, which
 *   is the existing tablet/foldable behaviour of this screen.
 */
@Composable
fun AuthScreenLayout(
    header: @Composable () -> Unit,
    form: @Composable () -> Unit,
    background: Brush? = null
) {
    val windowSizeClass = calculateWindowSizeClass()
    val isTwoColumn = windowSizeClass != WindowWidthSizeClass.Compact
    val backgroundModifier = if (background != null) Modifier.background(background) else Modifier

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isTwoColumn) {
            // paddingValues is applied here, on the Row that hosts both columns, so the status and
            // navigation bar insets protect the header and the form alike. The columns must not
            // apply it again or the bottom inset would be counted twice.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .padding(paddingValues)
                    .padding(Spacing.space24),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    header()
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(start = Spacing.space24),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    form()
                }
            }
        } else {
            // Same order as the two-column path: the page canvas fills the whole window (edge to
            // edge, behind the bars) and the insets are consumed inside the scroll container, with
            // the keyboard height added by imePadding so the focused field can always scroll clear
            // of it.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(backgroundModifier)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(paddingValues)
                    .padding(Spacing.space24),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                header()
                Spacer(modifier = Modifier.height(Spacing.space32))
                form()
            }
        }
    }
}

