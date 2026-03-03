package com.synapse.social.studioasinc.ui.settings



sealed class SettingsDestination(val route: String) {



    object Hub : SettingsDestination(ROUTE_HUB)



    object Account : SettingsDestination(ROUTE_ACCOUNT)



    object Privacy : SettingsDestination(ROUTE_PRIVACY)



    object Appearance : SettingsDestination(ROUTE_APPEARANCE)



    object Notifications : SettingsDestination(ROUTE_NOTIFICATIONS)



    object Storage : SettingsDestination(ROUTE_STORAGE)



    object Language : SettingsDestination(ROUTE_LANGUAGE)



    object About : SettingsDestination(ROUTE_ABOUT)



    object StorageProvider : SettingsDestination(ROUTE_STORAGE_PROVIDER)



    object Licenses : SettingsDestination(ROUTE_LICENSES)



    object SynapsePlus : SettingsDestination(ROUTE_SYNAPSE_PLUS)



    object Avatar : SettingsDestination(ROUTE_AVATAR)



    object Favourites : SettingsDestination(ROUTE_FAVOURITES)



    object Accessibility : SettingsDestination(ROUTE_ACCESSIBILITY)



    object Search : SettingsDestination(ROUTE_SEARCH)



    object ApiKey : SettingsDestination(ROUTE_API_KEY)



    object RequestAccountInfo : SettingsDestination(ROUTE_REQUEST_ACCOUNT_INFO)



    object AccountInfo : SettingsDestination(ROUTE_ACCOUNT_INFO)



    object ManageStorage : SettingsDestination(ROUTE_MANAGE_STORAGE)



    object NetworkUsage : SettingsDestination(ROUTE_NETWORK_USAGE)



    object BusinessPlatform : SettingsDestination(ROUTE_BUSINESS_PLATFORM)

    object ChangeNumber : SettingsDestination(ROUTE_CHANGE_NUMBER)

    companion object {

        const val ROUTE_HUB = "settings_hub"
        const val ROUTE_ACCOUNT = "settings_account"
        const val ROUTE_PRIVACY = "settings_privacy"
        const val ROUTE_APPEARANCE = "settings_appearance"
        const val ROUTE_NOTIFICATIONS = "settings_notifications"
        const val ROUTE_STORAGE = "settings_storage"
        const val ROUTE_STORAGE_PROVIDER = "settings_storage_provider"
        const val ROUTE_LANGUAGE = "settings_language"
        const val ROUTE_ABOUT = "settings_about"
        const val ROUTE_LICENSES = "settings_licenses"
        const val ROUTE_SYNAPSE_PLUS = "settings_synapse_plus"
        const val ROUTE_AVATAR = "settings_avatar"
        const val ROUTE_FAVOURITES = "settings_favourites"
        const val ROUTE_ACCESSIBILITY = "settings_accessibility"
        const val ROUTE_SEARCH = "settings_search"
        const val ROUTE_API_KEY = "settings_api_key"
        const val ROUTE_REQUEST_ACCOUNT_INFO = "settings_request_account_info"
        const val ROUTE_ACCOUNT_INFO = "settings_account_info"
        const val ROUTE_MANAGE_STORAGE = "settings_storage_manage"
        const val ROUTE_NETWORK_USAGE = "settings_network_usage"
        const val ROUTE_BUSINESS_PLATFORM = "settings_business_platform"
        const val ROUTE_CHANGE_NUMBER = "settings_change_number"



        fun allDestinations(): List<SettingsDestination> = listOf(
            Hub,
            Account,
            Privacy,
            Appearance,
            Notifications,
            Storage,
            StorageProvider,
            Language,
            About,
            Licenses,
            SynapsePlus,
            Avatar,
            Favourites,
            Accessibility,
            ApiKey,
            RequestAccountInfo,
            AccountInfo,
            ManageStorage,
            NetworkUsage,
            BusinessPlatform,
            ChangeNumber
        )



        fun fromRoute(route: String): SettingsDestination? = when (route) {
            ROUTE_HUB -> Hub
            ROUTE_ACCOUNT -> Account
            ROUTE_PRIVACY -> Privacy
            ROUTE_APPEARANCE -> Appearance
            ROUTE_NOTIFICATIONS -> Notifications
            ROUTE_STORAGE -> Storage
            ROUTE_STORAGE_PROVIDER -> StorageProvider
            ROUTE_LANGUAGE -> Language
            ROUTE_ABOUT -> About
            ROUTE_LICENSES -> Licenses
            ROUTE_SYNAPSE_PLUS -> SynapsePlus
            ROUTE_AVATAR -> Avatar
            ROUTE_FAVOURITES -> Favourites
            ROUTE_ACCESSIBILITY -> Accessibility
            ROUTE_API_KEY -> ApiKey
            ROUTE_REQUEST_ACCOUNT_INFO -> RequestAccountInfo
            ROUTE_ACCOUNT_INFO -> AccountInfo
            ROUTE_MANAGE_STORAGE -> ManageStorage
            ROUTE_NETWORK_USAGE -> NetworkUsage
            ROUTE_BUSINESS_PLATFORM -> BusinessPlatform
            ROUTE_CHANGE_NUMBER -> ChangeNumber
            else -> null
        }
    }
}
