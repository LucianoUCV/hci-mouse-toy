package com.example.mousetoyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mousetoyapp.navigation.AppNavGraph
import com.example.mousetoyapp.state.MouseViewModel
import com.example.mousetoyapp.ui.theme.BackgroundDeep

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MouseViewModel = viewModel()
            MaterialTheme {
                Surface(color = BackgroundDeep, modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(vm)
                }
            }
        }
    }
}