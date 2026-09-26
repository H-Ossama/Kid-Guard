package com.parentalguard.parent.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.parentalguard.parent.R
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.theme.MonoFontFamily

@Composable
fun DeviceOwnerGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val adbCommand = stringResource(R.string.device_owner_guide_adb_command)

    NeumorphicBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().auraEnter(0),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicIconTile(
                        icon = Icons.Default.ArrowBack,
                        onClick = onBack,
                        contentDescription = stringResource(R.string.back)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.device_owner_guide_title),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.device_owner_guide_subtitle),
                            color = Nm.onSurfaceMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    NeumorphicIconTile(
                        icon = Icons.Default.Shield,
                        tint = Nm.primary,
                        contentDescription = stringResource(R.string.device_owner_guide_title)
                    )
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(1),
                    padding = 20.dp,
                    corner = 24.dp,
                    backgroundColor = Nm.primary.copy(alpha = 0.06f)
                ) {
                    Text(
                        text = stringResource(R.string.device_owner_guide_intro),
                        color = Nm.onSurface,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // ADB command with copy button
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(2),
                    padding = 16.dp,
                    corner = 20.dp
                ) {
                    Text(
                        text = stringResource(R.string.device_owner_guide_adb_label),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = adbCommand,
                        color = Nm.onSurface,
                        fontFamily = MonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.device_owner_guide_copy),
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("adb-command", adbCommand))
                            Toast.makeText(
                                context,
                                context.getString(R.string.device_owner_guide_copied),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        icon = Icons.Default.ContentCopy,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Ordered 9-phase tutorial
            item { PhaseStep(1, R.string.device_owner_guide_phase_1_title, R.string.device_owner_guide_phase_1_body, 3) }
            item { PhaseStep(2, R.string.device_owner_guide_phase_2_title, R.string.device_owner_guide_phase_2_body, 4) }
            item { PhaseStep(3, R.string.device_owner_guide_phase_3_title, R.string.device_owner_guide_phase_3_body, 5) }
            item { PhaseStep(4, R.string.device_owner_guide_phase_4_title, R.string.device_owner_guide_phase_4_body, 6) }
            item { PhaseStep(5, R.string.device_owner_guide_phase_5_title, R.string.device_owner_guide_phase_5_body, 7) }
            item { PhaseStep(6, R.string.device_owner_guide_phase_6_title, R.string.device_owner_guide_phase_6_body, 8) }
            item { PhaseStep(7, R.string.device_owner_guide_phase_7_title, R.string.device_owner_guide_phase_7_body, 9) }
            item { PhaseStep(8, R.string.device_owner_guide_phase_8_title, R.string.device_owner_guide_phase_8_body, 10) }
            item { PhaseStep(9, R.string.device_owner_guide_phase_9_title, R.string.device_owner_guide_phase_9_body, 11) }

            // Troubleshooting
            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(12),
                    padding = 16.dp,
                    corner = 20.dp,
                    backgroundColor = Nm.warning.copy(alpha = 0.07f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, null, tint = Nm.warning)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.device_owner_guide_trouble_title),
                            color = Nm.onSurface,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.device_owner_guide_trouble_body),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            item {
                NeumorphicCard(
                    modifier = Modifier.fillMaxWidth().auraEnter(13),
                    padding = 16.dp,
                    corner = 20.dp
                ) {
                    Text(
                        text = stringResource(R.string.device_owner_guide_note),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseStep(number: Int, titleRes: Int, bodyRes: Int, animIndex: Int) {
    NeumorphicCard(
        modifier = Modifier.fillMaxWidth().auraEnter(animIndex),
        padding = 16.dp,
        corner = 20.dp
    ) {
        Row(verticalAlignment = Alignment.Top) {
            PhaseNumberBadge(number = number)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "$number. ${stringResource(titleRes)}",
                    color = Nm.onSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(bodyRes),
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun PhaseNumberBadge(number: Int) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .neumorphic(shape = CircleShape, backgroundColor = Nm.surface, elevation = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number.toString(),
            color = Nm.primary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
