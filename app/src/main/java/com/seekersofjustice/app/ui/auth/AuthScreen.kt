package com.seekersofjustice.app.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seekersofjustice.app.R
import com.seekersofjustice.app.data.auth.BiometricAuth
import com.seekersofjustice.app.domain.model.Officer

private val NavyTop = Color(0xFF16406F)
private val NavyBottom = Color(0xFF0C2A52)
private const val PIN_LENGTH = 4

@Composable
fun AuthScreen(
    onAuthenticated: (Officer) -> Unit,
    viewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
) {
    val needsSetup by viewModel.needsSetup.collectAsStateWithLifecycle()
    val officers by viewModel.officers.collectAsStateWithLifecycle()
    var addingOfficer by remember { mutableStateOf(false) }

    AuthScaffold {
        when {
            needsSetup == null -> CircularProgressIndicator(color = Color.White)
            needsSetup == true || addingOfficer || officers.isEmpty() ->
                CreateOfficerFlow(
                    canCancel = officers.isNotEmpty(),
                    onCancel = { addingOfficer = false },
                    onCreated = { viewModel.createOfficer(it.first, it.second) { o -> onAuthenticated(o) } },
                )
            else ->
                SignInFlow(
                    officers = officers,
                    onAddOfficer = { addingOfficer = true },
                    onVerify = { id, pin, cb -> viewModel.signIn(id, pin) { cb(it) } },
                    onAuthenticated = onAuthenticated,
                )
        }
    }
}

@Composable
private fun AuthScaffold(content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyTop, NavyBottom))),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Surface(color = Color.White, shape = CircleShape, modifier = Modifier.size(96.dp)) {
                Image(
                    painter = painterResource(R.drawable.pp_crest),
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Text(
                stringResource(R.string.app_name),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            content()
        }
    }
}

@Composable
private fun CreateOfficerFlow(
    canCancel: Boolean,
    onCancel: () -> Unit,
    onCreated: (Pair<String, String>) -> Unit,
) {
    var step by remember { mutableStateOf(0) } // 0=name, 1=create pin, 2=confirm pin
    var name by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var mismatch by remember { mutableStateOf(false) }

    when (step) {
        0 -> {
            Text(
                stringResource(R.string.auth_officer_name),
                color = Color.White.copy(alpha = .85f),
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.auth_officer_name_hint), color = Color.White.copy(alpha = .5f)) },
                colors = whiteFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
            AuthTextButton(stringResource(R.string.auth_continue), enabled = name.isNotBlank()) { step = 1 }
            if (canCancel) AuthTextButton(stringResource(R.string.cancel), subtle = true, onClick = onCancel)
        }
        1 -> {
            PinEntry(
                title = stringResource(R.string.auth_create_title),
                hint = stringResource(R.string.auth_create_hint),
                pin = pin,
                error = if (mismatch) "mismatch" else null,
                errorText = stringResource(R.string.auth_passcodes_mismatch),
                onDigit = {
                    if (pin.length < PIN_LENGTH) { pin += it; mismatch = false }
                    if (pin.length == PIN_LENGTH) { firstPin = pin; pin = ""; step = 2 }
                },
                onBackspace = { pin = pin.dropLast(1) },
            )
        }
        2 -> {
            PinEntry(
                title = stringResource(R.string.auth_confirm_title),
                hint = null,
                pin = pin,
                error = null,
                onDigit = {
                    if (pin.length < PIN_LENGTH) pin += it
                    if (pin.length == PIN_LENGTH) {
                        if (pin == firstPin) onCreated(name to firstPin)
                        else { pin = ""; firstPin = ""; mismatch = true; step = 1 }
                    }
                },
                onBackspace = { pin = pin.dropLast(1) },
            )
        }
    }
}

