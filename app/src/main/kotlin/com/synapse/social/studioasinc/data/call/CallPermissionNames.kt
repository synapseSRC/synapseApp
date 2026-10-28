package com.synapse.social.studioasinc.data.call

import android.Manifest
import com.synapse.social.studioasinc.shared.domain.model.call.CallPermission

/**
 * The platform constant behind each capability.
 *
 * One place, because a typo here is a call that silently never starts: the domain deliberately does
 * not know these strings, and the permission prompt, the grant check and the manifest have to agree
 * on them exactly.
 */
internal val CallPermission.manifestName: String
    get() = when (this) {
        CallPermission.MICROPHONE -> Manifest.permission.RECORD_AUDIO
        CallPermission.CAMERA -> Manifest.permission.CAMERA
    }

internal fun List<CallPermission>.toManifestNames(): Array<String> = map { it.manifestName }.toTypedArray()
