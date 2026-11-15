package com.chagil.app.vehicle

/** One signal's latest value as the hub knows it. */
sealed interface Reading {
    /** A value read from the car (or the simulator). [atMs] is when the value last changed. */
    data class Value(val raw: String, val atMs: Long) : Reading

    /** The car refused or failed to give this value (2 failures in a row). */
    data object Unreadable : Reading

    /** Nothing received yet. */
    data object Unknown : Reading
}

val Reading?.rawOrNull: String? get() = (this as? Reading.Value)?.raw
