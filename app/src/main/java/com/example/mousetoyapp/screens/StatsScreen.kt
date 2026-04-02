package com.example.mousetoyapp.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.mousetoyapp.state.MouseViewModel
import com.example.mousetoyapp.ui.components.BounceButton
import com.example.mousetoyapp.ui.components.StatItem
import com.example.mousetoyapp.ui.theme.AccentGreen
import com.example.mousetoyapp.ui.theme.AccentRed
import com.example.mousetoyapp.ui.theme.TextMuted
import java.util.Locale

@Composable
fun StatsScreen(navController: NavController, vm: MouseViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        Text("Stats", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(40.dp))

        StatItem("Battery Level", "${vm.battery}%", AccentGreen, progress = vm.battery / 100f)
        StatItem("Voltage", "${String.format(Locale.US, "%.2f", vm.voltage)} V", Color.White)
        StatItem(
            "Signal Strength",
            "${vm.signalStrength} dBm",
            if (vm.signalStrength > -70) AccentGreen else AccentRed
        )
        StatItem("Firmware Version", "v1.0.3-stable", TextMuted)

        Spacer(modifier = Modifier.weight(1f))

        BounceButton(
            text = "Back",
            isConnected = true,
            onClick = { navController.popBackStack() }
        )
    }
}
