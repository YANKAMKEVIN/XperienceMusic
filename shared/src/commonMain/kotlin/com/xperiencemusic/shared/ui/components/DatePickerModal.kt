package com.xperiencemusic.shared.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.xperiencemusic.shared.resources.Res
import com.xperiencemusic.shared.resources.date_picker_cancel
import com.xperiencemusic.shared.resources.date_picker_ok
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

private const val MILLIS_PER_DAY = 86_400_000L

/** Material date picker speaking [LocalDate]; the picker itself works in UTC midnight millis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(
    initialDate: LocalDate,
    onDatePicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate.toEpochDays() * MILLIS_PER_DAY)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { onDatePicked(LocalDate.fromEpochDays(it / MILLIS_PER_DAY)) }
                    onDismiss()
                },
            ) { Text(stringResource(Res.string.date_picker_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.date_picker_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
