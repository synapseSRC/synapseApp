package com.synapse.social.studioasinc.core.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LightingColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ImageUtils {
    private const val TAG = "ImageUtils"

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val width = options.outWidth
        val height = options.outHeight
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    @JvmStatic
    fun decodeSampleBitmapFromPath(path: String, reqWidth: Int, reqHeight: Int): Bitmap {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(path, options)
    }

    fun saveBitmap(bitmap: Bitmap, destPath: String) {
        FileUtils.createNewFile(destPath)
        try {
            FileOutputStream(File(destPath)).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            Log.e(TAG, "saveBitmap failed", e)
        }
    }

    fun getScaledBitmap(path: String, max: Int): Bitmap {
        val src = BitmapFactory.decodeFile(path)
        var width = src.width
        var height = src.height
        val rate = if (width > height) max / width.toFloat() else max / height.toFloat()
        width = (width * rate).toInt()
        height = (height * rate).toInt()
        return Bitmap.createScaledBitmap(src, width, height, true)
    }

    fun resizeBitmapFileRetainRatio(fromPath: String, destPath: String, max: Int) {
        if (!FileUtils.isExistFile(fromPath)) return
        val bitmap = getScaledBitmap(fromPath, max)
        saveBitmap(bitmap, destPath)
    }
}
