package com.seekersofjustice.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.seekersofjustice.app.ui.auth.AuthScreen
import com.seekersofjustice.app.ui.navigation.AppNavHost
import com.seekersofjustice.app.ui.theme.SeekersTheme

// AppCompatActivity (a FragmentActivity) is required both for
// AppCompatDelegate.setApplicationLocales and for BiometricPrompt.
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as App).container
        setContent {
            SeekersTheme {
                val officer by container.currentOfficer.collectAsStateWithLifecycle()
                if (officer == null) {
                    AuthScreen(onAuthenticated = { container.signIn(it) })
                } else {
                    AppNavHost(onSignOut = { container.signOut() })
                }
            }
        }
    }
}
