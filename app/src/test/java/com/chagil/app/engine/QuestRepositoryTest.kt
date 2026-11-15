package com.chagil.app.engine

import com.chagil.app.vehicle.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestRepositoryTest {

    @Test
    fun `parses the rain drill file`() {
        val q = rainDrill()
        assertEquals("rain_drill", q.id)
        assertEquals(QuestLevel.L1, q.level)
        assertEquals(45_000, q.limitMs)
        assertEquals(4, q.steps.size)
        assertEquals(Signal.LOW_BEAM_SWITCH, q.steps[0].signal)
        assertTrue(q.steps[0].selfCheckIfUnreadable)
        assertEquals(ControlPoint.LIGHT_STALK, q.steps[0].controlPoint)
        assertEquals("핸들 왼쪽 레버 끝을 돌려요.", q.steps[0].where?.voice)
        assertEquals(Signal.DEFROST_FRONT, q.steps[2].signal) // short form "HVAC.defroster" + area
        assertTrue(q.steps[3].isRestore)
        assertTrue(q.validate().isEmpty())
    }

    @Test
    fun `unknown keys and unknown enum values do not break loading`() {
        val q = QuestRepository.parse(
            """{"id": "x", "level": "L9", "availability": "someday", "brandNew": {"a": 1},
                "steps": [{"read": "Wiper.switch", "to": "off", "future": true}]}""",
        )
        assertEquals(QuestLevel.L1, q.level)
        assertEquals(Availability.OPEN, q.availability)
        assertTrue(q.steps[0].matches("OFF"))
    }

    @Test
    fun `coming soon quests load`() {
        val q = QuestRepository.parse("""{"id": "belt", "availability": "coming_soon", "label": null}""")
        assertEquals(Availability.COMING_SOON, q.availability)
        assertNull(q.label)
        assertTrue(q.validate().isEmpty())
    }

    @Test
    fun `broken files are skipped and reported, the rest are sorted`() {
        val repo = QuestRepository {
            listOf(
                "b.json" to """{"id": "b", "order": 2}""",
                "broken.json" to "{ nope",
                "a.json" to """{"id": "a", "order": 1, "steps": [{"read": "Nope.signal", "to": "ON"}]}""",
            )
        }
        assertEquals(listOf("a", "b"), repo.all().map { it.id })
        assertTrue(repo.errors.any { it.startsWith("broken.json") })
        assertTrue(repo.errors.any { it.contains("Nope.signal") })
        assertEquals("b", repo.get("b")?.id)
    }

    @Test
    fun `conditions are case insensitive and booleans accept true and false`() {
        val step = Step(read = "HVAC.defroster.FRONT", to = "true")
        assertTrue(step.matches("ON"))
        assertTrue(Step(read = "Wiper.switch", notAnyOf = listOf("OFF")).matches("MIST"))
        assertTrue(!Step(read = "Wiper.switch", notAnyOf = listOf("OFF")).matches("OFF"))
    }
}
