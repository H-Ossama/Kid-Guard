package com.parentalguard.child.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.parentalguard.child.R
import com.parentalguard.child.ui.components.NeumorphicBackground
import com.parentalguard.child.ui.components.NeumorphicButton
import com.parentalguard.child.ui.components.NeumorphicCard
import com.parentalguard.child.ui.components.NeumorphicIconTile
import com.parentalguard.child.ui.theme.NeumorphicError
import com.parentalguard.child.ui.theme.NeumorphicOnSurface
import com.parentalguard.child.ui.theme.NeumorphicOnSurfaceMuted
import com.parentalguard.child.ui.theme.NeumorphicPrimary

/**
 * Protection-PIN screens for the child app settings ([com.parentalguard.child.MainActivity]).
 *
 * - [PinGateMode.SETUP]: first run (or after a parent reset) — choose a PIN.
 * - [PinGateMode.VERIFY]: every later open — enter the PIN to reach settings.
 *
 * The parent clears a forgotten PIN remotely ("Reset child PIN"), which flips
 * the app back to [PinGateMode.SETUP].
 */
enum class PinGateMode { SETUP, VERIFY }

@Composable
fun PinGateScreen(
    mode: PinGateMode,
    onSetupComplete: (String) -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember(mode) { mutableStateOf("") }
    var confirm by remember(mode) { mutableStateOf("") }
    var error by remember(mode) { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

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
            NeumorphicIconTile(
                icon = if (mode == PinGateMode.SETUP) Icons.Default.Shield else Icons.Default.Lock,
                tint = NeumorphicPrimary,
                size = 96.dp,
                iconSize = 44.dp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(
                    if (mode == PinGateMode.SETUP) R.string.pin_setup_title else R.string.pin_verify_title
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = NeumorphicOnSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(
                    if (mode == PinGateMode.SETUP) R.string.pin_setup_message else R.string.pin_verify_message
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = NeumorphicOnSurfaceMuted,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))

            NeumorphicCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.length <= 6 && it.all(Char::isDigit)) {
                                pin = it
                                error = null
                            }
                        },
                        label = { Text(stringResource(R.string.pin_enter)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (mode == PinGateMode.SETUP) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = confirm,
                            onValueChange = {
                                if (it.length <= 6 && it.all(Char::isDigit)) {
                                    confirm = it
                                    error = null
                                }
                            },
                            label = { Text(stringResource(R.string.pin_confirm)) },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            isError = error != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error!!,
                            color = NeumorphicError,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    NeumorphicButton(
                        text = stringResource(R.string.save),
                        onClick = {
                            if (mode == PinGateMode.SETUP) {
                                when {
                                    pin.length < 4 -> error =
                                        context.getString(R.string.pin_too_short)
                                    pin != confirm -> error =
                                        context.getString(R.string.pin_mismatch)
                                    else -> onSetupComplete(pin)
                                }
                            } else {
                                if (onVerify(pin)) {
                                    pin = ""
                                } else {
                                    error = context.getString(R.string.pin_incorrect)
                                    pin = ""
                                }
                            }
                        },
                        icon = Icons.Default.Lock,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
