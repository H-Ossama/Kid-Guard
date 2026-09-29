package com.parentalguard.parent.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Help
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.parentalguard.parent.R
import com.parentalguard.parent.security.PinManager
import com.parentalguard.parent.update.ChildUpdateSection
import com.parentalguard.parent.viewmodel.ChildDevice
import com.parentalguard.parent.ui.aura.auraEnter
import com.parentalguard.parent.ui.currentAppVersion
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.NeumorphicIconTile
import com.parentalguard.parent.ui.neumorphic.NeumorphicSwitch
import com.parentalguard.parent.ui.neumorphic.neumorphic
import com.parentalguard.parent.ui.theme.ThemePreferences
import com.parentalguard.parent.ui.theme.trackingFor
import java.util.Locale

@Composable
fun ControlScreen(
    modifier: Modifier = Modifier,
    devices: List<ChildDevice> = emptyList(),
    onLanguageChanged: (String) -> Unit = {},
    onOpenDeviceOwnerGuide: () -> Unit = {},
    onShareChildApk: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onOpenHelpSupport: () -> Unit = {}
) {
    var showPinDialog by remember { mutableStateOf(false) }
    var showPinManageDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("parent_prefs", android.content.Context.MODE_PRIVATE) }
    var biometricEnabled by remember { mutableStateOf(prefs.getBoolean("biometric_enabled", false)) }
    var currentThemeMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }
    var isPinSet by remember { mutableStateOf(PinManager.isPinSet(context)) }

    NeumorphicBackground {
        LazyColumn(
            modifier = modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 128.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(Modifier.auraEnter(0)) {
                    Text(
                        text = stringResource(R.string.control_title),
                        color = Nm.onSurface,
                        style = MaterialTheme.typography.displayMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.control_subtitle),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // -- Security -------------------------------------------------------
            item {
                ControlGroup(title = stringResource(R.string.security_title), index = 1) {
                    ControlRow(
                        icon = Icons.Outlined.Lock,
                        title = stringResource(R.string.pin_protection_title),
                        subtitle = stringResource(if (isPinSet) R.string.pin_status_enabled else R.string.pin_status_disabled),
                        onClick = { if (isPinSet) showPinManageDialog = true else showPinDialog = true }
                    )
                    NmDivider()
                    ControlSwitchRow(
                        icon = Icons.Outlined.Fingerprint,
                        title = stringResource(R.string.biometric_title),
                        subtitle = stringResource(R.string.biometric_desc),
                        checked = biometricEnabled,
                        onCheckedChange = {
                            biometricEnabled = it
                            prefs.edit().putBoolean("biometric_enabled", it).apply()
                        }
                    )
                    NmDivider()
                    ControlRow(
                        icon = Icons.Outlined.Shield,
                        title = stringResource(R.string.device_owner_guide_title),
                        subtitle = stringResource(R.string.device_owner_guide_settings_desc),
                        onClick = onOpenDeviceOwnerGuide
                    )
                    NmDivider()
                    ControlRow(
                        icon = Icons.Outlined.Share,
                        title = stringResource(R.string.share_child_apk_title),
                        subtitle = stringResource(R.string.share_child_apk_desc),
                        onClick = onShareChildApk
                    )
                }
            }

            // -- Child updates (GitHub release -> push to paired devices) -----
            item {
                Column(Modifier.auraEnter(2)) {
                    Text(
                        text = stringResource(R.string.child_update_title).uppercase(),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = trackingFor(1.4.sp),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                    ChildUpdateSection(devices = devices)
                }
            }

            // -- Appearance -----------------------------------------------------
            item {
                ControlGroup(title = stringResource(R.string.appearance_title), index = 3) {
                    ControlRow(
                        icon = Icons.Outlined.DarkMode,
                        title = stringResource(R.string.dark_mode_title),
                        subtitle = ThemePreferences.getThemeModeDisplayName(currentThemeMode),
                        onClick = { showThemeDialog = true }
                    )
                    NmDivider()
                    ControlRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.settings_language),
                        subtitle = currentLanguageName(),
                        onClick = { showLanguageDialog = true }
                    )
                    NmDivider()
                    ControlSwitchRow(
                        icon = Icons.Outlined.Notifications,
                        title = stringResource(R.string.push_notif_title),
                        subtitle = stringResource(R.string.push_notif_desc),
                        checked = notificationsEnabled,
                        onCheckedChange = { notificationsEnabled = it }
                    )
                }
            }

            // -- About ----------------------------------------------------------
            item {
                ControlGroup(title = stringResource(R.string.about_section_title), index = 4) {
                    ControlRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.about_title),
                        subtitle = stringResource(R.string.about_desc),
                        onClick = { onOpenAbout() }
                    )
                    NmDivider()
                    ControlRow(
                        icon = Icons.Outlined.Help,
                        title = stringResource(R.string.help_title),
                        subtitle = stringResource(R.string.help_desc),
                        onClick = { onOpenHelpSupport() }
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().auraEnter(5).padding(top = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    NeumorphicIconTile(
                        icon = Icons.Outlined.Shield,
                        tint = Nm.primary,
                        size = 52.dp,
                        iconSize = 24.dp
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.app_name) + " · " + stringResource(R.string.app_version, currentAppVersion(context)),
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    // -- Dialogs ------------------------------------------------------------

    if (showPinDialog) {
        PinSetupDialog(
            changing = isPinSet,
            onDismiss = { showPinDialog = false },
            onPinSet = { pin ->
                PinManager.setPin(context, pin)
                isPinSet = true
                showPinDialog = false
            }
        )
    }

    if (showPinManageDialog) {
        PinManageDialog(
            onDismiss = { showPinManageDialog = false },
            onRemove = {
                PinManager.disablePin(context)
                isPinSet = false
                showPinManageDialog = false
            },
            onChange = {
                showPinManageDialog = false
                showPinDialog = true
            }
        )
    }

    if (showLanguageDialog) {
        val currentCode = (AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()).language
        NmLanguageDialog(
            currentCode = currentCode,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode))
                onLanguageChanged(languageCode)
                showLanguageDialog = false
            }
        )
    }

    if (showThemeDialog) {
        NmThemeDialog(
            currentMode = currentThemeMode,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { mode ->
                currentThemeMode = mode
                ThemePreferences.saveThemeMode(context, mode)
                showThemeDialog = false
                (context as? android.app.Activity)?.recreate()
            }
        )
    }
}
// ============================================================================
// Rows & groups
// ============================================================================

