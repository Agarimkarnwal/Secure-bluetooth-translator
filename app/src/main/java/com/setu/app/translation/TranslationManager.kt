package com.setu.app.translation

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import com.setu.app.bluetooth.DebugLogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationResult(
    val translatedText: String,
    val latencyMs: Long
)

/**
 * TranslationManager — wraps ML Kit on-device translation for Hindi ↔ English.
 *
 * Performance Metrics:
 *  - Tracks execution latency (ms) for NPU / On-Device ML verification.
 *  - Exposes last TranslationResult with timing metadata.
 */
class TranslationManager {

    companion object {
        private const val TAG = "SetuTranslation"

        const val LANG_ENGLISH = TranslateLanguage.ENGLISH
        const val LANG_HINDI = TranslateLanguage.HINDI
    }

    private val _lastLatencyMs = MutableStateFlow<Long>(-1L)
    val lastLatencyMs: StateFlow<Long> = _lastLatencyMs

    private val translatorEnToHi: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(LANG_ENGLISH)
                .setTargetLanguage(LANG_HINDI)
                .build()
        )
    }

    private val translatorHiToEn: Translator by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(LANG_HINDI)
                .setTargetLanguage(LANG_ENGLISH)
                .build()
        )
    }

    suspend fun translateWithResult(text: String, sourceLang: String, targetLang: String): TranslationResult {
        if (sourceLang == targetLang || text.isBlank()) {
            return TranslationResult(text, 0L)
        }

        val startTime = System.currentTimeMillis()
        val translator = when {
            sourceLang == LANG_ENGLISH && targetLang == LANG_HINDI -> translatorEnToHi
            sourceLang == LANG_HINDI && targetLang == LANG_ENGLISH -> translatorHiToEn
            else -> throw IllegalArgumentException("Unsupported language pair: $sourceLang → $targetLang")
        }

        val translated = suspendCancellableCoroutine<String> { cont ->
            translator.translate(text)
                .addOnSuccessListener { result ->
                    cont.resume(result)
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }

        val latency = System.currentTimeMillis() - startTime
        _lastLatencyMs.value = latency
        DebugLogManager.log("NPU_TRANSLATE", "Translated in ${latency}ms: \"$text\" -> \"$translated\"")

        return TranslationResult(translated, latency)
    }

    suspend fun translate(text: String, sourceLang: String, targetLang: String): String {
        return translateWithResult(text, sourceLang, targetLang).translatedText
    }

    suspend fun downloadModels(allowMobileData: Boolean = false) {
        val conditions = DownloadConditions.Builder()
            .apply { if (!allowMobileData) requireWifi() }
            .build()

        listOf(translatorEnToHi, translatorHiToEn).forEach { translator ->
            suspendCancellableCoroutine { cont ->
                translator.downloadModelIfNeeded(conditions)
                    .addOnSuccessListener { cont.resume(Unit) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
        }
    }

    fun release() {
        try { translatorEnToHi.close() } catch (_: Exception) {}
        try { translatorHiToEn.close() } catch (_: Exception) {}
    }
}
