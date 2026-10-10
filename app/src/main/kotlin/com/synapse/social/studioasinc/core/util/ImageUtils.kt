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

    fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                options.inJustDecodeBounds = false
                context.contentResolver.openInputStream(uri)?.use { inputStream2 ->
                    BitmapFactory.decodeStream(inputStream2, null, options)
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to decode bitmap from URI: $uri", e)
            null
        }
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

    fun resizeBitmapFileToSquare(fromPath: String, destPath: String, max: Int) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createScaledBitmap(src, max, max, true)
        saveBitmap(bitmap, destPath)
    }

    fun resizeBitmapFileToCircle(fromPath: String, destPath: String) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = 0xff424242.toInt()
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, src.width, src.height)

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawCircle(src.width / 2f, src.height / 2f, src.width / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    fun resizeBitmapFileWithRoundedBorder(fromPath: String, destPath: String, pixels: Int) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val color = 0xff424242.toInt()
        val paint = Paint().apply { isAntiAlias = true }
        val rect = Rect(0, 0, src.width, src.height)
        val rectF = RectF(rect)
        val roundPx = pixels.toFloat()

        canvas.drawARGB(0, 0, 0, 0)
        paint.color = color
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(src, rect, rect, paint)
        saveBitmap(bitmap, destPath)
    }

    fun cropBitmapFileFromCenter(fromPath: String, destPath: String, w: Int, h: Int) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val width = src.width
        val height = src.height
        if (width < w && height < h) return

        val x = if (width > w) (width - w) / 2 else 0
        val y = if (height > h) (height - h) / 2 else 0
        val cw = if (w > width) width else w
        val ch = if (h > height) height else h

        val bitmap = Bitmap.createBitmap(src, x, y, cw, ch)
        saveBitmap(bitmap, destPath)
    }

    fun rotateBitmapFile(fromPath: String, destPath: String, angle: Float) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postRotate(angle) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun scaleBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postScale(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun skewBitmapFile(fromPath: String, destPath: String, x: Float, y: Float) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val matrix = Matrix().apply { postSkew(x, y) }
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileColorFilter(fromPath: String, destPath: String, color: Int) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val bitmap = Bitmap.createBitmap(src, 0, 0, src.width - 1, src.height - 1)
        val paint = Paint().apply { colorFilter = LightingColorFilter(color, 1) }
        val canvas = Canvas(bitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileBrightness(fromPath: String, destPath: String, brightness: Float) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val cm = ColorMatrix(
            floatArrayOf(
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val bitmap = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun setBitmapFileContrast(fromPath: String, destPath: String, contrast: Float) {
        if (!FileUtils.isExistFile(fromPath)) return
        val src = BitmapFactory.decodeFile(fromPath)
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, 0f,
                0f, contrast, 0f, 0f, 0f,
                0f, 0f, contrast, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val bitmap = Bitmap.createBitmap(src.width, src.height, src.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(cm) }
        canvas.drawBitmap(src, 0f, 0f, paint)
        saveBitmap(bitmap, destPath)
    }

    fun getJpegRotate(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (e: Exception) {
            0
        }
    }

    fun createNewPictureFile(context: Context): File {
        val date = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${date.format(Date())}.jpg"
        return File(context.getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + File.separator + fileName)
    }

    fun saveImageToGallery(context: Context, bitmap: Bitmap, fileName: String, subFolder: String?, format: Bitmap.CompressFormat): Result<Uri> {
        val mimeType = if (format == Bitmap.CompressFormat.PNG) "image/png" else "image/jpeg"
        val extension = if (format == Bitmap.CompressFormat.PNG) ".png" else ".jpg"
        val finalFileName = fileName + extension

        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, finalFileName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                var relativePath = Environment.DIRECTORY_PICTURES
                if (!subFolder.isNullOrEmpty()) {
                    relativePath += File.separator + subFolder
                }
                put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val itemUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return Result.failure(IOException("Failed to create new MediaStore entry for image."))

        return try {
            resolver.openOutputStream(itemUri)?.use { os ->
                bitmap.compress(format, 95, os)
            } ?: throw IOException("Failed to get output stream for URI: $itemUri")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(itemUri, updateValues, null, null)
            }
            Result.success(itemUri)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save bitmap.", e)
            resolver.delete(itemUri, null, null)
            Result.failure(e)
        }
    }
}
