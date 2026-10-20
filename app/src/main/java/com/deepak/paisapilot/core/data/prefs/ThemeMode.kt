package com.deepak.paisapilot.core.data.prefs

enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        System -> systemDark
        Light -> false
        Dark -> true
    }
}
