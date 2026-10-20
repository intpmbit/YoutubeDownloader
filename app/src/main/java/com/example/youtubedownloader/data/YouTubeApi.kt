package com.example.youtubedownloader.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.util.concurrent.TimeUnit

object YouTubeApi {

    private const val PLAYER_ENDPOINT = "https://www.youtube.com/youtubei/v1/player"

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    /**
     * يجلب معلومات الفيديو والتيارات المتاحة من نقطة نهاية YouTube InnerTube (بدون مفتاح API).
     * يعيد VideoInfo zawierすerde قائمة التيارات (جودة فيديو وصوت).
     * ملاحظة: يوتيوب يغيّر آلية التوقيع (cipher) دورياً؛ إذا توفّر رابط مباشر في الاستجابة يُستخدم،
     * وإلا يُحفظ التشفير في [Stream.cipher] لمعالجته لاحقاً.
     */
    suspend fun fetchVideoInfo(videoId: String): VideoInfo {
        val body = JsonObject()
        body.addProperty("videoId", videoId)
        val context = JsonObject()
        val clientObj = JsonObject()
        clientObj.addProperty("clientName", "WEB")
        clientObj.addProperty("clientVersion", "2.20240101.00.00")
        clientObj.addProperty("clientScreen", "EMBED")
        context.add("client", clientObj)
        body.add("context", context)

        val requestBody = RequestBody.create(
            body.toString(),
            MediaType.get("application/json; charset=utf-8")!!
        )
        val request = Request.Builder()
            .url(PLAYER_ENDPOINT)
            .post(requestBody)
            .header("X-YouTube-Client-Name", "1")
            .header("X-YouTube-Client-Version", "2.20240101.00.00")
            .header("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36")
            .header("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val json = response.body?.string() ?: throw Exception("استجابة فارغة من يوتيوب")
        if (!response.isSuccessful) {
            throw Exception("فشل جلب معلومات الفيديو (${response.code})")
        }

        val root = JsonParser.parseString(json).asJsonObject
        return parse(root, videoId)
    }

    private fun parse(root: JsonObject, videoId: String): VideoInfo {
        val details = root.getAsJsonObject("videoDetails")
        val title = getString(details, "title")
        val author = getString(details, "author")
        val lengthSeconds = getInt(details, "lengthSeconds")
        val thumbnail = extractThumbnail(details)

        val streamingData = root.getAsJsonObject("streamingData")
        val streams = mutableListOf<Stream>()

        // تجميع التيارات التكيفية (فيديو فقط / صوت فقط) حسب جودة العرض مع الاحتفاظ بأعلى بتreit
        val adaptive = streamingData.getAsJsonArray("adaptiveFormats")
        if (adaptive != null) {
            val bestByQuality = LinkedHashMap<String, Stream>()
            for (i in 0 until adaptive.size()) {
                val fmt = adaptive.get(i).asJsonObject
                val stream = parseFormat(fmt)
                if (stream == null) continue
                val key = stream.qualityLabel ?: "unknown_${stream.itag}"
                val existing = bestByQuality[key]
                if (existing == null || stream.bitrate > existing.bitrate) {
                    bestByQuality[key] = stream
                }
            }
            streams.addAll(bestByQuality.values)
        }

        // تجميع التقدمية (فيديو+صوت مدمج) إن توفّرت
        val formats = streamingData.getAsJsonArray("formats")
        if (formats != null) {
            for (i in 0 until formats.size()) {
                val fmt = formats.get(i).asJsonObject
                val stream = parseFormat(fmt)
                if (stream != null) streams.add(stream)
            }
        }

        // ترتيب: الفيديوهات أولاً ثم الصوت، وبجودة تنازلية
        streams.sortWith(compareBy({ it.type != "VIDEO" }, { -(it.qualityLabel?.let { parseQuality(it) } ?: 0) }, { -it.bitrate }))

        return VideoInfo(videoId, title, author, lengthSeconds, thumbnail, streams)
    }

    private fun parseFormat(fmt: JsonObject): Stream? {
        val itag = getInt(fmt, "itag")
        val mimeType = getString(fmt, "mimeType") ?: return null
        val type = if (mimeType.startsWith("audio/", ignoreCase = true)) "AUDIO" else "VIDEO"
        val qualityLabel = getString(fmt, "qualityLabel")
        val bitrate = getLong(fmt, "bitrate")
        val width = getInt(fmt, "width")
        val height = getInt(fmt, "height")
        val fileSize = getLongNullable(fmt, "contentLength")
        val url = getString(fmt, "url")
        val cipher = getString(fmt, "signatureCipher")
        return Stream(itag, mimeType, type, qualityLabel, bitrate, width, height, fileSize, url, cipher)
    }

    private fun parseQuality(stream: Stream): Int {
        val label = stream.qualityLabel ?: return 0
        return when {
            label.contains("1440") -> 1440
            label.contains("1080") -> 1080
            label.contains("720") -> 720
            label.contains("480") -> 480
            label.contains("360") -> 360
            label.contains("240") -> 240
            label.contains("144") -> 144
            else -> 0
        }
    }

    private fun getString(obj: JsonObject, key: String): String? {
        val e = obj.get(key)
        return if (e == null || e.isJsonNull) null else e.asString
    }

    private fun getInt(obj: JsonObject, key: String): Int {
        val e = obj.get(key)
        return if (e == null || e.isJsonNull) 0 else e.asInt
    }

    private fun getLong(obj: JsonObject, key: String): Long {
        val e = obj.get(key)
        return if (e == null || e.isJsonNull) 0L else e.asLong
    }

    private fun getLongNullable(obj: JsonObject, key: String): Long? {
        val e = obj.get(key)
        return if (e == null || e.isJsonNull) null else e.asLong
    }

    private fun extractThumbnail(details: JsonObject): String? {
        val thumb = details.getAsJsonObject("thumbnail")
        if (thumb == null) return null
        val thumbnails = thumb.getAsJsonArray("thumbnails")
        if (thumbnails == null || thumbnails.size == 0) return null
        // اختر أكبر صورة مصغرة متاحة
        var best: JsonElement? = null
        var bestW = 0
        for (i in 0 until thumbnails.size()) {
            val t = thumbnails.get(i).asJsonObject
            val w = getInt(t, "width")
            if (w >= bestW) {
                bestW = w
                best = t
            }
        }
        return best?.let { getString(it.asJsonObject, "url") }
    }
}
