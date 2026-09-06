package com.setu.app.bluetooth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String
)

object DebugLogManager {

    private const val MAX_LOGS = 150
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val logList = mutableListOf<LogEntry>()

    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    @Synchronized
    fun log(tag: String, message: String) {
        val entry = LogEntry(
            timestamp = timeFormat.format(Date()),
            tag = tag,
            message = message
        )
        if (logList.size >= MAX_LOGS) {
            logList.removeAt(0)
        }
        logList.add(entry)
        _logsFlow.value = ArrayList(logList)
        android.util.Log.d("SetuDebug [$tag]", message)
    }

    @Synchronized
    fun clear() {
        logList.clear()
        _logsFlow.value = emptyList()
    }
}
