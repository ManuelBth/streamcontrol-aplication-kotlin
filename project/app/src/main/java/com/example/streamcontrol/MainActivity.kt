package com.example.streamcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.streamcontrol.navigation.StreamControlNavigation
import com.example.streamcontrol.ui.theme.StreamControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StreamControlTheme {
                StreamControlNavigation()
            }
        }
    }
}
