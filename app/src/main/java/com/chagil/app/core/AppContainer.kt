package com.chagil.app.core

import android.content.Context
import android.os.SystemClock
import com.chagil.app.data.ProgressStore
import com.chagil.app.engine.DrillEngine
import com.chagil.app.engine.QuestRepository
import com.chagil.app.safety.SafetyLevel
import com.chagil.app.safety.SafetyMonitor
import com.chagil.app.vehicle.Diagnostics
import com.chagil.app.vehicle.SimulatedVehicleSource
import com.chagil.app.vehicle.SwitchingVehicleHub
import com.chagil.app.voice.VoiceGuide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Manual DI. One per process: `AppContainer.get(context)`. Call [start] once (e.g. from MainActivity.onCreate);
 * it is idempotent. The hub keeps running while the activity is hidden so a drill can pause on D
 * and offer "이어서 할까요?" when the car is parked again.
 */
class AppContainer private constructor(context: Context) {

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val diagnostics = Diagnostics()
    val hub = SwitchingVehicleHub(context, scope, diagnostics)
    val simulator: SimulatedVehicleSource get() = hub.simulation
    val safety = SafetyMonitor(hub, scope)
    val quests = QuestRepository(context)
    val progress = ProgressStore(context)
    val engine = DrillEngine(
        hub = hub,
        safety = safety,
        clock = SystemClock::elapsedRealtime,
        scope = scope,
        records = progress,
    )
    val voice = VoiceGuide(context)

    private var started = false

    init {
        // L0: no sound (plan: "소리와 알림도 내지 않는다").
        scope.launch { safety.level.collect { if (it == SafetyLevel.L0_DRIVING) voice.stop() } }
    }

    fun start() {
        if (started) return
        started = true
        hub.start()
    }

    companion object {
        @Volatile private var instance: AppContainer? = null

        fun get(context: Context): AppContainer =
            instance ?: synchronized(this) {
                instance ?: AppContainer(context.applicationContext).also { instance = it }
            }
    }
}
