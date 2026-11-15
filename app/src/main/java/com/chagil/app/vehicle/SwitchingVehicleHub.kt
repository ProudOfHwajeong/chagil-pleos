package com.chagil.app.vehicle

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The hub the app uses. Tries the real Pleos SDK first; falls back to [simulation] when
 * `initialize()` throws or no gear/wiper value arrives within [probeTimeoutMs] (3 s).
 * [useSimulation] switches by hand (the simulator panel / demo).
 *
 * Driving restriction: in SDK mode the platform value is used. In simulation the simulated value is used,
 * but a real platform `true` always wins (safety first, even while simulating).
 */
class SwitchingVehicleHub internal constructor(
    private val sdkFactory: () -> VehicleSignalHub,
    /** The simulator to drive from the simulator panel. Only active when [mode] is SIMULATION. */
    val simulation: SimulatedVehicleSource,
    private val platformUx: StateFlow<Boolean?>,
    private val onStartPlatform: () -> Unit,
    private val onStopPlatform: () -> Unit,
    private val scope: CoroutineScope,
    private val log: Diagnostics,
    private val probeTimeoutMs: Long = PROBE_TIMEOUT_MS,
) : VehicleSignalHub {

    constructor(
        context: Context,
        scope: CoroutineScope = MainScope(),
        diagnostics: Diagnostics = Diagnostics(),
    ) : this(context.applicationContext, UxRestrictionWatcher(context, diagnostics), scope, diagnostics)

    private constructor(
        context: Context,
        watcher: UxRestrictionWatcher,
        scope: CoroutineScope,
        diagnostics: Diagnostics,
    ) : this(
        sdkFactory = { PleosVehicleSource(context, diagnostics, watcher.restricted) },
        simulation = SimulatedVehicleSource(),
        platformUx = watcher.restricted,
        onStartPlatform = watcher::start,
        onStopPlatform = watcher::stop,
        scope = scope,
        log = diagnostics,
    )

    private val _mode = MutableStateFlow(SourceMode.SIMULATION)
    override val mode: StateFlow<SourceMode> = _mode.asStateFlow()

    private val _readings = MutableStateFlow(simulation.readings.value)
    override val readings: StateFlow<Map<Signal, Reading>> = _readings.asStateFlow()

    override val uxRestricted: StateFlow<Boolean?> =
        combine(_mode, platformUx, simulation.uxRestricted) { mode, platform, simulated ->
            when {
                platform == true -> true
                mode == SourceMode.PLEOS_SDK -> platform
                else -> simulated
            }
        }.stateIn(scope, SharingStarted.Eagerly, simulation.uxRestricted.value)

    /** Recent connection errors, newest last. */
    val diagnostics: StateFlow<List<String>> = log.entries

    /** true while waiting (up to 3 s) for the first real value. */
    private val _probing = MutableStateFlow(false)
    val probing: StateFlow<Boolean> = _probing.asStateFlow()

    /** true when the simulation was chosen by hand (not by fallback). */
    private val _forcedSimulation = MutableStateFlow(false)
    val forcedSimulation: StateFlow<Boolean> = _forcedSimulation.asStateFlow()

    private var started = false
    private var sdk: VehicleSignalHub? = null
    private var mirrorJob: Job? = null
    private var probeJob: Job? = null

    override fun start() {
        if (started) return
        started = true
        runCatching(onStartPlatform).onFailure { log.add("주행 제한 감시 시작 안 됨 · ${it.shortMessage()}") }
        if (_forcedSimulation.value) activateSimulation() else tryPleos()
    }

    override fun stop() {
        if (!started) return
        started = false
        probeJob?.cancel()
        mirrorJob?.cancel()
        stopSdk()
        runCatching(onStopPlatform)
        _probing.value = false
    }

    /** true → simulation (stays until called with false). false → try the real SDK again. */
    fun useSimulation(enabled: Boolean) {
        _forcedSimulation.value = enabled
        if (!started) return
        if (enabled) {
            probeJob?.cancel()
            stopSdk()
            activateSimulation()
        } else {
            tryPleos()
        }
    }

    private fun tryPleos() {
        probeJob?.cancel()
        stopSdk()
        val source = try {
            sdkFactory().also { it.start() }
        } catch (t: Throwable) {
            log.add("차 신호 연결 안 됨 → 시뮬레이션 · ${t.shortMessage()}")
            activateSimulation()
            return
        }
        sdk = source
        _mode.value = SourceMode.PLEOS_SDK
        mirror(source)
        _probing.value = true
        probeJob = scope.launch {
            val gotValue = withTimeoutOrNull(probeTimeoutMs) {
                source.readings.first { it[Signal.GEAR] is Reading.Value || it[Signal.WIPER_SWITCH] is Reading.Value }
            } != null
            _probing.value = false
            if (!gotValue) {
                log.add("${probeTimeoutMs / 1000}초 안에 기어·와이퍼 값이 오지 않음 → 시뮬레이션")
                stopSdk()
                activateSimulation()
            }
        }
    }

    private fun activateSimulation() {
        _probing.value = false
        _mode.value = SourceMode.SIMULATION
        mirror(simulation)
    }

    private fun mirror(source: VehicleSignalHub) {
        mirrorJob?.cancel()
        _readings.value = source.readings.value
        mirrorJob = scope.launch { source.readings.collect { _readings.value = it } }
    }

    private fun stopSdk() {
        val s = sdk ?: return
        sdk = null
        runCatching { s.stop() }.onFailure { log.add("SDK 정리 안 됨 · ${it.shortMessage()}") }
    }

    companion object {
        const val PROBE_TIMEOUT_MS = 3_000L
    }
}
