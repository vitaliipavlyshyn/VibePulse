package com.vpch.vibepulse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vpch.vibepulse.presentation.auth.AuthIntent
import com.vpch.vibepulse.presentation.auth.AuthViewModel
import com.vpch.vibepulse.screens.navigation.AppNavigation
import com.vpch.vibepulse.ui.VibePulseTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)

        setContent {
            VibePulseTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)

    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data
        Log.d("VibePulseAuth", "DeepLink received -> $data")

        if (data != null && data.scheme == "vibepulse") {
            val code = data.getQueryParameter("code")
            val error = data.getQueryParameter("error")

            when {
                code != null -> authViewModel.onIntent(AuthIntent.AuthCodeReceived(code))
                error != null -> authViewModel.onIntent(AuthIntent.AuthErrorReceived("Spotify auth error: $error"))
            }
        }
    }
}

