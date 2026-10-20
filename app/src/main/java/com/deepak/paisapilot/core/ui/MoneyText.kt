package com.deepak.paisapilot.core.ui

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.deepak.paisapilot.domain.money.Paise
import com.deepak.paisapilot.domain.money.formatInr
import com.deepak.paisapilot.ui.theme.AmountTextStyle
import com.deepak.paisapilot.ui.theme.LocalStatusColors
import com.deepak.paisapilot.ui.theme.PaisaPilotTheme

enum class MoneyStatus {
    Owed,
    Owe,
    Settled,
}

@Composable
fun MoneyText(
    paise: Paise,
    modifier: Modifier = Modifier,
    status: MoneyStatus? = null,
    style: TextStyle = AmountTextStyle,
) {
    val statusColors = LocalStatusColors.current
    val color = when (status) {
        MoneyStatus.Owed -> statusColors.owed
        MoneyStatus.Owe -> statusColors.owe
        MoneyStatus.Settled -> statusColors.settled
        null -> MaterialTheme.colorScheme.onSurface
    }
    Text(
        text = formatInr(paise),
        modifier = modifier,
        color = color,
        style = style,
    )
}

@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MoneyTextPreview() {
    PaisaPilotTheme(dynamicColor = false) {
        MoneyText(paise = Paise(12550), status = MoneyStatus.Owed)
    }
}
