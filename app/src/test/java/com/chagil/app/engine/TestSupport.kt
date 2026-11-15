package com.chagil.app.engine

import com.chagil.app.safety.SafetyMonitor
import com.chagil.app.vehicle.Signal
import com.chagil.app.vehicle.SimulatedVehicleSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent

fun resource(path: String): String =
    requireNotNull(Thread.currentThread().contextClassLoader?.getResourceAsStream(path)) { "missing $path" }
        .bufferedReader(Charsets.UTF_8).use { it.readText() }

fun rainDrill(): Quest = QuestRepository.parse(resource("quests/rain_drill.json"))

class FakeRecords(var best: Long? = null) : DrillRecords {
    val saved = mutableListOf<Pair<Long, Set<String>>>()
    var snapshot: DrillSnapshot? = null
    override fun bestMs(questId: String) = best
    override fun saveResult(questId: String, elapsedMs: Long, litControls: Set<String>) {
        saved += elapsedMs to litControls
        if (best == null || elapsedMs < best!!) best = elapsedMs
    }
    override fun savePausedSnapshot(snapshot: DrillSnapshot) { this.snapshot = snapshot }
    override fun clearPausedSnapshot() { snapshot = null }
}

/** A simulated car + safety + engine on virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class Rig(val test: TestScope, records: DrillRecords = FakeRecords()) {
    val car = SimulatedVehicleSource(clock = { test.testScheduler.currentTime })
    val safety = SafetyMonitor(car, test.backgroundScope)
    val engine = DrillEngine(car, safety, clock = { test.testScheduler.currentTime }, scope = test.backgroundScope, records = records)

    init { test.runCurrent() }

    fun set(signal: Signal, raw: String) {
        car.set(signal, raw)
        test.runCurrent()
    }

    /** Moves virtual time forward and runs everything due, including tasks exactly at the target time. */
    fun advance(ms: Long) {
        test.advanceTimeBy(ms)
        test.runCurrent()
    }

    val state get() = engine.state.value
    val running get() = engine.state.value as DrillState.Running
    val lines get() = engine.log.value.map { it.line }
}
