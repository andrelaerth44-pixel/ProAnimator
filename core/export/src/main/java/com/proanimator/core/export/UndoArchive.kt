package com.proanimator.core.export

import android.graphics.Bitmap
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

/**
 * Eternal undo — serialize recent layer snapshots into PAN meta / UNDO chunk.
 *
 * Stores last N bitmaps as PNG base64 so load restores an in-session undo stack.
 * Not infinite history (that would explode file size); depth is configurable.
 */
object UndoArchive {

    const val DEFAULT_DEPTH = 8

    data class Snapshot(
        val frameIndex: Int,
        val pngBase64: String
    )

    fun encode(snapshots: List<Pair<Int, Bitmap>>, maxDepth: Int = DEFAULT_DEPTH): JSONObject {
        val arr = JSONArray()
        snapshots.takeLast(maxDepth).forEach { (frame, bmp) ->
            val baos = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.PNG, 90, baos)
            val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
            arr.put(JSONObject().apply {
                put("frame", frame)
                put("png", b64)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("depth", arr.length())
            put("snapshots", arr)
        }
    }

    fun decode(json: JSONObject?): List<Snapshot> {
        if (json == null) return emptyList()
        val arr = json.optJSONArray("snapshots") ?: return emptyList()
        val out = mutableListOf<Snapshot>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(Snapshot(o.getInt("frame"), o.getString("png")))
        }
        return out
    }

    fun decodeBitmap(pngBase64: String): Bitmap? {
        return try {
            val bytes = Base64.decode(pngBase64, Base64.NO_WRAP)
            android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }
}
