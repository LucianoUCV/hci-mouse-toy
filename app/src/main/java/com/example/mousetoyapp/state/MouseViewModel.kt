package com.example.mousetoyapp.state

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

@SuppressLint("MissingPermission")
class MouseViewModel(application: Application) : AndroidViewModel(application) {
    var status by mutableStateOf(ConnectionStatus.DISCONNECTED)
    var activeMode by mutableIntStateOf(0)
    val consoleLogs = mutableStateListOf<String>()

    var bluetoothError by mutableStateOf(false)

    var battery by mutableIntStateOf(85)
    var voltage by mutableFloatStateOf(3.72f)
    var signalStrength by mutableIntStateOf(-62)
    private var telemetryJob: Job? = null

    private val bluetoothAdapter: BluetoothAdapter? =
        (application.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHAR_UUID_RX = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        consoleLogs.add(0, "[$time] $msg")
        if (consoleLogs.size > 15) consoleLogs.removeAt(consoleLogs.lastIndex)
    }

    private fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val deviceName = device.name ?: result.scanRecord?.deviceName

            if (device.name == "MOUSE_ESP32S3") {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(this)
                addLog("GĂSIT: ${device.name}. Se conectează...")
                connectToDevice(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                this@MouseViewModel.status = ConnectionStatus.CONNECTED
                addLog("LINK STABIL. Descoperire servicii...")
                gatt.discoverServices()
                startTelemetry()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                this@MouseViewModel.status = ConnectionStatus.DISCONNECTED
                addLog("LINK TERMINAT / PIERDUT.")
                bluetoothGatt?.close()
                bluetoothGatt = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                writeCharacteristic = service?.getCharacteristic(CHAR_UUID_RX)
                if (writeCharacteristic != null) {
                    addLog("SISTEM PREGĂTIT. Gata de comenzi.")
                } else {
                    addLog("ERR: Caracteristica RX lipsă!")
                }
            }
        }

        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                this@MouseViewModel.signalStrength = rssi
            }
        }
    }

    fun connect() {
        if (status != ConnectionStatus.DISCONNECTED) return

        if (!isBluetoothEnabled()) {
            bluetoothError = true
            addLog("ERR: BLUETOOTH ESTE OPRIT")
            return
        }
        bluetoothError = false
        status = ConnectionStatus.CONNECTING
        addLog("SCANARE DUPĂ MOUSE_ESP32S3...")

        bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)

        viewModelScope.launch {
            delay(5000)
            if (status == ConnectionStatus.CONNECTING) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
                status = ConnectionStatus.DISCONNECTED
                addLog("ERR: TIMEOUT SCANARE")
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(getApplication(), false, gattCallback)
    }

    fun disconnect() {
        bluetoothGatt?.disconnect()
        telemetryJob?.cancel()
        activeMode = 0
    }

    private fun sendData(data: String) {
        if (bluetoothGatt != null && writeCharacteristic != null) {
            writeCharacteristic?.value = data.toByteArray()
            writeCharacteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            bluetoothGatt?.writeCharacteristic(writeCharacteristic)
        }
    }

    fun sendJoystickCommand(x: Float, y: Float) {
        val mapX = (x * 2.55f).toInt()
        val mapY = (y * -2.55f).toInt()

        sendData("J:$mapX,$mapY")
    }

    fun sendSpecialCommand(commandType: String) {
        sendData("CMD:$commandType")
        addLog("TX >> CMD:$commandType")
    }

    private fun startTelemetry() {
        telemetryJob = viewModelScope.launch {
            while (status == ConnectionStatus.CONNECTED) {
                delay(2000)
                if (!isBluetoothEnabled()) {
                    addLog("FATAL: BLUETOOTH OPRIT SUBIT")
                    disconnect()
                    continue
                }

                bluetoothGatt?.readRemoteRssi()

                voltage = 3.6f + Random().nextFloat() * 0.3f
                if (Random().nextInt(10) > 8 && battery > 0) battery--
            }
        }
    }
}