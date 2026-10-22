package com.deepak.paisapilot.core.ui

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.deepak.paisapilot.R
import com.deepak.paisapilot.domain.time.isAllowedReportingDate
import com.deepak.paisapilot.domain.time.localDateFromUtcPickerMillis
import com.deepak.paisapilot.domain.time.utcPickerMillisFromLocalDate
import java.time.LocalDate

/** Only place that touches the experimental date picker APIs; see DECISIONS.md. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectDialog(
    initial: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = utcPickerMillisFromLocalDate(initial),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                isAllowedReportingDate(localDateFromUtcPickerMillis(utcTimeMillis))

            override fun isSelectableYear(year: Int): Boolean = year in 2000..2100
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onDateSelected(localDateFromUtcPickerMillis(it)) }
                },
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    ) {
        DatePicker(state = state)
    }
}
