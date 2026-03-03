package com.synapse.social.studioasinc.core.util

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.widget.ImageView
import coil.imageLoader
import coil.load
import coil.request.ImageRequest
import coil.request.Disposable
import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.R



object ImageLoader {
    private const val TAG = "ImageLoader"
    private const val MAX_RETRIES = 2
    private const val INITIAL_RETRY_DELAY_MS = 100L



    fun loadImage(
        context: Context,
        url: String?,
        imageView: ImageView,
        placeholder: Int = R.drawable.default_image,
        onSuccess: (() -> Unit)? = null,
        onFailure: (() -> Unit)? = null
    ) {
        if (url.isNullOrBlank()) {
            imageView.setImageResource(placeholder)
            onFailure?.invoke()
            return
        }

        loadImageWithRetry(
            context = context,
            url = url,
            imageView = imageView,
            placeholder = placeholder,
            retryCount = 0,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }



    private fun loadImageWithRetry(
        context: Context,
        url: String,
        imageView: ImageView,
        placeholder: Int,
        retryCount: Int,
        onSuccess: (() -> Unit)?,
        onFailure: (() -> Unit)?
    ) {

        imageView.load(url) {
            placeholder(placeholder)
            error(placeholder)


            addSupabaseAuthHeaders(this, url)

            listener(
                onSuccess = { _, _ ->
                    Log.d(TAG, "Image loaded successfully: $url")
                    onSuccess?.invoke()
                },
                onError = { _, _ ->
                    Log.w(TAG, "Image load failed (attempt ${retryCount + 1}/${MAX_RETRIES + 1}): $url")

                    if (retryCount < MAX_RETRIES) {

                        val delayMs = INITIAL_RETRY_DELAY_MS * (1 shl retryCount)

                        Log.d(TAG, "Retrying image load after ${delayMs}ms...")




                        imageView.postDelayed({
                            loadImageWithRetry(
                                context = context,
                                url = url,
                                imageView = imageView,
                                placeholder = placeholder,
                                retryCount = retryCount + 1,
                                onSuccess = onSuccess,
                                onFailure = onFailure
                            )
                        }, delayMs)
                    } else {
                        Log.e(TAG, "All retry attempts exhausted for: $url")

                        imageView.setImageResource(placeholder)
                        onFailure?.invoke()
                    }
                }
            )
        }
    }



    private fun addSupabaseAuthHeaders(builder: ImageRequest.Builder, url: String) {

        val needsAuth = url.contains("supabase.co/storage") &&
                       !url.contains("/public/")

        if (needsAuth) {
            builder.addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            builder.addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
        }
    }



    fun buildImageRequest(context: Context, url: String?): ImageRequest {
        val builder = ImageRequest.Builder(context)
            .data(url)

        if (!url.isNullOrBlank()) {
            addSupabaseAuthHeaders(builder, url)
        }

        return builder.build()
    }



    fun preloadImage(context: Context, url: String?) {
        if (url.isNullOrBlank()) return

        val requestBuilder = ImageRequest.Builder(context)
            .data(url)

        addSupabaseAuthHeaders(requestBuilder, url)

        context.imageLoader.enqueue(requestBuilder.build())
    }



    fun clearMemoryCache(context: Context) {
        context.imageLoader.memoryCache?.clear()
    }



    suspend fun clearDiskCache(context: Context) {
        context.imageLoader.diskCache?.clear()
    }
}
