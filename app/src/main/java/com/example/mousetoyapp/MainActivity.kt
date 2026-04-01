package com.example.mousetoyapp

import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

val BackgroundDeep = Color(0xFF0A0A0C)
val SurfaceCard = Color(0xFF141417)
val AccentCyan = Color(0xFF00E5FF)
val AccentRed = Color(0xFFFF3366)
val AccentGreen = Color(0xFF00FF88)
val TextMuted = Color(0xFF9E9EAE)

enum class ConnectionStatus { DISCONNECTED, CONNECTING, CONNECTED }

class MouseViewModel : ViewModel() {
    var status by mutableStateOf(ConnectionStatus.DISCONNECTED)
    var activeMode by mutableIntStateOf(0)
    val consoleLogs = mutableStateListOf<String>()

    var bluetoothError by mutableStateOf(false)

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
            while(status == ConnectionStatus.CONNECTED) {
                delay(2000)
                if (!isBluetoothEnabled()) {
                    addLog("FATAL: BLUETOOTH ADAPTER OFFLINE")
                    disconnect()
                    bluetoothError = true
                    continue
                }

                voltage = 3.6f + Random().nextFloat() * 0.3f
                signalStrength = -55 - Random().nextInt(20)
                if (Random().nextInt(10) > 8 && battery > 0) battery--
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MouseViewModel = viewModel()
            MaterialTheme {
                Surface(color = BackgroundDeep, modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    NavHost(navController, startDestination = "main") {
                        composable("main") { MainScreen(navController, vm) }
                        composable("stats") { StatsScreen(navController, vm) }
                    }
                }
            }
        }
    }
}

@Composable
fun BounceButton(
    text: String,
    isConnected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed && enabled) 0.95f else 1f, animationSpec = tween(100), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(if (!enabled) Color(0xFF1A1A1A) else if (isConnected) Color(0xFF252529) else AccentCyan)
            .clickable(interactionSource = interactionSource, indication = null, enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (!enabled) Color.DarkGray else if (isConnected) Color.White else Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
fun CommandButton(
    label: String,
    isActive: Boolean,
    isConnected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed && isConnected) 0.92f else 1f, animationSpec = tween(100), label = "")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) AccentCyan.copy(0.15f) else SurfaceCard)
            .border(1.dp, if (isActive) AccentCyan else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null, enabled = isConnected) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (isConnected) (if(isActive) AccentCyan else Color.White) else Color.Gray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun MainScreen(navController: androidx.navigation.NavController, vm: MouseViewModel) {
    val isConnected = vm.status == ConnectionStatus.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Status", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                val statusColor by animateColorAsState(
                    targetValue = when (vm.status) {
                        ConnectionStatus.DISCONNECTED -> AccentRed
                        ConnectionStatus.CONNECTING -> Color(0xFFFFB74D)
                        ConnectionStatus.CONNECTED -> AccentGreen
                    }, label = ""
                )
                Text(vm.status.name, color = statusColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            FilledIconButton(
                onClick = { if(isConnected) navController.navigate("stats") },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if(isConnected) SurfaceCard else Color.DarkGray.copy(0.3f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if(isConnected) Icons.Default.Info else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if(isConnected) AccentCyan else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BounceButton(
            text = if (isConnected) "Disconnect" else "Connect",
            isConnected = isConnected,
            enabled = vm.status != ConnectionStatus.CONNECTING,
            onClick = { if (vm.status == ConnectionStatus.DISCONNECTED) vm.connect() else vm.disconnect() }
        )

        if (vm.bluetoothError) {
            Text("Please turn on Bluetooth to connect!", color = AccentRed, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text("Commands", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("Locate", "Squeak", "Reboot").forEachIndexed { index, label ->
                val active = vm.activeMode == index + 1
                Box(modifier = Modifier.weight(1f)) {
                    CommandButton(
                        label = label,
                        isActive = active,
                        isConnected = isConnected,
                        onClick = {
                            vm.activeMode = if (active) 0 else index + 1
                            if (!active) vm.addLog("CMD >> ${label.uppercase()}")
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            HCIJoystick(enabled = isConnected) { x, y ->
                if (abs(x) > 10 || abs(y) > 10 || (x == 0f && y == 0f)) {
                    vm.addLog("TX >> X:${x.toInt()}, Y:${(y.toInt() * -1)}")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text("Data Stream", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(modifier = Modifier.height(6.dp))
        Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(Color.Black, RoundedCornerShape(8.dp)).border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp)).padding(10.dp)) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(vm.consoleLogs) { log ->
                    val isErrorOrTerminated = log.contains("ERR") || log.contains("FATAL") || log.contains("TERMINATED")
                    Text(
                        text = log,
                        color = if (isErrorOrTerminated) AccentRed else AccentGreen,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun HCIJoystick(enabled: Boolean, onMove: (Float, Float) -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val baseSize = 220.dp
    val thumbSize = 80.dp
    val maxTravelPx = with(LocalDensity.current) { ((baseSize - thumbSize) / 2).toPx() }

    Box(
        modifier = Modifier
            .size(baseSize)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF222228), Color.Transparent),
                    radius = 300f
                ),
                CircleShape
            )
            .border(2.dp, Color.White.copy(alpha = if (enabled) 0.15f else 0.05f), CircleShape),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(thumbSize)
                .shadow(if (enabled) 15.dp else 0.dp, CircleShape, ambientColor = AccentCyan, spotColor = AccentCyan)
                .background(
                    if (enabled) Brush.radialGradient(listOf(Color(0xFF88FFFF), AccentCyan, Color(0xFF0099AA)), center = Offset(30f, 30f), radius = 150f)
                    else Brush.radialGradient(listOf(Color.Gray, Color.DarkGray), center = Offset(30f, 30f), radius = 150f),
                    CircleShape
                )
                .border(1.dp, Color.White.copy(0.3f), CircleShape)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragEnd = { offsetX = 0f; offsetY = 0f; onMove(0f, 0f) },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val nX = offsetX + dragAmount.x
                            val nY = offsetY + dragAmount.y
                            val dist = sqrt(nX*nX + nY*nY)

                            if (dist <= maxTravelPx) {
                                offsetX = nX; offsetY = nY
                            } else {
                                val a = atan2(nY, nX); offsetX = cos(a)*maxTravelPx; offsetY = sin(a)*maxTravelPx
                            }

                            onMove((offsetX/maxTravelPx)*100f, (offsetY/maxTravelPx)*100f)
                        }
                    )
                }
        )
    }
}

@Composable
fun StatsScreen(navController: androidx.navigation.NavController, vm: MouseViewModel) {
    Column(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(24.dp)) {
        Text("Stats", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(40.dp))

        StatItem("Battery Level", "${vm.battery}%", AccentGreen, progress = vm.battery / 100f)
        StatItem("Voltage", "${String.format(Locale.US, "%.2f", vm.voltage)} V", Color.White)
        StatItem("Signal Strength", "${vm.signalStrength} dBm", if(vm.signalStrength > -70) AccentGreen else AccentRed)
        StatItem("Firmware Version", "v1.0.3-stable", TextMuted)

        Spacer(modifier = Modifier.weight(1f))

        BounceButton(
            text = "Back",
            isConnected = true,
            onClick = { navController.popBackStack() }
        )
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color, progress: Float? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = TextMuted, fontSize = 14.sp)
                Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
            if (progress != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF252529))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = progress)
                            .fillMaxHeight()
                            .background(color)
                    )
                }
            }
        }
    }
}