package com.seekersofjustice.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.seekersofjustice.app.ui.navigation.AppNavHost
import com.seekersofjustice.app.ui.theme.SeekersTheme

// AppCompatActivity (not ComponentActivity) is required for
// AppCompatDelegate.setApplicationLocales-based language switching.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeekersTheme {
                AppNavHost()
            }
        }
    }
}
