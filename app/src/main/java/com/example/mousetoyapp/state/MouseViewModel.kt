package com.example.mousetoyapp.state

import android.bluetooth.BluetoothAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

class MouseViewModel : ViewModel() {
    var status by mutableStateOf(ConnectionStatus.DISCONNECTED)
    var activeMode by mutableIntStateOf(0)
    val consoleLogs = mutableStateListOf<String>()

    var bluetoothError by mutableStateOf(false)

    // hardcoded for now
    var battery by mutableIntStateOf(85)
    var voltage by mutableFloatStateOf(3.72f)
    var signalStrength by mutableIntStateOf(-62)
    private var telemetryJob: Job? = null

    fun addLog(msg: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        consoleLogs.add(0, "[$time] $msg")
        if (consoleLogs.size > 15) consoleLogs.removeAt(consoleLogs.lastIndex)
    }

    private fun isBluetoothEnabled(): Boolean {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            adapter?.isEnabled == true
        } catch (e: Exception) {
            true
        }
    }

    fun connect() {
        if (status != ConnectionStatus.DISCONNECTED) return

        if (!isBluetoothEnabled()) {
            bluetoothError = true
            addLog("ERR: BLUETOOTH IS DISABLED")
            return
        }
        bluetoothError = false

        viewModelScope.launch {
            status = ConnectionStatus.CONNECTING
            addLog("SCANNING FOR MOUSE_ESP...")
            delay(1200)
            addLog("DEVICE FOUND: 00:1A:7D:DA:71:13")
            delay(800)
            status = ConnectionStatus.CONNECTED
            addLog("LINK STABLE. SYSTEM READY.")
            startTelemetry()
        }
    }

    fun disconnect() {
        status = ConnectionStatus.DISCONNECTED
        telemetryJob?.cancel()
        activeMode = 0
        addLog("LINK TERMINATED.")
    }

    private fun startTelemetry() {
        telemetryJob = viewModelScope.launch {
            while (status == ConnectionStatus.CONNECTED) {
                delay(2000)
                if (!isBluetoothEnabled()) {
                    addLog("FATAL: BLUETOOTH ADAPTER OFFLINE")
                    disconnect()
                    bluetoothError = true
                    continue
                }

                // mock data for now
                voltage = 3.6f + Random().nextFloat() * 0.3f
                signalStrength = -55 - Random().nextInt(20)
                if (Random().nextInt(10) > 8 && battery > 0) battery--
            }
        }
    }
}

