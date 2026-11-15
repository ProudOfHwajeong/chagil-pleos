package com.chagil.app.vehicle

import kotlinx.coroutines.flow.StateFlow

/** Where the values come from. The UI must label SIMULATION clearly ("시뮬레이션 — 실제 차 신호가 아닙니다"). */
enum class SourceMode { PLEOS_SDK, SIMULATION }

/** The single place the app receives vehicle values from. Read-only: nothing here changes the car. */
interface VehicleSignalHub {
    val mode: StateFlow<SourceMode>
    val readings: StateFlow<Map<Signal, Reading>>

    /** true = platform says driving restrictions apply. null = unknown (no Car service). */
    val uxRestricted: StateFlow<Boolean?>

    fun start()
    fun stop()
}

fun VehicleSignalHub.reading(signal: Signal): Reading = readings.value[signal] ?: Reading.Unknown
