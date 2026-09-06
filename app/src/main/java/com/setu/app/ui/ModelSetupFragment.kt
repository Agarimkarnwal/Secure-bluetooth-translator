package com.setu.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.setu.app.databinding.FragmentModelSetupBinding
import kotlinx.coroutines.launch

/**
 * ModelSetupFragment — one-time setup screen to download ML Kit models.
 *
 * This screen is shown before the demo (with internet available).
 * At demo time, models are already on-device and this screen is skipped.
 *
 * Shows clear status: "Models ready ✓" or "Downloading... (Wi-Fi required)".
 */
class ModelSetupFragment : Fragment() {

    private var _binding: FragmentModelSetupBinding? = null
    private val binding get() = _binding!!

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentModelSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkStatus()

        binding.btnDownloadModels.setOnClickListener {
            downloadModels()
        }

        binding.btnDeleteModels.setOnClickListener {
            deleteModels()
        }
    }

    private fun checkStatus() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvStatus.text = "Checking model status…"

            try {
                val status = mainActivity.modelDownloadManager.checkModelsDownloaded()
                updateUI(status.englishDownloaded, status.hindiDownloaded)
            } catch (e: Exception) {
                binding.tvStatus.text = "Error checking status: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun updateUI(enReady: Boolean, hiReady: Boolean) {
        val enIcon = if (enReady) "✅" else "❌"
        val hiIcon = if (hiReady) "✅" else "❌"

        binding.tvStatus.text = buildString {
            appendLine("Model Status:")
            appendLine("$enIcon  English model")
            appendLine("$hiIcon  Hindi model")
            appendLine()
            if (enReady && hiReady) {
                appendLine("🎉 All models ready! The app will work fully offline.")
            } else {
                appendLine("⚠️ Missing models. Connect to Wi-Fi and tap Download.")
                appendLine("This is a ONE-TIME setup — not needed during the demo.")
            }
        }

        binding.btnDownloadModels.isEnabled = !enReady || !hiReady
        binding.btnDownloadModels.alpha = if (!enReady || !hiReady) 1f else 0.5f
    }

    private fun downloadModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.btnDownloadModels.isEnabled = false
            binding.progressBar.visibility = View.VISIBLE
            binding.tvStatus.text = "Downloading language models over Wi-Fi…\nThis may take a minute."

            try {
                mainActivity.modelDownloadManager.downloadMissingModels(
                    wifiOnly = true,
                    onProgress = { done, total ->
                        binding.tvStatus.text = "Downloading model $done/$total…"
                    }
                )
                binding.tvStatus.text = "✅ Download complete! Models are now on-device.\nYou can go offline."
                checkStatus()
            } catch (e: Exception) {
                binding.tvStatus.text = "Download failed: ${e.message}\n\nMake sure Wi-Fi is connected."
                binding.btnDownloadModels.isEnabled = true
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    private fun deleteModels() {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvStatus.text = "Deleting models…"
            try {
                mainActivity.modelDownloadManager.deleteAllModels()
                binding.tvStatus.text = "Models deleted. You'll need to re-download before going offline."
                checkStatus()
            } catch (e: Exception) {
                binding.tvStatus.text = "Error: ${e.message}"
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
