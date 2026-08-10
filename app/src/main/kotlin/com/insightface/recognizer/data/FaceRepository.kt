package com.insightface.recognizer.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import androidx.annotation.UiThread
import com.insightface.sdk.inspireface.InspireFace
import com.insightface.sdk.inspireface.base.FaceFeature
import com.insightface.sdk.inspireface.base.FaceFeatureIdentity
import com.insightface.sdk.inspireface.base.FeatureHubConfiguration
import com.insightface.sdk.inspireface.base.SearchTopKResults
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * Local face library built on the official InspireFace **FeatureHub** (the SDK's embedded
 * vector database). Each registered face = a [FaceFeature] inserted into the native hub
 * (searchable by cosine similarity) + a name/crop stored in app-private metadata.
 *
 * This is a Kotlin port of the official Android example's `FaceRepository`, using the exact
 * same FeatureHub API:
 *  - [InspireFace.CreateFeatureHubConfiguration] / [InspireFace.FeatureHubDataEnable]
 *  - [InspireFace.FeatureHubFaceSearchTopK] (1:N search)
 *  - [InspireFace.FeatureHubInsertFeature] / [InspireFace.FeatureHubFaceUpdate] /
 *    [InspireFace.FeatureHubFaceRemove] / [InspireFace.FeatureHubGetFaceIdentity]
 *
 * The native DB lives at `files/face_hub/<model>/features.db`; crops under
 * `files/face_hub/<model>/crops/`. Must be opened/closed on the SDK executor thread.
 */
