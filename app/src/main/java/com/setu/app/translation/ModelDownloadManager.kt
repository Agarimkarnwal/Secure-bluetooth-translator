package com.setu.app.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ModelDownloadManager — checks and triggers ML Kit model downloads.
 *
 * This is called ONCE during the "Model Setup" screen (before the demo, with
 * internet access). At demo time the models are already on-device and
 * TranslationManager works completely offline.
 */
class ModelDownloadManager {

    private val remoteModelManager = RemoteModelManager.getInstance()

    data class ModelStatus(
        val englishDownloaded: Boolean,
        val hindiDownloaded: Boolean
    ) {
        val allReady: Boolean get() = englishDownloaded && hindiDownloaded
    }

    /** Checks if both EN and HI models are already downloaded. */
    suspend fun checkModelsDownloaded(): ModelStatus {
        val enDownloaded = isModelDownloaded(TranslateLanguage.ENGLISH)
        val hiDownloaded = isModelDownloaded(TranslateLanguage.HINDI)
        return ModelStatus(enDownloaded, hiDownloaded)
    }

    private suspend fun isModelDownloaded(languageCode: String): Boolean {
        val model = TranslateRemoteModel.Builder(languageCode).build()
        return suspendCancellableCoroutine { cont ->
            remoteModelManager.isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded -> cont.resume(isDownloaded) }
                .addOnFailureListener { cont.resume(false) }
        }
    }

    /**
     * Downloads whichever models are missing.
     * @param wifiOnly If true (default), only downloads on Wi-Fi.
     * @param onProgress Callback with (downloaded, total) counts.
     */
    suspend fun downloadMissingModels(
        wifiOnly: Boolean = true,
        onProgress: (downloaded: Int, total: Int) -> Unit = { _, _ -> }
    ) {
        val status = checkModelsDownloaded()
        val toDownload = mutableListOf<String>()
        if (!status.englishDownloaded) toDownload.add(TranslateLanguage.ENGLISH)
        if (!status.hindiDownloaded) toDownload.add(TranslateLanguage.HINDI)

        val conditions = DownloadConditions.Builder()
            .apply { if (wifiOnly) requireWifi() }
            .build()

        toDownload.forEachIndexed { index, lang ->
            onProgress(index, toDownload.size)
            val model = TranslateRemoteModel.Builder(lang).build()
            suspendCancellableCoroutine { cont ->
                remoteModelManager.download(model, conditions)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
        }
        onProgress(toDownload.size, toDownload.size)
    }

    /** Deletes all downloaded translate models (for debugging/reset). */
    suspend fun deleteAllModels() {
        for (lang in listOf(TranslateLanguage.ENGLISH, TranslateLanguage.HINDI)) {
            val model = TranslateRemoteModel.Builder(lang).build()
            suspendCancellableCoroutine { cont ->
                remoteModelManager.deleteDownloadedModel(model)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { cont.resume(Unit) } // ignore if not present
            }
        }
    }
}
