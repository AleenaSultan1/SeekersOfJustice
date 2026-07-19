package com.seekersofjustice.app.ui.cases

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.seekersofjustice.app.R
import com.seekersofjustice.app.domain.model.RelationToCrime
import com.seekersofjustice.app.domain.model.Sex
import com.seekersofjustice.app.ui.common.labelRes

@Composable
fun AddCaseDialog(
    duplicateFirError: Boolean,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (fir: String, name: String, age: Int, sex: Sex, relation: RelationToCrime) -> Unit,
) {
    var fir by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var ageText by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf(Sex.MALE) }
    var relation by remember { mutableStateOf(RelationToCrime.VICTIM) }
    var showValidation by remember { mutableStateOf(false) }

    val age = ageText.toIntOrNull()
    val firValid = fir.isNotBlank()
    val nameValid = name.isNotBlank()
    val ageValid = age != null && age in 1..130

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_case)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = fir,
                    onValueChange = {
                        fir = it
                        onClearError()
                    },
                    label = { Text(stringResource(R.string.fir_number)) },
                    isError = duplicateFirError || (showValidation && !firValid),
                    supportingText = when {
                        duplicateFirError -> {
                            { Text(stringResource(R.string.error_fir_duplicate)) }
                        }
                        showValidation && !firValid -> {
                            { Text(stringResource(R.string.error_required)) }
                        }
                        else -> null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.full_name)) },
                    isError = showValidation && !nameValid,
                    supportingText = if (showValidation && !nameValid) {
                        { Text(stringResource(R.string.error_required)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter(Char::isDigit).take(3) },
                    label = { Text(stringResource(R.string.age)) },
                    isError = showValidation && !ageValid,
                    supportingText = if (showValidation && !ageValid) {
                        { Text(stringResource(R.string.error_invalid_age)) }
                    } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                EnumDropdown(
                    label = stringResource(R.string.sex),
                    options = Sex.entries,
                    selected = sex,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = { sex = it },
                )
                EnumDropdown(
                    label = stringResource(R.string.relation),
                    options = RelationToCrime.entries,
                    selected = relation,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelected = { relation = it },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showValidation = true
                if (firValid && nameValid && ageValid) {
                    onSave(fir, name, age!!, sex, relation)
                }
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdown(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
