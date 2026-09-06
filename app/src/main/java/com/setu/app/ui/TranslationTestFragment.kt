package com.setu.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.setu.app.databinding.FragmentTranslationTestBinding
import com.setu.app.translation.TranslationManager
import kotlinx.coroutines.launch

/**
 * TranslationTestFragment — isolated test screen for Step 2.
 *
 * Purpose: verify on-device translation works with Wi-Fi/data OFF
 * BEFORE wiring it into the chat pipeline.
 *
 * The user types text, picks direction (EN→HI or HI→EN), and taps Translate.
 * Output appears in a result box. This screen is accessible from the Start screen.
 */
class TranslationTestFragment : Fragment() {

    private var _binding: FragmentTranslationTestBinding? = null
    private val binding get() = _binding!!

    private val mainActivity get() = activity as MainActivity
    private var translateEnToHi = true  // direction toggle

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTranslationTestBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        updateDirectionLabel()

        binding.btnSwapDirection.setOnClickListener {
            translateEnToHi = !translateEnToHi
            updateDirectionLabel()
        }

        binding.btnTranslate.setOnClickListener {
            val inputText = binding.etInput.text.toString().trim()
            if (inputText.isEmpty()) {
                Toast.makeText(requireContext(), "Enter some text first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            performTranslation(inputText)
        }

        // Pre-fill a sample sentence
        binding.etInput.setText("Hello, how are you?")
    }

    private fun updateDirectionLabel() {
        binding.tvDirection.text = if (translateEnToHi) {
            "Direction: English → Hindi"
        } else {
            "Direction: Hindi → English"
        }
    }

    private fun performTranslation(text: String) {
        binding.btnTranslate.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE
        binding.tvOutput.text = "Translating…"
        binding.tvTiming.text = ""

        val src = if (translateEnToHi) TranslationManager.LANG_ENGLISH else TranslationManager.LANG_HINDI
        val tgt = if (translateEnToHi) TranslationManager.LANG_HINDI else TranslationManager.LANG_ENGLISH

        val start = System.currentTimeMillis()

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val result = mainActivity.translationManager.translate(text, src, tgt)
                val elapsed = System.currentTimeMillis() - start

                binding.tvOutput.text = result
                binding.tvTiming.text = "⚡ Translated on-device in ${elapsed}ms (no internet used)"
            } catch (e: Exception) {
                binding.tvOutput.text = "❌ Error: ${e.message}"
                binding.tvTiming.text = "If models aren't downloaded, go to Model Setup first."
            } finally {
                binding.btnTranslate.isEnabled = true
                binding.progressBar.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
