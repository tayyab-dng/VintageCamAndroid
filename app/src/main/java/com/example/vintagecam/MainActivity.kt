package com.example.vintagecam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.vintagecam.permissions.CameraPermissionGate
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageCamTheme
import com.example.vintagecam.ui.VintageCameraScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            VintageCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = VintageBlack
                ) {
                    CameraPermissionGate {
                        VintageCameraScreen()
                    }
                }
            }
        }
    }
}