@Composable
private fun NmDivider() {
    Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(Nm.divider))
}

@Composable
private fun ControlGroup(
    title: String,
    index: Int,
    content: @Composable () -> Unit
) {
    Column(Modifier.auraEnter(index)) {
        Text(
            text = title.uppercase(),
            color = Nm.onSurfaceMuted,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = trackingFor(1.4.sp),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
        )
        NeumorphicCard(padding = 0.dp, corner = 24.dp) {
            Column { content() }
        }
    }
}

@Composable
private fun ControlRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicIconTile(icon = icon, tint = Nm.primary, size = 38.dp, iconSize = 18.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(1.dp))
            Text(subtitle, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Outlined.ChevronRight, null, tint = Nm.onSurfaceMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ControlSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NeumorphicIconTile(icon = icon, tint = Nm.primary, size = 38.dp, iconSize = 18.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Nm.onSurface, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(1.dp))
            Text(subtitle, color = Nm.onSurfaceMuted, style = MaterialTheme.typography.bodySmall)
        }
        NeumorphicSwitch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// ============================================================================
// Dialogs
// ============================================================================

@Composable
private fun currentLanguageName(): String {
    val locale = AppCompatDelegate.getApplicationLocales().get(0) ?: Locale.getDefault()
    return when (locale.language) {
        "ar" -> stringResource(R.string.language_arabic)
        "fr" -> stringResource(R.string.language_french)
        else -> stringResource(R.string.language_english)
    }
}

