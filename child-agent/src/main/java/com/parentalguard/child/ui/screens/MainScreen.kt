package com.parentalguard.child.ui.screens

import android.graphics.Bitmap
import androidx.compose.ui.res.stringResource
import com.parentalguard.child.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.child.ui.components.*
import com.parentalguard.child.ui.theme.*

@Composable
fun MainScreen(
    connectionString: String,
    status: String,
    qrBitmap: Bitmap?,
    deviceName: String,
    onRequestUnlock: () -> Unit,
    onHideIcon: () -> Unit,
    onRenameDevice: () -> Unit
) {
    var showFakeSettings by remember { mutableStateOf(true) }
    var clickCount by remember { mutableStateOf(0) }

    if (showFakeSettings) {
        FakeSystemSettings(
            onTitleClick = {
                clickCount++
                if (clickCount >= 7) {
                    showFakeSettings = false
                }
            }
        )
    } else {
        NeumorphicBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = stringResource(R.string.child_agent_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = NeumorphicOnSurface
                        )
                        Text(
                            text = deviceName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = NeumorphicOnSurfaceMuted,
                            modifier = Modifier.clickable {
                                // Secret way to go back to fake mode if needed
                                showFakeSettings = true
                                clickCount = 0
                            }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .neumorphic(
                                shape = CircleShape,
                                backgroundColor = NeumorphicSurface,
                                elevation = 5.dp
                            )
                            .clip(CircleShape)
                            .clickable(onClick = onHideIcon),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VisibilityOff,
                            contentDescription = stringResource(R.string.hide_icon_content_description),
                            tint = NeumorphicOnSurfaceMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // System Status Tile
                NeumorphicCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.status_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = NeumorphicOnSurfaceMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (status == stringResource(R.string.address_unavailable)) stringResource(R.string.status_no_network) else status,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = NeumorphicOnSurface
                            )
                        }

                        val isConnected = status != stringResource(R.string.address_unavailable) && status != stringResource(R.string.status_no_network)
                        NeumorphicStatusDot(
                            color = if (isConnected) NeumorphicOnline else NeumorphicOffline
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Pairing Code and QR Tile
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.pairing_code_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = NeumorphicOnSurfaceMuted
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = connectionString,
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                            color = NeumorphicPrimary
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Inset (concave) well holding the QR code
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .neumorphic(
                                    shape = RoundedCornerShape(24.dp),
                                    backgroundColor = NeumorphicSurfaceInset,
                                    elevation = 6.dp,
                                    pressed = true
                                )
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.qr_code_content_description),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(
                                    color = NeumorphicPrimary,
                                    trackColor = NeumorphicSurfaceInset,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.scan_instruction_child),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeumorphicOnSurfaceMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    NeumorphicButton(
                        text = stringResource(R.string.rename_device),
                        onClick = onRenameDevice,
                        icon = Icons.Default.Edit,
                        modifier = Modifier.weight(1f)
                    )

                    NeumorphicButton(
                        text = stringResource(R.string.request_unlock_short),
                        onClick = onRequestUnlock,
                        icon = Icons.Default.LockOpen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "Kid Guard v${com.parentalguard.child.BuildConfig.VERSION_NAME} (${com.parentalguard.child.BuildConfig.VERSION_CODE})",
                    style = MaterialTheme.typography.labelSmall,
                    color = NeumorphicOnSurfaceMuted.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
