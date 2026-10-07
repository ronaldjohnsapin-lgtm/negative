package com.negative.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repo(applicationContext)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Ui.WHITE, onPrimary = Ui.BG, background = Ui.BG, onBackground = Ui.WHITE,
                    surface = Ui.CARD, onSurface = Ui.WHITE, onSurfaceVariant = Ui.MUTED, error = Ui.RED
                )
            ) { NegativeApp(repo) }
        }
    }
}
