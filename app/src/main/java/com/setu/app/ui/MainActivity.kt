package com.setu.app.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.setu.app.R
import com.setu.app.bluetooth.BluetoothConnectionManager
import com.setu.app.databinding.ActivityMainBinding
import com.setu.app.translation.ModelDownloadManager
import com.setu.app.translation.TranslationManager

/**
 * Single-Activity host for all Setu screens.
 * Holds the shared BluetoothConnectionManager and TranslationManager singletons
 * so fragments can access them without ViewModel boilerplate for this prototype.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Shared singletons — fragments access via (activity as MainActivity).xxx
    lateinit var btManager: BluetoothConnectionManager
    val translationManager = TranslationManager()
    val modelDownloadManager = ModelDownloadManager()

    /** The display language chosen by this device's user (set in StartFragment). */
    var myLanguage: String = TranslationManager.LANG_ENGLISH

    /** The sender's language (learned from the SYS:LANG handshake). */
    var remoteLanguage: String = TranslationManager.LANG_HINDI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        // Initialize Bluetooth
        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if (adapter == null) {
            Toast.makeText(this, "This device does not support Bluetooth", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!adapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivity(enableBtIntent)
        }

        btManager = BluetoothConnectionManager(adapter)

        // Navigation
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        setupActionBarWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        return navHostFragment.navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        super.onDestroy()
        btManager.release()
        translationManager.release()
    }
}
