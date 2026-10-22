package com.deepak.paisapilot.feature.personal

import androidx.annotation.StringRes
import com.deepak.paisapilot.R
import com.deepak.paisapilot.domain.personal.Category

@StringRes
fun Category.labelRes(): Int = when (this) {
    Category.Food -> R.string.category_food
    Category.Transport -> R.string.category_transport
    Category.Shopping -> R.string.category_shopping
    Category.Bills -> R.string.category_bills
    Category.Home -> R.string.category_home
    Category.Health -> R.string.category_health
    Category.Entertainment -> R.string.category_entertainment
    Category.Other -> R.string.category_other
}
