package com.kegeltrainer.app.ui.theme

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    fun displayName(): String = when (this) {
        SYSTEM -> "跟随系统"
        LIGHT -> "浅色"
        DARK -> "深色"
    }
}
