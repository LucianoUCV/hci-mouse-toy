package com.example.mousetoyapp.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mousetoyapp.navigation.Destinations
import com.example.mousetoyapp.state.ConnectionStatus
import com.example.mousetoyapp.state.MouseViewModel
import com.example.mousetoyapp.ui.components.BounceButton
import com.example.mousetoyapp.ui.components.CommandButton
import com.example.mousetoyapp.ui.components.HCIJoystick
import com.example.mousetoyapp.ui.theme.AccentCyan
import com.example.mousetoyapp.ui.theme.AccentGreen
import com.example.mousetoyapp.ui.theme.AccentRed
import com.example.mousetoyapp.ui.theme.SurfaceCard
import com.example.mousetoyapp.ui.theme.TextMuted
import kotlin.math.abs

@Composable
fun MainScreen(navController: NavController, vm: MouseViewModel) {
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
                    },
                    label = ""
                )
                Text(vm.status.name, color = statusColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            }

            FilledIconButton(
                onClick = { if (isConnected) navController.navigate(Destinations.STATS) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isConnected) SurfaceCard else Color.DarkGray.copy(0.3f)
                ),
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Info else Icons.Default.Lock,
                    contentDescription = null,
                    tint = if (isConnected) AccentCyan else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        BounceButton(
            text = if (isConnected) "Disconnect" else "Connect",
            isConnected = isConnected,
            enabled = vm.status != ConnectionStatus.CONNECTING,
            onClick = {
                if (vm.status == ConnectionStatus.DISCONNECTED) vm.connect() else vm.disconnect()
            }
        )

        if (vm.bluetoothError) {
            Text(
                "Please turn on Bluetooth to connect!",
                color = AccentRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            "Commands",
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Locate", "Squeak", "Reboot").forEachIndexed { index, label ->
                val active = vm.activeMode == index + 1
                Box(modifier = Modifier.weight(1f)) {
                    CommandButton(
                        label = label,
                        isActive = active,
                        isConnected = isConnected,
                        onClick = {
                            vm.activeMode = if (active) 0 else index + 1
                            if (!active) {
                                vm.sendSpecialCommand(label.uppercase())
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            HCIJoystick(enabled = isConnected) { x, y ->
                if (abs(x) > 10 || abs(y) > 10 || (x == 0f && y == 0f)) {
                    vm.sendJoystickCommand(x, y)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "Data Stream",
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(vm.consoleLogs) { log ->
                    val isErrorOrTerminated =
                        log.contains("ERR") || log.contains("FATAL") || log.contains("TERMINATED")
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