@Composable
private fun SignInFlow(
    officers: List<Officer>,
    onAddOfficer: () -> Unit,
    onVerify: (Long, String, (Officer?) -> Unit) -> Unit,
    onAuthenticated: (Officer) -> Unit,
) {
    val context = LocalContext.current
    var selected by remember(officers) { mutableStateOf(officers.first()) }
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    if (officers.size > 1) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            OfficerPicker(officers = officers, selected = selected, onSelect = { selected = it; pin = ""; wrong = false })
        }
    } else {
        Text(selected.name, color = Color.White, fontWeight = FontWeight.Medium)
    }

    PinEntry(
        title = stringResource(R.string.auth_enter_title),
        hint = null,
        pin = pin,
        error = if (wrong) "wrong" else null,
        errorText = stringResource(R.string.auth_wrong_passcode),
        showBiometric = BiometricAuth.isAvailable(context),
        onBiometric = {
            (context as? FragmentActivity)?.let { activity ->
                BiometricAuth.prompt(
                    activity = activity,
                    title = activity.getString(R.string.auth_biometric_title),
                    subtitle = activity.getString(R.string.auth_biometric_subtitle),
                    negativeButton = activity.getString(R.string.auth_enter_title),
                    onSuccess = { onAuthenticated(selected) },
                )
            }
        },
        onDigit = {
            if (pin.length < PIN_LENGTH) { pin += it; wrong = false }
            if (pin.length == PIN_LENGTH) {
                onVerify(selected.id, pin) { officer ->
                    if (officer != null) onAuthenticated(officer) else { wrong = true; pin = "" }
                }
            }
        },
        onBackspace = { pin = pin.dropLast(1); wrong = false },
    )
    AuthTextButton(stringResource(R.string.auth_add_officer), subtle = true, onClick = onAddOfficer)
}

@Composable
private fun OfficerPicker(officers: List<Officer>, selected: Officer, onSelect: (Officer) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.auth_choose_officer), color = Color.White.copy(alpha = .7f), fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            officers.forEach { officer ->
                val active = officer.id == selected.id
                Surface(
                    onClick = { onSelect(officer) },
                    color = if (active) Color.White else Color.White.copy(alpha = .12f),
                    shape = CircleShape,
                ) {
                    Text(
                        officer.name,
                        color = if (active) NavyBottom else Color.White,
                        fontSize = 13.sp,
                        fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PinEntry(
    title: String,
    hint: String?,
    pin: String,
    error: String?,
    errorText: String? = null,
    showBiometric: Boolean = false,
    onBiometric: () -> Unit = {},
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.Medium)
        hint?.let {
            Text(it, color = Color.White.copy(alpha = .7f), fontSize = 12.5.sp, textAlign = TextAlign.Center)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(PIN_LENGTH) { i ->
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(if (i < pin.length) Color.White else Color.White.copy(alpha = .28f))
                )
            }
        }
        if (error != null && errorText != null) {
            Text(errorText, color = Color(0xFFF2A3A9), fontSize = 12.5.sp)
        }
        PinPad(onDigit = onDigit, onBackspace = onBackspace, showBiometric = showBiometric, onBiometric = onBiometric)
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    showBiometric: Boolean,
    onBiometric: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9")).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { d -> PadKey(d) { onDigit(d) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showBiometric) {
                PadIcon(Icons.Default.Fingerprint, stringResource(R.string.auth_unlock_biometric), onBiometric)
            } else {
                Spacer(Modifier.size(72.dp))
            }
            PadKey("0") { onDigit("0") }
            PadIcon(Icons.AutoMirrored.Filled.Backspace, stringResource(R.string.auth_delete_digit), onBackspace)
        }
    }
}

@Composable
private fun PadKey(digit: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Text(digit, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun PadIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, cd: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.size(72.dp)) {
        Icon(icon, contentDescription = cd, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun AuthTextButton(label: String, enabled: Boolean = true, subtle: Boolean = false, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            color = if (subtle) Color.White.copy(alpha = .7f) else Color.White,
            fontWeight = if (subtle) FontWeight.Normal else FontWeight.SemiBold,
        )
    }
}

@Composable
private fun whiteFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = Color.White,
    focusedBorderColor = Color.White,
    unfocusedBorderColor = Color.White.copy(alpha = .5f),
)
