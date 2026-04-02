package com.example.mousetoyapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mousetoyapp.screens.MainScreen
import com.example.mousetoyapp.screens.StatsScreen
import com.example.mousetoyapp.state.MouseViewModel

@Composable
fun AppNavGraph(vm: MouseViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Destinations.MAIN) {
        composable(Destinations.MAIN) { MainScreen(navController, vm) }
        composable(Destinations.STATS) { StatsScreen(navController, vm) }
    }
}

