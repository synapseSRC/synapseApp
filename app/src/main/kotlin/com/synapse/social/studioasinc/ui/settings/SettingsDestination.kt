package com.synapse.social.studioasinc.ui.settings



sealed class SettingsDestination(val route: String) {



    object Hub : SettingsDestination(ROUTE_HUB)



    object Account : SettingsDestination(ROUTE_ACCOUNT)



    object Privacy : SettingsDestination(ROUTE_PRIVACY)



    object Appearance : SettingsDestination(ROUTE_APPEARANCE)



    object Notifications : SettingsDestination(ROUTE_NOTIFICATIONS)



    object Storage : SettingsDestination(ROUTE_STORAGE)

    object BackupRestore : SettingsDestination(ROUTE_BACKUP_RESTORE)



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



    object ManageStorage : SettingsDestination(ROUTE_MANAGE_STORAGE)



    object NetworkUsage : SettingsDestination(ROUTE_NETWORK_USAGE)



    object BusinessPlatform : SettingsDestination(ROUTE_BUSINESS_PLATFORM)

    object ChangeNumber : SettingsDestination(ROUTE_CHANGE_NUMBER)

    object BrandPartnerships : SettingsDestination(ROUTE_BRAND_PARTNERSHIPS)

    object BlockedContacts : SettingsDestination(ROUTE_BLOCKED_CONTACTS)

    object ChatSettings : SettingsDestination(ROUTE_CHAT_SETTINGS)

    object ChatFolders : SettingsDestination(ROUTE_CHAT_FOLDERS)

    object Flags : SettingsDestination(ROUTE_FLAGS)

    object Font : SettingsDestination(ROUTE_FONT)

    object ScheduledPosts : SettingsDestination(ROUTE_SCHEDULED_POSTS)

    object PrivacyLastSeen : SettingsDestination(ROUTE_PRIVACY_LAST_SEEN)

    object PrivacyProfilePhoto : SettingsDestination(ROUTE_PRIVACY_PROFILE_PHOTO)

    object PrivacyAbout : SettingsDestination(ROUTE_PRIVACY_ABOUT)

    object PrivacyStatus : SettingsDestination(ROUTE_PRIVACY_STATUS)

    object PrivacyGroups : SettingsDestination(ROUTE_PRIVACY_GROUPS)

    object PrivacyDisappearingMessages : SettingsDestination(ROUTE_PRIVACY_DISAPPEARING_MESSAGES)

    object PrivacyCheckup : SettingsDestination(ROUTE_PRIVACY_CHECKUP)

    object PrivacyCheckupCategory : SettingsDestination(ROUTE_PRIVACY_CHECKUP_CATEGORY)

    object PrivacyExceptionSelector : SettingsDestination(ROUTE_PRIVACY_EXCEPTION_SELECTOR)

    object AppLock : SettingsDestination(ROUTE_APP_LOCK)

