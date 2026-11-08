package com.synapse.social.studioasinc.core.util

import com.synapse.social.studioasinc.shared.core.config.SynapseConfig

/**
 * Canonical legal destinations, derived from the project website configured in [SynapseConfig].
 *
 * Single source of truth for every entry point that links to the terms or the privacy policy
 * (welcome screen, Settings -> About), so the paths can never drift apart.
 */
object SynapseLegalUrls {
    val website: String = SynapseConfig.APP_WEBSITE_URL
    val termsOfService: String = "$website/reference/terms/"
    val privacyPolicy: String = "$website/reference/privacy/"
}
