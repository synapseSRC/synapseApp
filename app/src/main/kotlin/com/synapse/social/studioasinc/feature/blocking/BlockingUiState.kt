package com.synapse.social.studioasinc.feature.blocking

/**
 * UI state for blocking feature.
 * Represents the different states of blocking operations.
 */
sealed class BlockingUiState {
    /**
     * Idle state - no operation in progress.
     */
    data object Idle : BlockingUiState()
    
    /**
     * Loading state - operation in progress.
     */
    data object Loading : BlockingUiState()
    
    /**
     * Block operation completed successfully.
     */
    data object BlockSuccess : BlockingUiState()
    
    /**
     * Unblock operation completed successfully.
     */
    data object UnblockSuccess : BlockingUiState()
    
    /**
     * Error state with message.
     * @param message User-friendly error message
     */
    data class Error(val message: String) : BlockingUiState()
}
