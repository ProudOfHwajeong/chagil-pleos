package com.chagil.app.engine

import com.chagil.app.vehicle.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DrillEngineTest {

    private val quest = rainDrill()

    @Test
    fun `step passes only after the value stays right for stableMs`() = runTest {
        val rig = Rig(this)
        rig.engine.start(quest)
        assertEquals(StepStatus.WAITING, rig.running.stepStatus)

        rig.advance(2_000)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        assertEquals(StepStatus.STABILIZING, rig.running.stepStatus)
        assertEquals("ON", rig.running.lastRead)

        rig.advance(900)
        assertEquals(0, rig.running.stepIndex)

        rig.advance(100)
        assertEquals(1, rig.running.stepIndex)
        assertEquals("00:03.0 · LowBeam 스위치 AUTOMATIC → ON · 1초 안정 · 통과", rig.lines.last())
    }

    @Test
    fun `leaving the value before stableMs restarts the stable timer`() = runTest {
        val rig = Rig(this)
        rig.engine.start(quest)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.advance(600)
        rig.set(Signal.LOW_BEAM_SWITCH, "OFF")
        assertEquals(StepStatus.WAITING, rig.running.stepStatus)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.advance(900)
        assertEquals(0, rig.running.stepIndex)
        rig.advance(100)
        assertEquals(1, rig.running.stepIndex)
    }

    @Test
    fun `toAnyOf accepts any listed value`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest)
        rig.advance(1_000)
        assertEquals(1, rig.running.stepIndex)

        rig.set(Signal.WIPER_SWITCH, "CONTINUOUS_LEVEL_1")
        rig.advance(1_000)
        assertEquals(2, rig.running.stepIndex)
        assertTrue(rig.lines.last().endsWith("Wiper 스위치 OFF → CONTINUOUS_LEVEL_1 · 1초 안정 · 통과"))
    }

    @Test
    fun `wrong value shows a hint at once and logs it, the right value clears it`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest)
        rig.advance(1_000)

        rig.advance(3_000)
        rig.set(Signal.WIPER_SWITCH, "MIST")
        val hint = rig.running.hint
        assertNotNull(hint)
        assertEquals("지금은 한 번 닦기예요. 반대쪽으로 한 칸", hint!!.text)
        assertEquals("반대쪽으로 한 칸 옮겨 보세요.", hint.voice)
        assertEquals(HintKind.WRONG_VALUE, hint.kind)
        assertEquals("MIST", rig.running.lastRead)
        assertEquals("00:04.0 · Wiper 스위치 OFF → MIST · 오조작: \"지금은 한 번 닦기예요. 반대쪽으로 한 칸\"", rig.lines.last())

        rig.set(Signal.WIPER_SWITCH, "INTERMITTENT_LEVEL_1")
        assertNull(rig.running.hint)
        rig.advance(1_000)
        assertEquals(2, rig.running.stepIndex)
        assertTrue(rig.lines.last().endsWith("Wiper 스위치 MIST → INTERMITTENT_LEVEL_1 · 1초 안정 · 통과"))
    }

    @Test
    fun `wrong map also accepts a plain string hint`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest)
        rig.advance(1_000)
        rig.set(Signal.WIPER_SWITCH, "CONTINUOUS_LEVEL_2")
        assertEquals("너무 빨라요. 한 칸 내려요", rig.running.hint?.text)
        assertNull(rig.running.hint?.voice)
    }

    @Test
    fun `already satisfied value passes after stableMs and is logged as already on`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest)
        assertEquals(StepStatus.STABILIZING, rig.running.stepStatus)
        rig.advance(1_000)
        assertEquals(1, rig.running.stepIndex)
        assertEquals("00:01.0 · LowBeam 스위치 ON · 이미 켜져 있음 · 통과", rig.lines.last())
    }

    @Test
    fun `alreadyOn other than pass needs a real change`() = runTest {
        val strict = quest.copy(steps = listOf(quest.steps[0].copy(alreadyOn = "change")))
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(strict)
        rig.advance(2_000)
        assertEquals(StepStatus.WAITING, rig.running.stepStatus)
        rig.set(Signal.LOW_BEAM_SWITCH, "OFF")
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.advance(1_000)
        assertTrue(rig.state is DrillState.Completed)
    }

    @Test
    fun `unreadable signal asks for self check and the button passes the step`() = runTest {
        val rig = Rig(this)
        rig.car.setUnreadable(Signal.LOW_BEAM_SWITCH)
        rig.engine.start(quest)
        assertEquals(StepStatus.SELF_CHECK_NEEDED, rig.running.stepStatus)
        assertNull(rig.running.lastRead)

        rig.advance(2_000)
        assertTrue(rig.engine.selfCheck())
        assertEquals(1, rig.running.stepIndex)
        assertEquals("00:02.0 · LowBeam 스위치 읽을 수 없음 · 운전자 확인 · 통과", rig.lines.last())
        assertFalse("self check only works while needed", rig.engine.selfCheck())
    }

    @Test
    fun `self checked controls are not lit on the car map`() = runTest {
        val records = FakeRecords()
        val rig = Rig(this, records)
        rig.car.setUnreadable(Signal.LOW_BEAM_SWITCH)
        rig.engine.start(quest)
        rig.engine.selfCheck()
        rig.set(Signal.WIPER_SWITCH, "INTERMITTENT_LEVEL_1"); rig.advance(1_000)
        rig.set(Signal.DEFROST_FRONT, "ON"); rig.advance(1_000)
        rig.set(Signal.WIPER_SWITCH, "OFF"); rig.advance(1_000)
        val done = rig.state as DrillState.Completed
        assertEquals(setOf("WIPER_STALK", "HVAC_FRONT_DEFROST"), done.litControls)
        assertEquals(setOf("WIPER_STALK", "HVAC_FRONT_DEFROST"), records.saved.single().second)
    }

    @Test
    fun `full rain drill completes with the plan's log lines and previous best`() = runTest {
        val records = FakeRecords(best = 14_300)
        val rig = Rig(this, records)
        rig.engine.start(quest)

        rig.advance(1_100); rig.set(Signal.LOW_BEAM_SWITCH, "ON"); rig.advance(1_000)          // 00:02.1
        rig.advance(1_900); rig.set(Signal.WIPER_SWITCH, "MIST")                                // 00:04.0
        rig.advance(600); rig.set(Signal.WIPER_SWITCH, "INTERMITTENT_LEVEL_1"); rig.advance(1_000) // 00:05.6
        rig.advance(2_300); rig.set(Signal.DEFROST_FRONT, "ON"); rig.advance(1_000)             // 00:08.9
        rig.advance(1_300); rig.set(Signal.WIPER_SWITCH, "OFF"); rig.advance(1_000)             // 00:11.2

        val done = rig.state as DrillState.Completed
        assertEquals("rain_drill", done.questId)
        assertEquals(11_200, done.elapsedMs)
        assertEquals(14_300L, done.previousBestMs)
        assertEquals(
            listOf(
                "00:00.0 · 폭우 드릴 시작",
                "00:02.1 · LowBeam 스위치 AUTOMATIC → ON · 1초 안정 · 통과",
                "00:04.0 · Wiper 스위치 OFF → MIST · 오조작: \"지금은 한 번 닦기예요. 반대쪽으로 한 칸\"",
                "00:05.6 · Wiper 스위치 MIST → INTERMITTENT_LEVEL_1 · 1초 안정 · 통과",
                "00:08.9 · HVAC 앞유리 디프로스터 OFF → ON · 1초 안정 · 통과",
                "00:11.2 · Wiper 스위치 → OFF · 되돌리기 · 통과",
                "00:11.2 · 드릴 완료 · 11.2초 · 지난 기록보다 3.1초 빠름",
            ),
            rig.lines,
        )
        assertEquals(11_200L, records.best)
        assertEquals(setOf("LIGHT_STALK", "WIPER_STALK", "HVAC_FRONT_DEFROST"), done.litControls)
    }

    @Test
    fun `first completion has no previous best`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest.copy(steps = quest.steps.take(1)))
        rig.advance(1_000)
        val done = rig.state as DrillState.Completed
        assertNull(done.previousBestMs)
        assertTrue(rig.lines.last().endsWith("첫 기록"))
    }

    @Test
    fun `time limit ends the drill with passed steps marked`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest)
        rig.advance(1_000)
        rig.advance(43_000)
        assertTrue(rig.state is DrillState.Running)
        assertEquals(1_000, rig.running.remainingMs)
        rig.advance(1_000)
        val out = rig.state as DrillState.TimedOut
        assertEquals(1, out.stepIndex)
        assertEquals(listOf(true, false, false, false), out.passed)
        assertEquals("00:45.0 · 시간 종료 · 1/4단계 통과", rig.lines.last())
    }

    @Test
    fun `shifting to D pauses for safety, remaining time is kept and resume continues`() = runTest {
        val records = FakeRecords()
        val rig = Rig(this, records)
        rig.engine.start(quest)
        rig.advance(10_000)
        rig.set(Signal.GEAR, "GEAR_DRIVE")

        val paused = rig.state as DrillState.Paused
        assertEquals(PauseReason.SAFETY, paused.reason)
        assertEquals(35_000, paused.snapshot.remainingMs)
        assertEquals(DrillSnapshot("rain_drill", 0, 35_000, 10_000), records.snapshot)
        assertFalse(rig.engine.resumable.value)
        assertFalse("cannot resume while driving", rig.engine.resume())

        rig.advance(60_000) // driving time does not count, and no time-out
        assertTrue(rig.state is DrillState.Paused)

        rig.set(Signal.GEAR, "GEAR_PARK")
        assertTrue("이어서 할까요?", rig.engine.resumable.value)
        assertTrue(rig.state is DrillState.Paused)
        assertTrue(rig.engine.resume())
        assertEquals(35_000, rig.running.remainingMs)
        assertNull(records.snapshot)

        rig.advance(5_000)
        assertEquals(30_000, rig.running.remainingMs)
    }

    @Test
    fun `a stable timer does not survive a pause`() = runTest {
        val rig = Rig(this)
        rig.engine.start(quest)
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.advance(800)
        rig.engine.pause()
        rig.advance(5_000)
        rig.engine.resume()
        rig.advance(500)
        assertEquals(0, rig.running.stepIndex)
        rig.advance(500)
        assertEquals(1, rig.running.stepIndex)
    }

    @Test
    fun `starting an L1 quest in neutral without confirmation starts paused until the driver confirms`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.GEAR, "GEAR_NEUTRAL")
        rig.engine.start(quest)
        assertEquals(PauseReason.SAFETY, (rig.state as DrillState.Paused).reason)
        assertFalse(rig.engine.resume())

        rig.safety.confirmParkedByDriver()
        rig.test.testScheduler.runCurrent()
        assertTrue(rig.engine.resumable.value)
        assertTrue(rig.engine.resume())
        assertEquals(45_000, rig.running.remainingMs)
    }

    @Test
    fun `an L2 quest stays paused at L1 even with confirmation`() = runTest {
        val rig = Rig(this)
        rig.set(Signal.GEAR, "GEAR_NEUTRAL")
        rig.safety.confirmParkedByDriver()
        rig.engine.start(quest.copy(level = QuestLevel.L2))
        assertTrue(rig.state is DrillState.Paused)
        rig.set(Signal.GEAR, "GEAR_PARK")
        assertTrue(rig.engine.resume())
    }

    @Test
    fun `user pause becomes a safety pause when the car starts moving`() = runTest {
        val rig = Rig(this)
        rig.engine.start(quest)
        rig.engine.pause()
        assertEquals(PauseReason.USER, (rig.state as DrillState.Paused).reason)
        rig.set(Signal.GEAR, "GEAR_REVERSE")
        assertEquals(PauseReason.SAFETY, (rig.state as DrillState.Paused).reason)
    }

    @Test
    fun `guard shows a hint while violated and never fails the drill`() = runTest {
        val frost = QuestRepository.parse(
            """
            {"id": "frost_morning", "title": "성에 낀 아침", "level": "L1", "limitSec": 60,
             "steps": [
               {"id": "front", "read": "HVAC.defroster.FRONT", "to": "ON", "control": "HVAC_FRONT_DEFROST"},
               {"id": "rear", "read": "HVAC.defroster.REAR", "to": "ON", "control": "HVAC_REAR_DEFROST"},
               {"id": "heat", "read": "SteeringWheel.heat", "notAnyOf": ["0"], "control": "STEERING_HEAT_BUTTON"}
             ],
             "guards": [{"read": "Wiper.switch", "notAnyOf": ["OFF"], "text": "언 유리에서는 와이퍼를 꺼 두세요", "voice": "와이퍼는 꺼 두세요."}]}
            """.trimIndent(),
        )
        val rig = Rig(this)
        rig.engine.start(frost)
        assertNull(rig.running.hint)

        rig.advance(1_500)
        rig.set(Signal.WIPER_SWITCH, "INTERMITTENT_LEVEL_1")
        assertEquals("언 유리에서는 와이퍼를 꺼 두세요", rig.running.hint?.text)
        assertEquals(HintKind.GUARD, rig.running.hint?.kind)
        assertEquals("00:01.5 · Wiper 스위치 INTERMITTENT_LEVEL_1 · 주의: \"언 유리에서는 와이퍼를 꺼 두세요\"", rig.lines.last())

        // Steps still pass while the guard is violated.
        rig.set(Signal.DEFROST_FRONT, "ON"); rig.advance(1_000)
        assertEquals(1, rig.running.stepIndex)
        assertEquals(HintKind.GUARD, rig.running.hint?.kind)

        rig.set(Signal.WIPER_SWITCH, "OFF")
        assertNull(rig.running.hint)

        rig.set(Signal.DEFROST_REAR, "ON"); rig.advance(1_000)
        rig.set(Signal.STEERING_HEAT, "2"); rig.advance(1_000)
        assertTrue(rig.state is DrillState.Completed)
    }

    @Test
    fun `abort returns to idle and clears the paused snapshot`() = runTest {
        val records = FakeRecords()
        val rig = Rig(this, records)
        rig.engine.start(quest)
        rig.engine.pause()
        assertNotNull(records.snapshot)
        rig.engine.abort()
        assertEquals(DrillState.Idle, rig.state)
        assertNull(records.snapshot)
        assertNull(rig.engine.quest.value)
    }

    @Test
    fun `resumeFrom continues a saved drill at its step with its remaining time`() = runTest {
        val rig = Rig(this)
        rig.engine.resumeFrom(quest, DrillSnapshot("rain_drill", 2, 20_000, 25_000))
        assertEquals(2, rig.running.stepIndex)
        assertEquals(20_000, rig.running.remainingMs)
    }

    @Test
    fun `events announce step starts, wrong values and completion`() = runTest {
        val rig = Rig(this)
        val events = mutableListOf<DrillEvent>()
        backgroundScope.launchCollect(rig.engine, events)
        rig.test.testScheduler.runCurrent()
        rig.set(Signal.LOW_BEAM_SWITCH, "ON")
        rig.engine.start(quest.copy(steps = quest.steps.take(2)))
        rig.advance(1_000)
        rig.set(Signal.WIPER_SWITCH, "MIST")
        rig.set(Signal.WIPER_SWITCH, "INTERMITTENT_LEVEL_1"); rig.advance(1_000)
        val kinds = events.map { it::class.simpleName }
        assertEquals(
            listOf("StepStarted", "StepPassed", "StepStarted", "WrongValue", "StepPassed", "Completed"),
            kinds,
        )
        assertTrue((events[1] as DrillEvent.StepPassed).alreadyOn)
    }

    @Test
    fun `log time format`() {
        assertEquals("00:05.6", LogFormat.clock(5_600))
        assertEquals("01:02.3", LogFormat.clock(62_345))
        assertEquals("11.2", LogFormat.seconds(11_250))
        assertEquals("지난 기록보다 3.1초 빠름", LogFormat.comparison(11_200, 14_300))
        assertEquals("지난 기록 9.0초", LogFormat.comparison(11_200, 9_000))
    }
}

private fun CoroutineScope.launchCollect(engine: DrillEngine, into: MutableList<DrillEvent>) =
    launch { engine.events.collect { into += it } }
