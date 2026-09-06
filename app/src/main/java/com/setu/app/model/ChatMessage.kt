package com.setu.app.model

/**
 * Represents a single chat message in the conversation.
 *
 * @param id Unique ID (timestamp-based)
 * @param originalText The text as typed/sent by the sender (their language)
 * @param displayText The translated text shown to the local user (their language)
 * @param isSent true = message was sent by THIS device, false = received from remote
 * @param timestamp Unix epoch ms
 * @param isTranslated Whether displayText was auto-translated
 * @param isEncrypted E2EE status badge indicator
 * @param latencyMs On-Device NPU translation latency in milliseconds
 * @param isQuickReply Whether message was sent via Monster Mode instant phrase
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val originalText: String,
    val displayText: String,
    val isSent: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val isTranslated: Boolean = false,
    val isEncrypted: Boolean = true,
    val latencyMs: Long = -1L,
    val isQuickReply: Boolean = false
)

object WireProtocol {

    const val PREFIX_TEXT = "TXT:"
    const val PREFIX_SYSTEM = "SYS:"
    const val PREFIX_QUICK_REPLY = "QRP:"
    const val SYS_LANG_PREFIX = "SYS:LANG:"

    fun encodeText(text: String): String = "$PREFIX_TEXT$text"
    fun encodeLang(langCode: String): String = "$SYS_LANG_PREFIX$langCode"
    fun encodeQuickReply(key: String): String = "$PREFIX_QUICK_REPLY$key"

    fun decode(raw: String): WireMessage? = when {
        raw.startsWith(PREFIX_TEXT) -> WireMessage.Text(raw.removePrefix(PREFIX_TEXT))
        raw.startsWith(SYS_LANG_PREFIX) -> WireMessage.Language(raw.removePrefix(SYS_LANG_PREFIX).trim())
        raw.startsWith(PREFIX_QUICK_REPLY) -> WireMessage.QuickReply(raw.removePrefix(PREFIX_QUICK_REPLY).trim())
        raw.startsWith(PREFIX_SYSTEM) -> WireMessage.System(raw.removePrefix(PREFIX_SYSTEM).trim())
        else -> WireMessage.Text(raw)
    }
}

sealed class WireMessage {
    data class Text(val content: String) : WireMessage()
    data class Language(val langCode: String) : WireMessage()
    data class QuickReply(val key: String) : WireMessage()
    data class System(val event: String) : WireMessage()
}

object QuickReplies {
    val phrases: List<Triple<String, String, String>> = listOf(
        Triple("safe", "I'm safe", "मैं सुरक्षित हूँ"),
        Triple("help", "Need help urgently!", "मुझे तुरंत मदद चाहिए!"),
        Triple("otw", "On my way to you", "मैं आपके पास आ रहा हूँ"),
        Triple("meetpoint", "Go to designated meeting point", "निर्धारित मिलन स्थल पर जाएँ"),
        Triple("ok", "Understood / Confirmed", "समझ गया / पुष्टि की गई"),
        Triple("wait", "Standby / Wait for me", "प्रतीक्षा करें / मेरा इंतज़ार करें")
    )

    fun getEnglish(key: String): String = phrases.firstOrNull { it.first == key }?.second ?: key
    fun getHindi(key: String): String = phrases.firstOrNull { it.first == key }?.third ?: key

    fun getLocalized(key: String, lang: String): String {
        return if (lang == "hi") getHindi(key) else getEnglish(key)
    }
}
