package com.parentalguard.parent.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.parent.R
import com.parentalguard.parent.security.BiometricHelper
import com.parentalguard.parent.security.PinManager
import com.parentalguard.parent.ui.neumorphic.Nm
import com.parentalguard.parent.ui.neumorphic.NeumorphicBackground
import com.parentalguard.parent.ui.neumorphic.NeumorphicButton
import com.parentalguard.parent.ui.neumorphic.NeumorphicCard
import com.parentalguard.parent.ui.neumorphic.neumorphic

@Composable
fun PinLockScreen(onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricAvailable = remember { activity?.let { BiometricHelper.isBiometricAvailable(it) } ?: false }

    fun requestBiometric() {
        activity?.let { BiometricHelper.showBiometricPrompt(it, onSuccess = onUnlocked, onError = { error = it }) }
    }
    LaunchedEffect(biometricAvailable) { if (biometricAvailable) requestBiometric() }

    NeumorphicBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .neumorphic(shape = CircleShape, backgroundColor = Nm.surface, elevation = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Lock, null, tint = Nm.primary, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(24.dp))
            NeumorphicCard(modifier = Modifier.fillMaxWidth(), corner = 30.dp, padding = 24.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.title_app_locked),
                        color = Nm.onSurface,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.desc_enter_pin),
                        textAlign = TextAlign.Center,
                        color = Nm.onSurfaceMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 6) { pin = it; error = null } },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(R.string.label_pin)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
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
                        )
                    )
                    if (error != null) {
                        Text(
                            text = error!!,
                            color = Nm.danger,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.action_unlock),
                        onClick = {
                            if (PinManager.verifyPin(context, pin)) onUnlocked()
                            else { error = context.getString(R.string.error_invalid_pin); pin = "" }
                        },
                        icon = Icons.Default.LockOpen,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (biometricAvailable) {
                        Spacer(Modifier.height(10.dp))
                        NeumorphicButton(
                            text = stringResource(R.string.action_unlock_biometric),
                            onClick = ::requestBiometric,
                            icon = Icons.Default.Fingerprint,
                            inset = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
