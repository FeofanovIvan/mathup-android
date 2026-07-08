package com.feofanova.mathup.ui.screens.splash

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.NavHostController

import com.feofanova.mathup.ui.navigation.Routes
import com.feofanova.mathup.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavHostController) {
    SetStatusBarColor(color = Color.White, darkIcons = true)
    val context = LocalView.current.context

    LaunchedEffect(Unit) {
        delay(1500) // Пауза для логотипа

        val isSignedIn = FirebaseAuth.getInstance().currentUser != null
        if (isSignedIn) {
            navController.navigate(Routes.MAIN) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        } else {
            navController.navigate(Routes.AUTH) {
                popUpTo(Routes.SPLASH) { inclusive = true }
            }
        }

    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "App Logo",
            modifier = Modifier.size(540.dp)
        )
    }
}

@Composable
fun SetStatusBarColor(color: Color, darkIcons: Boolean = true) {
    val view = LocalView.current
    val window = (view.context as Activity).window

    SideEffect {
        @Suppress("DEPRECATION")
        window.statusBarColor = color.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkIcons
    }
}


