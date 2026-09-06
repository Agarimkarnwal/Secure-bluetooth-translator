package com.setu.app.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.setu.app.R
import com.setu.app.adapter.ChatAdapter
import com.setu.app.bluetooth.BluetoothConnectionManager
import com.setu.app.bluetooth.ConnectionListener
import com.setu.app.bluetooth.DebugLogManager
import com.setu.app.bluetooth.PermissionUtils
import com.setu.app.databinding.FragmentChatBinding
import com.setu.app.model.ChatMessage
import com.setu.app.model.QuickReplies
import com.setu.app.model.WireProtocol
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Locale

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val mainActivity get() = activity as MainActivity
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private var myRole = "HOST"
    private var deviceName = "Remote"
    private var currentPingMs = -1L
    private var lastNpuLatencyMs = -1L

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spokenText.isNullOrBlank()) {
            binding.etMessage.setText(spokenText)
            binding.etMessage.setSelection(spokenText.length)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!PermissionUtils.hasConnectPermission(requireContext())) {
            Toast.makeText(requireContext(), "Bluetooth permissions required", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        myRole = arguments?.getString("role") ?: "HOST"
        deviceName = arguments?.getString("deviceName") ?: "Remote"

        setupRecyclerView()
        setupConnectionStatus()
        setupButtons()
        setupQuickReplyChips()
        observeIncomingMessages()
        observeConnectionState()
        observeDiagnostics()

        if (myRole == "HOST") {
            setupHostServer()
        } else {
            sendLanguageHandshake()
        }
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        binding.rvMessages.apply {
            layoutManager = LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupConnectionStatus() {
        updateStatusBar(BluetoothConnectionManager.ConnectionState.CONNECTING)
        val langLabel = if (mainActivity.myLanguage == "en") "English" else "Hindi"
        binding.tvMyLanguage.text = "My language: $langLabel"
    }

    private fun setupButtons() {
        binding.btnSend.setOnClickListener { sendTextMessage() }
        binding.btnMonsterMode.setOnClickListener { showMonsterModeSheet() }
        binding.btnVoice.setOnClickListener { startVoiceInput() }
        binding.btnDebugConsole.setOnClickListener { showDebugLogsDialog() }

        binding.btnBack.setOnClickListener {
            mainActivity.btManager.disconnect()
            findNavController().navigateUp()
        }
    }

    private fun setupQuickReplyChips() {
        binding.chipSafe.setOnClickListener { sendQuickReply("safe") }
        binding.chipHelp.setOnClickListener { sendQuickReply("help") }
        binding.chipOtw.setOnClickListener { sendQuickReply("otw") }
        binding.chipMeet.setOnClickListener { sendQuickReply("meetpoint") }
        binding.chipOk.setOnClickListener { sendQuickReply("ok") }
    }

    private fun setupHostServer() {
        addSystemMessage("Waiting for client phone to connect...")

        mainActivity.btManager.listener = object : ConnectionListener {
            override fun onConnected(deviceName: String) {
                updateStatusBar(BluetoothConnectionManager.ConnectionState.CONNECTED)
                this@ChatFragment.deviceName = deviceName
                binding.tvDeviceName.text = "Connected to: $deviceName"
                addSystemMessage("✅ E2EE Connected to $deviceName")
                sendLanguageHandshake()
                vibrateDevice()
            }

            override fun onMessageReceived(raw: String) { }

            override fun onConnectionLost() {
                updateStatusBar(BluetoothConnectionManager.ConnectionState.DISCONNECTED)
                addSystemMessage("❌ Connection lost")
            }

            override fun onConnectionFailed(reason: String) {
                updateStatusBar(BluetoothConnectionManager.ConnectionState.DISCONNECTED)
                addSystemMessage("⚠️ Connection failed: $reason")
            }
        }

        mainActivity.btManager.startServer()
    }

    private fun sendTextMessage() {
        val text = binding.etMessage.text.toString().trim()
        if (text.isEmpty()) return

        val wireMsg = WireProtocol.encodeText(text)
        val success = mainActivity.btManager.sendMessage(wireMsg)

        if (success) {
            binding.etMessage.setText("")
            val msg = ChatMessage(
                originalText = text,
                displayText = text,
                isSent = true,
                isTranslated = false,
                isEncrypted = true
            )
            appendMessage(msg)
            vibrateDevice()
        } else {
            Toast.makeText(requireContext(), "Send failed — check connection", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendLanguageHandshake() {
        val handshake = WireProtocol.encodeLang(mainActivity.myLanguage)
        mainActivity.btManager.sendMessage(handshake)
    }

    private fun sendQuickReply(key: String) {
        val wireMsg = WireProtocol.encodeQuickReply(key)
        val localizedText = QuickReplies.getLocalized(key, mainActivity.myLanguage)
        val success = mainActivity.btManager.sendMessage(wireMsg)

        if (success) {
            val msg = ChatMessage(
                originalText = localizedText,
                displayText = localizedText,
                isSent = true,
                isTranslated = false,
                isEncrypted = true,
                isQuickReply = true
            )
            appendMessage(msg)
            vibrateDevice()
        }
    }

    private fun observeIncomingMessages() {
        mainActivity.btManager.incomingMessages
            .onEach { raw -> handleIncomingMessage(raw) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun handleIncomingMessage(raw: String) {
        when (val parsed = WireProtocol.decode(raw)) {
            is com.setu.app.model.WireMessage.Language -> {
                mainActivity.remoteLanguage = parsed.langCode
                val label = if (parsed.langCode == "en") "English" else "Hindi"
                addSystemMessage("ℹ️ Remote phone is using $label")
            }

            is com.setu.app.model.WireMessage.Text -> {
                translateAndAppend(
                    originalText = parsed.content,
                    sourceLang = mainActivity.remoteLanguage,
                    targetLang = mainActivity.myLanguage,
                    isQuickReply = false
                )
                vibrateDevice()
            }

            is com.setu.app.model.WireMessage.QuickReply -> {
                val phraseText = QuickReplies.getLocalized(parsed.key, mainActivity.myLanguage)
                val msg = ChatMessage(
                    originalText = phraseText,
                    displayText = phraseText,
                    isSent = false,
                    isTranslated = false,
                    isEncrypted = true,
                    isQuickReply = true
                )
                appendMessage(msg)
                vibrateDevice()
            }

            is com.setu.app.model.WireMessage.System -> {
                addSystemMessage("[SYS] ${parsed.event}")
            }
        }
    }

    private fun translateAndAppend(
        originalText: String,
        sourceLang: String,
        targetLang: String,
        isQuickReply: Boolean
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = try {
                mainActivity.translationManager.translateWithResult(originalText, sourceLang, targetLang)
            } catch (e: Exception) {
                com.setu.app.translation.TranslationResult(
                    translatedText = "$originalText\n⚠️ Model not ready — run Model Setup first",
                    latencyMs = -1L
                )
            }

            lastNpuLatencyMs = result.latencyMs
            updateDiagnosticsHud()

            val isActuallyTranslated = result.translatedText != originalText
            val msg = ChatMessage(
                originalText = originalText,
                displayText = result.translatedText,
                isSent = false,
                isTranslated = isActuallyTranslated,
                isEncrypted = true,
                latencyMs = result.latencyMs,
                isQuickReply = isQuickReply
            )
            appendMessage(msg)
        }
    }

    private fun observeConnectionState() {
        mainActivity.btManager.connectionState
            .onEach { state -> updateStatusBar(state) }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun observeDiagnostics() {
        mainActivity.btManager.pingLatencyMs
            .onEach { latency ->
                currentPingMs = latency
                updateDiagnosticsHud()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        mainActivity.translationManager.lastLatencyMs
            .onEach { npu ->
                lastNpuLatencyMs = npu
                updateDiagnosticsHud()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
    }

    private fun updateDiagnosticsHud() {
        val pingStr = if (currentPingMs > 0) "${currentPingMs}ms" else "--ms"
        val npuStr = if (lastNpuLatencyMs > 0) "${lastNpuLatencyMs}ms" else "Ready"
        binding.tvDiagnosticsHud.text = "🔒 E2EE: AES-256 • Ping: $pingStr • Hexagon NPU: $npuStr"
    }

    private fun updateStatusBar(state: BluetoothConnectionManager.ConnectionState) {
        val (statusText, colorRes) = when (state) {
            BluetoothConnectionManager.ConnectionState.CONNECTED ->
                "🟢 Connected to $deviceName" to R.color.status_connected
            BluetoothConnectionManager.ConnectionState.CONNECTING,
            BluetoothConnectionManager.ConnectionState.LISTENING ->
                "🟡 Connecting…" to R.color.status_connecting
            else ->
                "🔴 Disconnected" to R.color.status_disconnected
        }
        binding.tvConnectionStatus.text = statusText
        binding.statusBar.setBackgroundColor(requireContext().getColor(colorRes))
        binding.tvDeviceName.text = if (state == BluetoothConnectionManager.ConnectionState.CONNECTED)
            "Connected to: $deviceName" else ""
    }

    private fun showMonsterModeSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetStyle)
        val sheetView = LayoutInflater.from(requireContext())
            .inflate(R.layout.bottom_sheet_monster_mode, null)

        val chipGroup = sheetView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupReplies)

        QuickReplies.phrases.forEach { (key, english, hindi) ->
            val phrase = if (mainActivity.myLanguage == "hi") hindi else english
            val chip = Chip(requireContext()).apply {
                text = phrase
                isClickable = true
                setChipBackgroundColorResource(R.color.accent_orange)
                setTextColor(resources.getColor(R.color.text_primary, null))
            }
            chip.setOnClickListener {
                sendQuickReply(key)
                dialog.dismiss()
            }
            chipGroup.addView(chip)
        }

        dialog.setContentView(sheetView)
        dialog.show()
    }

    private fun showDebugLogsDialog() {
        val logsView = TextView(requireContext()).apply {
            textSize = 11sp
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xFF101014.toInt())
            setTextColor(0xFF00FFCC.toInt())
            fontFeatureSettings = "monospace"
        }

        val scrollView = ScrollView(requireContext()).apply {
            addView(logsView)
        }

        val logsText = DebugLogManager.logsFlow.value.joinToString("\n") {
            "[${it.timestamp}] [${it.tag}] ${it.message}"
        }
        logsView.text = if (logsText.isBlank()) "No logs yet..." else logsText

        AlertDialog.Builder(requireContext())
            .setTitle("🛠 Bluetooth & Security Debug Logs")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .setNeutralButton("Clear") { _, _ -> DebugLogManager.clear() }
            .show()
    }

    private fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (mainActivity.myLanguage == "en") Locale.ENGLISH else Locale("hi", "IN"))
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your message...")
        }

        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Voice input unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun vibrateDevice() {
        try {
            val vibrator = requireContext().getSystemService(Context.Vibrator_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    private fun appendMessage(msg: ChatMessage) {
        messages.add(msg)
        chatAdapter.submitList(messages.toList())
        binding.rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun addSystemMessage(text: String) {
        val msg = ChatMessage(
            originalText = text,
            displayText = text,
            isSent = false,
            isTranslated = false,
            isEncrypted = false
        )
        appendMessage(msg)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
