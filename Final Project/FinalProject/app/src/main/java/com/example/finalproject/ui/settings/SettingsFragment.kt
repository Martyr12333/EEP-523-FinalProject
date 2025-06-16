package com.example.finalproject.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.finalproject.R
import com.example.finalproject.utils.BleManager

class SettingsFragment : Fragment() {
    
    private lateinit var bleManager: BleManager
    private lateinit var scanButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI(view)
        initializeBLE()
    }
    
    private fun setupUI(view: View) {
        scanButton = view.findViewById(R.id.btn_scan_devices)
        scanButton.setOnClickListener {
            startBLEScan()
        }
    }
    
    private fun initializeBLE() {
        bleManager = BleManager(requireContext())
        bleManager.setBleListener(object : BleManager.BleListener {
            override fun onDeviceDiscovered(device: com.example.finalproject.data.BleDevice) {
                // Add device to list
            }
            
            override fun onDeviceConnected(device: com.example.finalproject.data.BleDevice) {
                Toast.makeText(context, "Connected to ${device.name}", Toast.LENGTH_SHORT).show()
            }
            
            override fun onDeviceDisconnected(device: com.example.finalproject.data.BleDevice) {
                Toast.makeText(context, "Disconnected from ${device.name}", Toast.LENGTH_SHORT).show()
            }
            
            override fun onHeartRateUpdate(heartRate: Int) {}
            override fun onPowerUpdate(power: Int) {}
            override fun onConnectionError(device: com.example.finalproject.data.BleDevice, error: String) {
                Toast.makeText(context, "Connection error: $error", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    private fun startBLEScan() {
        Toast.makeText(context, "Scanning for devices...", Toast.LENGTH_SHORT).show()
        bleManager.startScanning()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        bleManager.cleanup()
    }
} 