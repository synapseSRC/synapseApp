


package com.synapse.social.studioasinc.core.config



object CloudinaryConfig {


    const val CLOUD_NAME = "synapse-social"
    const val BASE_URL = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/"
    const val VIDEO_BASE_URL = "https://res.cloudinary.com/$CLOUD_NAME/video/upload/"


    object ImageTransformations {

        const val GALLERY_THUMBNAIL = "w_400,h_400,c_fill,q_auto,f_auto"


        const val REPLY_PREVIEW = "w_120,h_120,c_fill,q_auto,f_auto"


        const val CAROUSEL_IMAGE_SMALL = "w_200,h_200,c_fill,q_auto,f_auto"
        const val CAROUSEL_IMAGE_MEDIUM = "w_400,h_400,c_fill,q_auto,f_auto"
        const val CAROUSEL_IMAGE_LARGE = "w_600,h_600,c_fill,q_auto,f_auto"


        const val FULL_RESOLUTION = "q_auto,f_auto"


        const val PROFILE_SMALL = "w_100,h_100,c_fill,g_face,q_auto,f_auto"
        const val PROFILE_MEDIUM = "w_200,h_200,c_fill,g_face,q_auto,f_auto"


        const val POST_THUMBNAIL = "w_300,h_300,c_fill,q_auto,f_auto"
        const val POST_FULL = "w_800,h_800,c_limit,q_auto,f_auto"
    }


    object VideoTransformations {
        const val VIDEO_THUMBNAIL = "w_400,h_400,c_fill,so_auto"
        const val VIDEO_PREVIEW = "w_200,h_200,c_fill,so_auto"
    }



    fun buildImageUrl(publicId: String?, transformation: String?): String {
        if (publicId.isNullOrEmpty()) {
            return ""
        }

        if (transformation.isNullOrEmpty()) {
            return BASE_URL + publicId
        }

        return "$BASE_URL$transformation/$publicId"
    }



    fun buildVideoUrl(publicId: String?, transformation: String?): String {
        if (publicId.isNullOrEmpty()) {
            return ""
        }

        if (transformation.isNullOrEmpty()) {
            return VIDEO_BASE_URL + publicId
        }

        return "$VIDEO_BASE_URL$transformation/$publicId"
    }



    fun buildGalleryThumbnailUrl(publicId: String?, isVideo: Boolean): String {
        return if (isVideo) {
            buildVideoUrl(publicId, VideoTransformations.VIDEO_THUMBNAIL)
        } else {
            buildImageUrl(publicId, ImageTransformations.GALLERY_THUMBNAIL)
        }
    }



    fun buildCarouselImageUrl(publicId: String?, densityDpi: Int): String {
        val transformation = when {
            densityDpi >= 480 -> ImageTransformations.CAROUSEL_IMAGE_LARGE
            densityDpi >= 320 -> ImageTransformations.CAROUSEL_IMAGE_MEDIUM
            else -> ImageTransformations.CAROUSEL_IMAGE_SMALL
        }
        return buildImageUrl(publicId, transformation)
    }



    fun buildCarouselImageUrl(publicId: String?): String {
        return buildImageUrl(publicId, ImageTransformations.CAROUSEL_IMAGE_MEDIUM)
    }



    fun buildOptimizedImageUrl(publicId: String?, widthPx: Int, heightPx: Int): String {
        val transformation = "w_$widthPx,h_$heightPx,c_fill,q_auto,f_auto"
        return buildImageUrl(publicId, transformation)
    }



    fun buildReplyPreviewUrl(publicId: String?): String {
        return buildImageUrl(publicId, ImageTransformations.REPLY_PREVIEW)
    }
}
