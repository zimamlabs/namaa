package dev.namaa.app

actual fun systemLanguage(): String = java.util.Locale.getDefault().language
