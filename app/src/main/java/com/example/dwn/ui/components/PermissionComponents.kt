package com.example.dwn.ui.components

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.dwn.util.PermissionManager

/**
 * Permission request dialog that shows all required permissions
 * and their purposes in a user-friendly way
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionRequestDialog(
    missingPermissions: List<PermissionManager.PermissionInfo>,
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "MediaGrab needs the following permissions to provide full functionality",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Permission list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(missingPermissions) { permissionInfo ->
                        PermissionItem(permissionInfo = permissionInfo)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Grant permissions button
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Grant Permissions",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Open settings button
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Open Settings")
                    }

                    // Skip button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Maybe Later",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionItem(permissionInfo: PermissionManager.PermissionInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Permission icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = getPermissionIcon(permissionInfo.icon),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = permissionInfo.feature,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = permissionInfo.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status icon
        Icon(
            imageVector = if (permissionInfo.isGranted)
                Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (permissionInfo.isGranted)
                Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun getPermissionIcon(iconName: String): ImageVector {
    return when (iconName) {
        "notifications" -> Icons.Default.Notifications
        "music_note" -> Icons.Default.MusicNote
        "video_library" -> Icons.Default.VideoLibrary
        "image" -> Icons.Default.Image
        "folder" -> Icons.Default.Folder
        "mic" -> Icons.Default.Mic
        "camera" -> Icons.Default.CameraAlt
        "bluetooth" -> Icons.Default.Bluetooth
        "phone" -> Icons.Default.Phone
        "security" -> Icons.Default.Security
        else -> Icons.Default.Lock
    }
}

/**
 * Simple permission denied banner that can be shown in the app
 */
@Composable
fun PermissionDeniedBanner(
    message: String,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onRequestPermission) {
                Text("Grant")
            }
        }
    }
}

/**
 * Compact permission request chip
 */
@Composable
fun PermissionRequestChip(
    permissionName: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    AssistChip(
        onClick = onRequest,
        label = { Text("Grant $permissionName") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}

