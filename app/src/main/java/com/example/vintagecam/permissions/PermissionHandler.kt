package com.example.vintagecam.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.vintagecam.theme.VintageAmber
import com.example.vintagecam.theme.VintageBlack
import com.example.vintagecam.theme.VintageBodyDark
import com.example.vintagecam.theme.VintageGold
import com.example.vintagecam.theme.VintageGreenAccent
import com.example.vintagecam.theme.VintageLeatherGrip
import com.example.vintagecam.theme.VintageMetalDark
import com.example.vintagecam.theme.VintageMetalSilver
import com.example.vintagecam.theme.VintageShutterRed
import com.example.vintagecam.theme.VintageSurfaceElevated
import com.example.vintagecam.theme.VintageTextLight
import com.example.vintagecam.theme.VintageTextMuted

val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.CAMERA,
    Manifest.permission.RECORD_AUDIO
)

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Clean, robust Jetpack Compose permission workflow using ActivityResultContracts.
 * Evaluates camera and audio permissions, listens for app resume to re-check
 * after returning from System Settings, and shows a vintage-styled fallback UI if denied.
 */
@Composable
fun CameraPermissionGate(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var cameraGranted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.CAMERA))
    }
    var audioGranted by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.RECORD_AUDIO))
    }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    fun refreshPermissions() {
        cameraGranted = context.hasPermission(Manifest.permission.CAMERA)
        audioGranted = context.hasPermission(Manifest.permission.RECORD_AUDIO)
    }

    // Automatically recheck permissions when app resumes (e.g. returning from settings)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissions()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        cameraGranted = results[Manifest.permission.CAMERA] ?: cameraGranted
        audioGranted = results[Manifest.permission.RECORD_AUDIO] ?: audioGranted
        permissionRequestedOnce = true
    }

    // Request permissions automatically on first launch
    LaunchedEffect(Unit) {
        if (!cameraGranted) {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    AnimatedContent(
        targetState = cameraGranted,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "PermissionGateTransition"
    ) { hasAccess ->
        if (hasAccess) {
            content()
        } else {
            val shouldShowRationale = activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
            val isPermanentlyDenied = permissionRequestedOnce && !shouldShowRationale && !cameraGranted

            PermissionFallbackUI(
                isPermanentlyDenied = isPermanentlyDenied,
                cameraGranted = cameraGranted,
                audioGranted = audioGranted,
                onRequestPermission = {
                    permissionLauncher.launch(REQUIRED_PERMISSIONS)
                },
                onOpenSettings = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun PermissionFallbackUI(
    isPermanentlyDenied: Boolean,
    cameraGranted: Boolean,
    audioGranted: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = VintageBlack
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(VintageBodyDark)
                    .border(1.dp, VintageMetalDark, RoundedCornerShape(20.dp))
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Retro Aperture Badge
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(VintageSurfaceElevated, VintageLeatherGrip)
                            )
                        )
                        .border(2.dp, VintageGold, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPermanentlyDenied) Icons.Default.Lock else Icons.Default.CameraAlt,
                        contentDescription = "Vintage Camera",
                        tint = VintageAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = if (isPermanentlyDenied) "CAMERA ACCESS RESTRICTED" else "LENS CAP ATTACHED",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = VintageTextLight,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle / Telemetry Badge
                Text(
                    text = "VINTAGE OPTICS MODULE • PERMISSION REQUIRED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    ),
                    color = VintageAmber,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Explanation
                Text(
                    text = if (isPermanentlyDenied) {
                        "Camera permission was previously disabled. To compose analog film photos and utilize CameraX real-time viewfinders, please enable camera access in App Settings."
                    } else {
                        "VintageCam requires direct camera sensor access to drive the live optical viewfinder, emulate mechanical leaf shutter physics, and capture vintage film frames."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = VintageTextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Status checklist
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(VintageLeatherGrip)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PermissionStatusRow(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera Sensor (Essential)",
                        isGranted = cameraGranted
                    )
                    PermissionStatusRow(
                        icon = Icons.Default.Mic,
                        label = "Audio Sensor (Synchronized Reels)",
                        isGranted = audioGranted
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action Buttons
                if (isPermanentlyDenied) {
                    Button(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageAmber,
                            contentColor = VintageBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "OPEN APP SETTINGS",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = VintageAmber,
                            contentColor = VintageBlack
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "REMOVE LENS CAP (GRANT ACCESS)",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = VintageMetalSilver
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VintageMetalDark)
                ) {
                    Text(
                        text = "DEVICE SETTINGS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isGranted) VintageGreenAccent else VintageAmber,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = if (isGranted) VintageTextLight else VintageTextMuted,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isGranted) VintageGreenAccent else VintageShutterRed,
            modifier = Modifier.size(16.dp)
        )
    }
}
