package com.chagil.app.vehicle

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Small ring buffer of recent connection problems, shown in the log panel. Never contains personal data. */
class Diagnostics(
    private val clock: () -> Long = System::currentTimeMillis,
    private val capacity: Int = 30,
) {
    private val _entries = MutableStateFlow<List<String>>(emptyList())
    val entries: StateFlow<List<String>> = _entries.asStateFlow()

    fun add(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date(clock()))
        _entries.update { (it + "$time · $message").takeLast(capacity) }
    }

    fun clear() {
        _entries.value = emptyList()
    }
}

internal fun Throwable.shortMessage(): String =
    "${this::class.java.simpleName}${message?.let { ": ${it.take(120)}" } ?: ""}"