@Composable
private fun NmLanguageDialog(
    currentCode: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(R.string.settings_language), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "en" to R.string.language_english,
                    "ar" to R.string.language_arabic,
                    "fr" to R.string.language_french
                ).forEach { (code, labelRes) ->
                    val selected = code == currentCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = if (selected) Nm.inset else Nm.surface,
                                elevation = if (selected) 3.dp else 7.dp,
                                pressed = selected
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onLanguageSelected(code) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(labelRes),
                            color = if (selected) Nm.primary else Nm.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (selected) {
                            Icon(Icons.Default.Check, null, tint = Nm.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun NmThemeDialog(
    currentMode: ThemePreferences.ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemePreferences.ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(R.string.dark_mode_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ThemePreferences.ThemeMode.values().forEach { mode ->
                    val selected = mode == currentMode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neumorphic(
                                shape = RoundedCornerShape(18.dp),
                                backgroundColor = if (selected) Nm.inset else Nm.surface,
                                elevation = if (selected) 3.dp else 7.dp,
                                pressed = selected
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onThemeSelected(mode) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = ThemePreferences.getThemeModeDisplayName(mode),
                            color = if (selected) Nm.primary else Nm.onSurface,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (selected) {
                            Icon(Icons.Default.Check, null, tint = Nm.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun PinSetupDialog(
    changing: Boolean = false,
    onDismiss: () -> Unit,
    onPinSet: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(if (changing) R.string.pin_dialog_change_title else R.string.pin_dialog_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    stringResource(if (changing) R.string.pin_dialog_change_desc else R.string.pin_dialog_desc),
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text(stringResource(R.string.enter_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Nm.onSurface,
                        unfocusedTextColor = Nm.onSurface,
                        focusedBorderColor = Nm.primary,
                        unfocusedBorderColor = Nm.fieldBorder,
                        focusedContainerColor = Nm.inset,
                        unfocusedContainerColor = Nm.inset,
                        focusedLabelColor = Nm.primary,
                        unfocusedLabelColor = Nm.onSurfaceMuted,
                        cursorColor = Nm.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) confirmPin = it },
                    label = { Text(stringResource(R.string.confirm_pin)) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = error != null,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Nm.onSurface,
                        unfocusedTextColor = Nm.onSurface,
                        focusedBorderColor = Nm.primary,
                        unfocusedBorderColor = Nm.fieldBorder,
                        focusedContainerColor = Nm.inset,
                        unfocusedContainerColor = Nm.inset,
                        focusedLabelColor = Nm.primary,
                        unfocusedLabelColor = Nm.onSurfaceMuted,
                        cursorColor = Nm.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error != null) {
                    Text(error!!, color = Nm.danger, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            NeumorphicButton(
                text = stringResource(if (changing) R.string.pin_save_btn else R.string.set_pin_btn),
                onClick = {
                    when {
                        pin.length < 4 -> error = context.getString(R.string.pin_error_short)
                        pin != confirmPin -> error = context.getString(R.string.pin_error_mismatch)
                        else -> onPinSet(pin)
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}

@Composable
private fun PinManageDialog(
    onDismiss: () -> Unit,
    onRemove: () -> Unit,
    onChange: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Nm.surface,
        title = { Text(stringResource(R.string.pin_protection_title), color = Nm.onSurface, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.pin_manage_desc),
                    color = Nm.onSurfaceMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
                NeumorphicButton(
                    text = stringResource(R.string.pin_remove_btn),
                    onClick = onRemove,
                    icon = Icons.Outlined.LockOpen,
                    tint = Nm.danger,
                    iconTint = Nm.danger,
                    inset = true,
                    modifier = Modifier.fillMaxWidth()
                )
                NeumorphicButton(
                    text = stringResource(R.string.pin_change_btn),
                    onClick = onChange,
                    icon = Icons.Outlined.Edit,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = Nm.onSurfaceMuted) }
        }
    )
}
