package com.synapse.social.studioasinc.data.permission

enum class PermissionStatus {
    GRANTED,

    /** Never asked on this device; the system dialog can still be shown. */
    NOT_REQUESTED,

    /** Denied at least once, but the system would still show a rationale. */
    DENIED,

    /** Denied and the system will no longer show the dialog; only Settings can change it. */
    PERMANENTLY_DENIED,

    /** Not controlled by a runtime dialog (notification toggle off below Android 13). */
    RESTRICTED
}

data class PermissionSnapshot(
    val permission: AppPermission,
    val status: PermissionStatus
)
