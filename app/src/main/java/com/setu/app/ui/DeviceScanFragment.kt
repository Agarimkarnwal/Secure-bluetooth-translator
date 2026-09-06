package com.setu.app.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.setu.app.R
import com.setu.app.bluetooth.ConnectionListener
import com.setu.app.bluetooth.PermissionUtils
import com.setu.app.databinding.FragmentDeviceScanBinding

class DeviceScanFragment : Fragment() {

    private var _binding: FragmentDeviceScanBinding? = null
    private val binding get() = _binding!!

    private val deviceList = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: DeviceListAdapter

    private val mainActivity get() = activity as MainActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceScanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        deviceAdapter = DeviceListAdapter(deviceList) { device ->
            connectTo(device)
        }

        binding.rvDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }

        binding.btnScanPaired.setOnClickListener { loadPairedDevices() }
        binding.btnStartDiscovery.setOnClickListener { startDiscovery() }

        loadPairedDevices()

        try {
            requireContext().registerReceiver(
                discoveryReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission", "NotifyDataSetChanged")
    private fun loadPairedDevices() {
        if (!PermissionUtils.hasConnectPermission(requireContext())) {
            binding.tvStatus.text = "Bluetooth permissions missing."
            return
        }

        val paired = try {
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices ?: emptySet()
        } catch (e: SecurityException) {
            emptySet()
        }

        deviceList.clear()
        deviceList.addAll(paired)
        deviceAdapter.notifyDataSetChanged()

        if (deviceList.isEmpty()) {
            binding.tvStatus.text = "No paired devices found. Pair phones in BT Settings."
        } else {
            binding.tvStatus.text = "Paired devices (tap to connect):"
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (!PermissionUtils.hasScanPermission(requireContext())) {
            Toast.makeText(requireContext(), "Scan permission required", Toast.LENGTH_SHORT).show()
            return
        }

        binding.tvStatus.text = "Scanning for nearby devices…"
        binding.progressBar.visibility = View.VISIBLE
        try {
            BluetoothAdapter.getDefaultAdapter()?.startDiscovery()
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun connectTo(device: BluetoothDevice) {
        if (!PermissionUtils.hasConnectPermission(requireContext())) {
            Toast.makeText(requireContext(), "Connect permission required", Toast.LENGTH_SHORT).show()
            return
        }

        val deviceNameStr = try { device.name ?: "Remote Device" } catch (_: SecurityException) { "Remote Device" }
        binding.tvStatus.text = "Connecting to $deviceNameStr…"
        binding.progressBar.visibility = View.VISIBLE

        mainActivity.btManager.listener = object : ConnectionListener {
            override fun onConnected(deviceName: String) {
                val args = Bundle().apply {
                    putString("role", "JOINER")
                    putString("myLang", mainActivity.myLanguage)
                    putString("deviceName", deviceName)
                }
                findNavController().navigate(R.id.action_deviceScan_to_chat, args)
            }

            override fun onMessageReceived(raw: String) { }

            override fun onConnectionLost() {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Connection lost. Try again."
            }

            override fun onConnectionFailed(reason: String) {
                binding.progressBar.visibility = View.GONE
                binding.tvStatus.text = "Failed: $reason"
                Toast.makeText(requireContext(), "Connection failed: $reason", Toast.LENGTH_SHORT).show()
            }
        }

        mainActivity.btManager.connectToDevice(device)
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission", "NotifyDataSetChanged")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device != null && !deviceList.contains(device)) {
                    deviceList.add(device)
                    deviceAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            requireContext().unregisterReceiver(discoveryReceiver)
        } catch (_: Exception) {}
        _binding = null
    }
}

class DeviceListAdapter(
    private val devices: List<BluetoothDevice>,
    private val onDeviceClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.VH>() {

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvDeviceName)
        val tvAddress: TextView = itemView.findViewById(R.id.tvDeviceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return VH(v)
    }

    @SuppressLint("MissingPermission")
    override fun onBindViewHolder(holder: VH, position: Int) {
        val device = devices[position]
        val nameStr = try { device.name ?: "Unknown Device" } catch (_: SecurityException) { "Unknown Device" }
        holder.tvName.text = nameStr
        holder.tvAddress.text = device.address
        holder.itemView.setOnClickListener { onDeviceClick(device) }
    }

    override fun getItemCount() = devices.size
}
