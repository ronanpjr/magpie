package com.magpie.magpie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.magpie.magpie.navigation.MagpieNavGraph
import com.magpie.magpie.ui.theme.MagpieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MagpieTheme {
                MagpieNavGraph()
            }
        }
    }
}