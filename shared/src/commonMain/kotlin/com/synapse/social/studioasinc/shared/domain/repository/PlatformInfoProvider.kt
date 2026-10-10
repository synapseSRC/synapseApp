package com.synapse.social.studioasinc.shared.domain.repository

interface PlatformInfoProvider {
    fun getAppVersionCode(): Int
    fun isNetworkAvailable(): Boolean
}
