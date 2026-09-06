package com.setu.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.setu.app.R
import com.setu.app.bluetooth.PermissionUtils
import com.setu.app.databinding.FragmentStartBinding
import com.setu.app.translation.TranslationManager

class StartFragment : Fragment() {

    private var _binding: FragmentStartBinding? = null
    private val binding get() = _binding!!

    private var selectedRole: String? = null
    private var selectedLang: String = TranslationManager.LANG_ENGLISH

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            proceedAfterPermissions()
        } else {
            Toast.makeText(
                requireContext(),
                "Bluetooth permissions are required for Setu to work",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnHost.setOnClickListener {
            selectedRole = "HOST"
            binding.btnHost.isSelected = true
            binding.btnJoiner.isSelected = false
            updateContinueButton()
        }

        binding.btnJoiner.setOnClickListener {
            selectedRole = "JOINER"
            binding.btnJoiner.isSelected = true
            binding.btnHost.isSelected = false
            updateContinueButton()
        }

        binding.btnLangEnglish.setOnClickListener {
            selectedLang = TranslationManager.LANG_ENGLISH
            binding.btnLangEnglish.isSelected = true
            binding.btnLangHindi.isSelected = false
        }
        binding.btnLangHindi.setOnClickListener {
            selectedLang = TranslationManager.LANG_HINDI
            binding.btnLangHindi.isSelected = true
            binding.btnLangEnglish.isSelected = false
        }

        binding.btnLangEnglish.isSelected = true

        binding.btnContinue.setOnClickListener {
            checkAndRequestPermissions()
        }

        binding.btnModelSetup.setOnClickListener {
            findNavController().navigate(R.id.action_start_to_modelSetup)
        }

        binding.btnTranslationTest.setOnClickListener {
            findNavController().navigate(R.id.action_start_to_translationTest)
        }

        updateContinueButton()
    }

    private fun updateContinueButton() {
        binding.btnContinue.isEnabled = selectedRole != null
        binding.btnContinue.alpha = if (selectedRole != null) 1f else 0.5f
    }

    private fun checkAndRequestPermissions() {
        if (PermissionUtils.hasPermissions(requireContext())) {
            proceedAfterPermissions()
        } else {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }

    private fun proceedAfterPermissions() {
        val mainActivity = activity as MainActivity
        mainActivity.myLanguage = selectedLang
        mainActivity.remoteLanguage = if (selectedLang == TranslationManager.LANG_ENGLISH)
            TranslationManager.LANG_HINDI else TranslationManager.LANG_ENGLISH

        val action = when (selectedRole) {
            "HOST" -> R.id.action_start_to_chat
            "JOINER" -> R.id.action_start_to_deviceScan
            else -> return
        }

        val args = Bundle().apply {
            putString("role", selectedRole)
            putString("myLang", selectedLang)
        }
        findNavController().navigate(action, args)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