class FaceRepository(
    context: Context,
    private val modelName: String,
) {

    data class FaceRecord(
        val id: Long,
        val name: String,
        val cropPath: String,
        val updatedAt: Long,
    )

    data class SearchResult(
        val matched: Boolean,
        val record: FaceRecord?,
        val confidence: Float,
        val threshold: Float,
    )

    data class InsertResult(val success: Boolean, val record: FaceRecord?)

    private val app: Context = context.applicationContext
    private val modelDir = File(File(app.filesDir, "face_hub"), modelName)
    private val cropDir = File(modelDir, "crops")
    private val dbFile = File(modelDir, "features.db")
    private val metadata: SharedPreferences =
        app.getSharedPreferences("face_records_$modelName", Context.MODE_PRIVATE)

    @Volatile private var hubAcquired = false

    /** Opens the native FeatureHub with persistence. Call on the SDK executor thread. */
    fun open(): Boolean {
        if (hubAcquired) return true
        if (!cropDir.exists() && !cropDir.mkdirs()) return false
        val config: FeatureHubConfiguration = InspireFace.CreateFeatureHubConfiguration()
            .setPrimaryKeyMode(InspireFace.PK_MANUAL_INPUT)
            .setEnablePersistence(true)
            .setPersistenceDbPath(dbFile.absolutePath)
            .setSearchThreshold(InspireFace.GetRecommendedCosineThreshold())
            .setSearchMode(InspireFace.SEARCH_MODE_EXHAUSTIVE)
        if (!InspireFace.FeatureHubDataEnable(config)) return false
        hubAcquired = true
        return true
    }

    fun close() {
        if (!hubAcquired) return
        InspireFace.FeatureHubDataDisable()
        hubAcquired = false
    }

    val isOpen: Boolean get() = hubAcquired

    /** Lists all registered faces, optionally filtered by [keyword] (name or id). */
    fun query(keyword: String? = null): List<FaceRecord> {
        val norm = keyword?.trim()?.lowercase(Locale.ROOT) ?: ""
        val out = ArrayList<FaceRecord>()
        for ((key, value) in metadata.all) {
            if (!key.startsWith(KEY_PREFIX) || value !is String) continue
            val rec = decode(value) ?: continue
            if (norm.isEmpty() || rec.name.lowercase(Locale.ROOT).contains(norm)
                || rec.id.toString().contains(norm)
            ) out.add(rec)
        }
        out.sortByDescending { it.updatedAt }
        return out
    }

    fun get(id: Long): FaceRecord? = decode(metadata.getString(key(id), null))

    /** 1:N search against the FeatureHub. Call on the SDK executor thread. */
    fun search(feature: FaceFeature, threshold: Float = InspireFace.GetRecommendedCosineThreshold()): SearchResult {
        if (!hubAcquired) return SearchResult(false, null, Float.NaN, threshold)
        val results: SearchTopKResults = InspireFace.FeatureHubFaceSearchTopK(feature, 1)
        if (results == null || results.num <= 0 || results.ids == null || results.confidence == null
            || results.ids.isEmpty() || results.confidence.isEmpty()
        ) return SearchResult(false, null, Float.NaN, threshold)
        val confidence = results.confidence[0]
        val record = get(results.ids[0])
        return SearchResult(record != null && confidence >= threshold, record, confidence, threshold)
    }

    /** Registers a new face. Call on the SDK executor thread. */
    fun insert(name: String, feature: FaceFeature, crop: Bitmap): InsertResult {
        if (!hubAcquired) return InsertResult(false, null)
        var id = metadata.getLong(KEY_NEXT_ID, 1L).coerceAtLeast(1L)
        while (metadata.contains(key(id))) id++
        val cropFile = cropFile(id)
        if (!saveCrop(cropFile, crop)) return InsertResult(false, null)
        val identity = FaceFeatureIdentity.create(id, feature)
        if (!InspireFace.FeatureHubInsertFeature(identity)) {
            cropFile.delete()
            return InsertResult(false, null)
        }
        val record = FaceRecord(id, normalizeName(name, id), cropFile.absolutePath, System.currentTimeMillis())
        val saved = metadata.edit()
            .putString(key(id), encode(record))
            .putLong(KEY_NEXT_ID, id + 1)
            .commit()
        if (!saved) {
            InspireFace.FeatureHubFaceRemove(id)
            cropFile.delete()
            return InsertResult(false, null)
        }
        return InsertResult(true, record)
    }

    /** Renames a face (metadata-only) or replaces feature+crop when provided. */
    fun update(id: Long, name: String, feature: FaceFeature? = null, crop: Bitmap? = null): Boolean {
        val old = get(id) ?: return false
        if (!hubAcquired) return false
        val cropFile = File(old.cropPath)
        if (crop != null && !saveCrop(cropFile, crop)) return false
        if (feature != null) {
            if (!InspireFace.FeatureHubFaceUpdate(FaceFeatureIdentity.create(id, feature))) return false
        }
        val updated = FaceRecord(id, normalizeName(name, id), cropFile.absolutePath, System.currentTimeMillis())
        val saved = metadata.edit().putString(key(id), encode(updated)).commit()
        if (!saved) return false
        return true
    }

    fun delete(id: Long): Boolean {
        val record = get(id) ?: return false
        if (!hubAcquired) return false
        val crop = File(record.cropPath)
        InspireFace.FeatureHubFaceRemove(id)
        metadata.edit().remove(key(id)).commit()
        if (crop.exists()) crop.delete()
        return true
    }

    private fun cropFile(id: Long) = File(cropDir, "$id.jpg")

    private fun saveCrop(dest: File, bitmap: Bitmap): Boolean {
        val parent = dest.parentFile
        if (parent != null && !parent.exists() && !parent.mkdirs()) return false
        return try {
            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
                out.fd.sync()
            }
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun normalizeName(name: String, id: Long): String {
        val trimmed = name.trim()
        return if (trimmed.isEmpty()) "人脸 $id" else trimmed
    }

    private fun key(id: Long) = KEY_PREFIX + id

    private fun encode(record: FaceRecord): String = try {
        JSONObject()
            .put("id", record.id)
            .put("name", record.name)
            .put("crop", record.cropPath)
            .put("updated", record.updatedAt)
            .toString()
    } catch (e: JSONException) {
        throw IllegalStateException(e)
    }

    private fun decode(value: String?): FaceRecord? {
        if (value == null) return null
        return try {
            val json = JSONObject(value)
            FaceRecord(
                json.getLong("id"),
                json.getString("name"),
                json.getString("crop"),
                json.getLong("updated"),
            )
        } catch (e: JSONException) {
            null
        }
    }

    private companion object {
        const val KEY_PREFIX = "record."
        const val KEY_NEXT_ID = "next_id"
    }
}
