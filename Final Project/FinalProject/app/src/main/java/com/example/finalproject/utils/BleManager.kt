package com.example.finalproject.utils

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.finalproject.data.BleDevice
import com.example.finalproject.data.DeviceType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

class BleManager(private val context: Context) {
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val bleScanner = bluetoothAdapter?.bluetoothLeScanner
    
    // Heart Rate Service UUID
    private val HEART_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805F9B34FB")
    private val HEART_RATE_MEASUREMENT_UUID = UUID.fromString("00002A37-0000-1000-8000-00805F9B34FB")
    
    // Cycling Power Service UUID
    private val CYCLING_POWER_SERVICE_UUID = UUID.fromString("00001818-0000-1000-8000-00805F9B34FB")
    private val CYCLING_POWER_MEASUREMENT_UUID = UUID.fromString("00002A63-0000-1000-8000-00805F9B34FB")
    
    // State tracking
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning
    
    private val _discoveredDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleDevice>> = _discoveredDevices
    
    private val _connectedDevices = MutableStateFlow<List<BleDevice>>(emptyList())
    val connectedDevices: StateFlow<List<BleDevice>> = _connectedDevices
    
    private val _heartRate = MutableStateFlow<Int?>(null)
    val heartRate: StateFlow<Int?> = _heartRate
    
    private val _power = MutableStateFlow<Int?>(null)
    val power: StateFlow<Int?> = _power
    
    private val _cadence = MutableStateFlow<Int?>(null)
    val cadence: StateFlow<Int?> = _cadence
    
    private val _speed = MutableStateFlow<Float?>(null)
    val speed: StateFlow<Float?> = _speed
    
    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel
    
    private val connectedGatts = mutableMapOf<String, BluetoothGatt>()
    private val handler = Handler(Looper.getMainLooper())
    
    private var bleListener: BleListener? = null
    
    interface BleListener {
        fun onDeviceDiscovered(device: BleDevice)
        fun onDeviceConnected(device: BleDevice)
        fun onDeviceDisconnected(device: BleDevice)
        fun onHeartRateUpdate(heartRate: Int)
        fun onPowerUpdate(power: Int)
        fun onConnectionError(device: BleDevice, error: String)
    }
    
    fun setBleListener(listener: BleListener?) {
        this.bleListener = listener
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = device.name ?: "Unknown Device"
            val address = device.address
            
            // Determine device type based on advertised services
            val deviceType = when {
                result.scanRecord?.serviceUuids?.any { 
                    it.uuid == HEART_RATE_SERVICE_UUID 
                } == true -> DeviceType.HEART_RATE_MONITOR
                result.scanRecord?.serviceUuids?.any { 
                    it.uuid == CYCLING_POWER_SERVICE_UUID 
                } == true -> DeviceType.CYCLING_POWER_SENSOR
                else -> DeviceType.HEART_RATE_MONITOR // Default
            }
            
            val bleDevice = BleDevice(name, address, deviceType)
            
            // Add to discovered devices if not already present
            val currentDevices = _discoveredDevices.value.toMutableList()
            if (currentDevices.none { it.address == address }) {
                currentDevices.add(bleDevice)
                _discoveredDevices.value = currentDevices
                bleListener?.onDeviceDiscovered(bleDevice)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }
    
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val device = findDeviceByAddress(gatt.device.address)
            
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedGatts[gatt.device.address] = gatt
                    device?.let { 
                        val connectedDevice = it.copy(isConnected = true)
                        updateConnectedDevice(connectedDevice)
                        bleListener?.onDeviceConnected(connectedDevice)
                    }
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectedGatts.remove(gatt.device.address)
                    device?.let { 
                        val disconnectedDevice = it.copy(isConnected = false)
                        removeConnectedDevice(disconnectedDevice)
                        bleListener?.onDeviceDisconnected(disconnectedDevice)
                    }
                    gatt.close()
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Enable notifications for heart rate
                gatt.getService(HEART_RATE_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)?.let { characteristic ->
                        enableNotification(gatt, characteristic)
                    }
                }
                