    companion object {

        const val ROUTE_HUB = "settings_hub"
        const val ROUTE_ACCOUNT = "settings_account"
        const val ROUTE_PRIVACY = "settings_privacy"
        const val ROUTE_APPEARANCE = "settings_appearance"
        const val ROUTE_NOTIFICATIONS = "settings_notifications"
        const val ROUTE_STORAGE = "settings_storage"
        const val ROUTE_BACKUP_RESTORE = "settings_backup_restore"
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
        const val ROUTE_MANAGE_STORAGE = "settings_storage_manage"
        const val ROUTE_NETWORK_USAGE = "settings_network_usage"
        const val ROUTE_BUSINESS_PLATFORM = "settings_business_platform"
        const val ROUTE_CHANGE_NUMBER = "settings_change_number"
        const val ROUTE_BRAND_PARTNERSHIPS = "settings_brand_partnerships"
        const val ROUTE_BLOCKED_CONTACTS = "settings_blocked_contacts"
        const val ROUTE_CHAT_SETTINGS = "settings_chat_settings"
        const val ROUTE_CHAT_FOLDERS = "settings_chat_folders"
        const val ROUTE_FLAGS = "settings_flags"
        const val ROUTE_FONT = "settings_font"
        const val ROUTE_SCHEDULED_POSTS = "settings_scheduled_posts"
        const val ROUTE_PRIVACY_LAST_SEEN = "settings_privacy_last_seen"
        const val ROUTE_PRIVACY_PROFILE_PHOTO = "settings_privacy_profile_photo"
        const val ROUTE_PRIVACY_ABOUT = "settings_privacy_about"
        const val ROUTE_PRIVACY_STATUS = "settings_privacy_status"
        const val ROUTE_PRIVACY_GROUPS = "settings_privacy_groups"
        const val ROUTE_PRIVACY_DISAPPEARING_MESSAGES = "settings_privacy_disappearing_messages"
        const val ROUTE_PRIVACY_CHECKUP = "settings_privacy_checkup"
        const val ROUTE_PRIVACY_CHECKUP_CATEGORY = "settings_privacy_checkup_category/{categoryId}"
        const val ROUTE_PRIVACY_EXCEPTION_SELECTOR = "settings_privacy_exception_selector/{settingKey}"
        const val ROUTE_APP_LOCK = "settings_app_lock"

        fun createPrivacyCheckupCategoryRoute(categoryId: String): String =
            "settings_privacy_checkup_category/$categoryId"

        fun createPrivacyExceptionSelectorRoute(settingKey: String): String =
            "settings_privacy_exception_selector/$settingKey"


        fun allDestinations(): List<SettingsDestination> = listOf(
            Hub,
            Account,
            Privacy,
            Appearance,
            Notifications,
            Storage,
            BackupRestore,
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
            ManageStorage,
            NetworkUsage,
            BusinessPlatform,
            ChangeNumber,
            BrandPartnerships,
            BlockedContacts,
            ChatSettings,
            ChatFolders,
            Flags,
            Font,
            ScheduledPosts,
            PrivacyLastSeen,
            PrivacyProfilePhoto,
            PrivacyAbout,
            PrivacyStatus,
            PrivacyGroups,
            PrivacyDisappearingMessages,
            PrivacyCheckup,
            PrivacyCheckupCategory,
            PrivacyExceptionSelector,
            AppLock
        )



        fun fromRoute(route: String): SettingsDestination? = when (route) {
            ROUTE_HUB -> Hub
            ROUTE_ACCOUNT -> Account
            ROUTE_PRIVACY -> Privacy
            ROUTE_APPEARANCE -> Appearance
            ROUTE_NOTIFICATIONS -> Notifications
            ROUTE_STORAGE -> Storage
            ROUTE_BACKUP_RESTORE -> BackupRestore
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
            ROUTE_MANAGE_STORAGE -> ManageStorage
            ROUTE_NETWORK_USAGE -> NetworkUsage
            ROUTE_BUSINESS_PLATFORM -> BusinessPlatform
            ROUTE_CHANGE_NUMBER -> ChangeNumber
            ROUTE_BRAND_PARTNERSHIPS -> BrandPartnerships
            ROUTE_BLOCKED_CONTACTS -> BlockedContacts
            ROUTE_CHAT_SETTINGS -> ChatSettings
            ROUTE_CHAT_FOLDERS -> ChatFolders
            ROUTE_FLAGS -> Flags
            ROUTE_FONT -> Font
            ROUTE_SCHEDULED_POSTS -> ScheduledPosts
            ROUTE_PRIVACY_LAST_SEEN -> PrivacyLastSeen
            ROUTE_PRIVACY_PROFILE_PHOTO -> PrivacyProfilePhoto
            ROUTE_PRIVACY_ABOUT -> PrivacyAbout
            ROUTE_PRIVACY_STATUS -> PrivacyStatus
            ROUTE_PRIVACY_GROUPS -> PrivacyGroups
            ROUTE_PRIVACY_DISAPPEARING_MESSAGES -> PrivacyDisappearingMessages
            ROUTE_PRIVACY_CHECKUP -> PrivacyCheckup
            ROUTE_APP_LOCK -> AppLock
            else -> null
        }
    }
}
