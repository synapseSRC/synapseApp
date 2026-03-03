


package com.synapse.social.studioasinc.domain.model

import kotlinx.serialization.Serializable



@Serializable
data class Attachment(
    var publicId: String? = null,
    var url: String? = null,
    var width: Int = 0,
    var height: Int = 0,
    var type: String? = null,
    var mimeType: String? = null,
    var size: Long = 0
) {

    constructor(publicId: String?, url: String?, width: Int, height: Int, type: String?) : this(
        publicId = publicId,
        url = url,
        width = width,
        height = height,
        type = type,
        mimeType = null,
        size = 0
    )



    fun isVideo(): Boolean {
        return "video" == type || (publicId != null && publicId!!.contains("|video"))
    }



    fun isImage(): Boolean {
        return "image" == type || !isVideo()
    }



    fun getAspectRatio(): Float {
        if (height > 0 && width > 0) {
            return width.toFloat() / height
        }
        return 1.0f
    }



    fun isPortrait(): Boolean {
        return height > width && width > 0
    }



    fun isLandscape(): Boolean {
        return width > height && height > 0
    }
}