                // Enable notifications for cycling power
                gatt.getService(CYCLING_POWER_SERVICE_UUID)?.let { service ->
                    service.getCharacteristic(CYCLING_POWER_MEASUREMENT_UUID)?.let { characteristic ->
                        enableNotification(gatt, characteristic)
                    }
                }
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                HEART_RATE_MEASUREMENT_UUID -> {
                    val heartRate = parseHeartRate(value)
                    _heartRate.value = heartRate
                    bleListener?.onHeartRateUpdate(heartRate)
                }
                CYCLING_POWER_MEASUREMENT_UUID -> {
                    val power = parsePower(value)
                    _power.value = power
                    bleListener?.onPowerUpdate(power)
                }
            }
        }
    }
    
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        )
        descriptor?.let {
            gatt.setCharacteristicNotification(characteristic, true)
            it.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(it)
        }
    }
    
    fun startScanning() {
        if (!hasBluetoothPermission() || bluetoothAdapter?.isEnabled != true) {
            return
        }
        
        if (_isScanning.value) return
        
        _discoveredDevices.value = emptyList()
        
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        try {
            bleScanner?.startScan(null, scanSettings, scanCallback)
            _isScanning.value = true
            
            // Stop scanning after 10 seconds
            handler.postDelayed({
                stopScanning()
            }, 10000)
        } catch (e: SecurityException) {
            // Handle permission error
        }
    }
    
    fun stopScanning() {
        if (_isScanning.value) {
            try {
                bleScanner?.stopScan(scanCallback)
            } catch (e: SecurityException) {
                // Handle permission error
            }
            _isScanning.value = false
        }
    }
    
    fun connectToDevice(device: BleDevice) {
        if (!hasBluetoothPermission()) return
        
        val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        try {
            bluetoothDevice?.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            bleListener?.onConnectionError(device, "Permission denied")
        }
    }
    
    fun disconnectDevice(device: BleDevice) {
        connectedGatts[device.address]?.disconnect()
    }
    
    private fun parseHeartRate(data: ByteArray): Int {
        return if (data.isNotEmpty()) {
            if (data[0].toInt() and 0x01 == 0) {
                // Heart rate format is in the 2nd byte
                data[1].toInt() and 0xFF
            } else {
                // Heart rate format is in the 2nd and 3rd bytes
                ((data[2].toInt() and 0xFF) shl 8) + (data[1].toInt() and 0xFF)
            }
        } else {
            0
        }
    }
    
    private fun parsePower(data: ByteArray): Int {
        return if (data.size >= 4) {
            // Power is in bytes 2-3 (little endian)
            ((data[3].toInt() and 0xFF) shl 8) + (data[2].toInt() and 0xFF)
        } else {
            0
        }
    }
    
    private fun findDeviceByAddress(address: String): BleDevice? {
        return _discoveredDevices.value.find { it.address == address }
            ?: _connectedDevices.value.find { it.address == address }
    }
    
    private fun updateConnectedDevice(device: BleDevice) {
        val currentConnected = _connectedDevices.value.toMutableList()
        val index = currentConnected.indexOfFirst { it.address == device.address }
        if (index >= 0) {
            currentConnected[index] = device
        } else {
            currentConnected.add(device)
        }
        _connectedDevices.value = currentConnected
    }
    
    private fun removeConnectedDevice(device: BleDevice) {
        val currentConnected = _connectedDevices.value.toMutableList()
        currentConnected.removeAll { it.address == device.address }
        _connectedDevices.value = currentConnected
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun cleanup() {
        stopScanning()
        connectedGatts.values.forEach { gatt ->
            try {
                gatt.disconnect()
                gatt.close()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        connectedGatts.clear()
        bleListener = null
    }
} 