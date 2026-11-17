package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.LruCache
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

object MediaUtils {
  private val bitmapCache =
    object : LruCache<String, Bitmap>(32) {}

  fun decodeDataUrlToBitmap(dataUrl: String?): Bitmap? {
    if (dataUrl.isNullOrBlank()) return null
    val key = dataUrl.hashCode().toString() + "_" + dataUrl.length
    bitmapCache.get(key)?.let { return it }
    if (!dataUrl.startsWith("data:image")) return null
    val commaIdx = dataUrl.indexOf(',')
    if (commaIdx <= 0) return null
    return runCatching {
        val bytes = Base64.decode(dataUrl.substring(commaIdx + 1), Base64.DEFAULT)
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bmp != null) bitmapCache.put(key, bmp)
        bmp
      }
      .getOrNull()
  }

  fun encodeImageUriToDataUrl(
    context: Context,
    uri: Uri,
    maxDim: Int = 720,
    quality: Int = 74,
  ): String? {
    return runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val origW = boundsOpts.outWidth
        val origH = boundsOpts.outHeight
        if (origW <= 0 || origH <= 0) return null

        var inSample = 1
        while (max(origW / inSample, origH / inSample) > maxDim * 2) {
          inSample *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
        val decoded =
          context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
          } ?: return null

        val scaled = scaleBitmap(decoded, maxDim)
        if (scaled !== decoded) decoded.recycle()
        bitmapToDataUrl(scaled, quality).also { scaled.recycle() }
      }
      .getOrNull()
  }

  fun extractVideoThumbnailDataUrl(context: Context, videoUri: Uri): String? {
    return runCatching {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, videoUri)
        val frame = retriever.getFrameAtTime(500_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
          ?: retriever.frameAtTime
        retriever.release()
        if (frame != null) {
          val scaled = scaleBitmap(frame, 600)
          if (scaled !== frame) frame.recycle()
          bitmapToDataUrl(scaled, 72).also { scaled.recycle() }
        } else {
          generateGradientCoverDataUrl("Video Reel")
        }
      }
      .getOrNull() ?: generateGradientCoverDataUrl("Video Reel")
  }

  fun copyVideoToInternalStorage(context: Context, sourceUri: Uri): String? {
    return runCatching {
        val dir = File(context.filesDir, "reels_media").apply { mkdirs() }
        val dest = File(dir, "vid_${UUID.randomUUID()}.mp4")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
          FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        Uri.fromFile(dest).toString()
      }
      .getOrNull()
  }

  fun generateGradientCoverDataUrl(seed: String): String {
    val width = 420
    val height = 540
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val palettes =
      listOf(
        intArrayOf(0xFFE1306C.toInt(), 0xFFF77737.toInt()),
        intArrayOf(0xFF111115.toInt(), 0xFF833AB4.toInt()),
        intArrayOf(0xFFFF5858.toInt(), 0xFFFCAF45.toInt()),
        intArrayOf(0xFF1A1A20.toInt(), 0xFF0095F6.toInt()),
      )
    val colors = palettes[(seed.hashCode() and 0x7fffffff) % palettes.size]
    val paint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            colors[0],
            colors[1],
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    val ringPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x28FFFFFF
        style = Paint.Style.STROKE
        strokeWidth = 6f
      }
    canvas.drawCircle(width * 0.78f, height * 0.25f, 110f, ringPaint)
    canvas.drawCircle(width * 0.25f, height * 0.75f, 140f, ringPaint)

    return bitmapToDataUrl(bmp, 76).also { bmp.recycle() }
  }

  private fun scaleBitmap(src: Bitmap, maxDim: Int): Bitmap {
    val maxSide = max(src.width, src.height)
    if (maxSide <= maxDim) return src
    val ratio = maxDim.toFloat() / maxSide.toFloat()
    val w = (src.width * ratio).roundToInt().coerceAtLeast(1)
    val h = (src.height * ratio).roundToInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(src, w, h, true)
  }

  private fun bitmapToDataUrl(bitmap: Bitmap, quality: Int): String {
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    return "data:image/jpeg;base64,$b64"
  }
}

class VoiceRecorderHelper(private val context: Context) {
  private var recorder: MediaRecorder? = null
  private var outputFile: File? = null
  private var startTimeMs: Long = 0L

  fun startRecording(): Boolean {
    return runCatching {
        stopRecordingInternal()
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        val rec =
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
          } else {
            @Suppress("DEPRECATION") MediaRecorder()
          }
        rec.apply {
          setAudioSource(MediaRecorder.AudioSource.MIC)
          setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
          setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
          setAudioEncodingBitRate(32000)
          setAudioSamplingRate(16000)
          setMaxDuration(30_000)
          setOutputFile(file.absolutePath)
          prepare()
          start()
        }
        recorder = rec
        startTimeMs = System.currentTimeMillis()
        true
      }
      .getOrDefault(false)
  }

  fun stopAndEncode(): Pair<String, Int>? {
    val file = outputFile ?: return null
    val durationSec =
      ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt().coerceIn(1, 30)
    stopRecordingInternal()
    return runCatching {
        if (!file.exists() || file.length() == 0L) return null
        val bytes = file.readBytes()
        file.delete()
        if (bytes.size > 380_000) return null
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        "data:audio/mp4;base64,$b64" to durationSec
      }
      .getOrNull()
  }

  fun cancel() {
    stopRecordingInternal()
    outputFile?.delete()
    outputFile = null
  }

  private fun stopRecordingInternal() {
    runCatching {
      recorder?.apply {
        stop()
        release()
      }
    }
    recorder = null
  }
}

class VoicePlayerHelper(private val context: Context) {
  private var player: MediaPlayer? = null
  private var tempFile: File? = null

  fun play(dataUrl: String, onCompleted: () -> Unit) {
    stop()
    val comma = dataUrl.indexOf(',')
    if (comma <= 0) {
      onCompleted()
      return
    }
    runCatching {
        val bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT)
        val file = File(context.cacheDir, "play_${System.currentTimeMillis()}.m4a")
        file.writeBytes(bytes)
        tempFile = file
        player =
          MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setOnCompletionListener {
              stop()
              onCompleted()
            }
            prepare()
            start()
          }
      }
      .onFailure { onCompleted() }
  }

  fun stop() {
    runCatching {
      player?.apply {
        if (isPlaying) stop()
        release()
      }
    }
    player = null
    tempFile?.delete()
    tempFile = null
  }
}
