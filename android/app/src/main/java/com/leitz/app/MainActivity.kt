package com.leitz.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.leitz.app.ui.screens.CallScreen
import com.leitz.app.ui.screens.ChatListScreen
import com.leitz.app.ui.screens.ChatScreen
import com.leitz.app.ui.screens.LoginScreen
import com.leitz.app.ui.screens.SettingsScreen
import com.leitz.app.ui.theme.LeitXTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeitXTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost()
                }
            }
        }
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate("chatList") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        composable("chatList") {
            ChatListScreen(
                onOpenChat = { userId ->
                    navController.navigate("chat/$userId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                }
            )
        }

        composable(
            route = "chat/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            ChatScreen(
                userId = userId,
                onBack = { navController.popBackStack() },
                onStartCall = { isVideo ->
                    navController.navigate("call/$userId?video=$isVideo")
                }
            )
        }

        composable(
            route = "call/{userId}?video={video}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("video") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: return@composable
            val isVideo = backStackEntry.arguments?.getBoolean("video") ?: false
            CallScreen(
                userId = userId,
                isVideo = isVideo,
                onEndCall = { navController.popBackStack() }
            )
        }

        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}