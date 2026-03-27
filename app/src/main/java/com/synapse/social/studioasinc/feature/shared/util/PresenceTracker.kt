package com.synapse.social.studioasinc.feature.shared.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import com.synapse.social.studioasinc.shared.domain.usecase.presence.StartPresenceTrackingUseCase
import com.synapse.social.studioasinc.shared.domain.usecase.presence.UpdatePresenceUseCase
import kotlinx.coroutines.launch

/**
 * Composable that manages user presence tracking lifecycle.
 * Add this to your main app composable or MainActivity.
 * 
 * Example usage:
 * ```
 * @Composable
 * fun App() {
 *     PresenceTracker()
 *     // Rest of your app
 * }
 * ```
 */
@Composable
fun PresenceTracker(
    startPresenceTracking: StartPresenceTrackingUseCase,
    updatePresence: UpdatePresenceUseCase
) {
    val scope = rememberCoroutineScope()
    
    DisposableEffect(Unit) {
        // Start tracking when composable enters composition
        scope.launch {
            startPresenceTracking()
        }
        
        // Stop tracking when composable leaves composition
        onDispose {
            scope.launch {
                updatePresence(false)
            }
        }
    }
}
