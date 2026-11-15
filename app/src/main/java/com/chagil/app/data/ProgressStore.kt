package com.chagil.app.data

import android.content.Context
import android.content.SharedPreferences
import com.chagil.app.engine.DrillRecords
import com.chagil.app.engine.DrillSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

/**
 * On-device progress only (no network, no personal data): best time per quest, lit ControlPoint keys
 * for the "내 차 도면", and the paused drill for "이어서 할까요?".
 */
class ProgressStore(private val prefs: SharedPreferences) : DrillRecords {

    constructor(context: Context) : this(context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE))

    private val _bestTimes = MutableStateFlow(readBestTimes())
    /** questId → best elapsed ms. */
    val bestTimes: StateFlow<Map<String, Long>> = _bestTimes.asStateFlow()

    private val _litControls = MutableStateFlow(prefs.getStringSet(KEY_LIT, emptySet()).orEmpty().toSet())
    /** ControlPoint keys (e.g. "WIPER_STALK") the car has confirmed at least once. */
    val litControls: StateFlow<Set<String>> = _litControls.asStateFlow()

    private val _pausedSnapshot = MutableStateFlow(readSnapshot())
    val pausedSnapshot: StateFlow<DrillSnapshot?> = _pausedSnapshot.asStateFlow()

    override fun bestMs(questId: String): Long? = _bestTimes.value[questId]

    override fun saveResult(questId: String, elapsedMs: Long, litControls: Set<String>) {
        val editor = prefs.edit()
        val best = bestMs(questId)
        if (best == null || elapsedMs < best) {
            editor.putLong(KEY_BEST_PREFIX + questId, elapsedMs)
            _bestTimes.value = _bestTimes.value + (questId to elapsedMs)
        }
        val lit = _litControls.value + litControls
        if (lit != _litControls.value) {
            editor.putStringSet(KEY_LIT, lit)
            _litControls.value = lit
        }
        editor.apply()
    }

    fun lightControls(keys: Set<String>) {
        val lit = _litControls.value + keys
        prefs.edit().putStringSet(KEY_LIT, lit).apply()
        _litControls.value = lit
    }

    override fun savePausedSnapshot(snapshot: DrillSnapshot) {
        prefs.edit().putString(KEY_SNAPSHOT, json.encodeToString(DrillSnapshot.serializer(), snapshot)).apply()
        _pausedSnapshot.value = snapshot
    }

    override fun clearPausedSnapshot() {
        if (_pausedSnapshot.value == null && !prefs.contains(KEY_SNAPSHOT)) return
        prefs.edit().remove(KEY_SNAPSHOT).apply()
        _pausedSnapshot.value = null
    }

    /** Clears everything (demo reset). */
    fun resetAll() {
        prefs.edit().clear().apply()
        _bestTimes.value = emptyMap()
        _litControls.value = emptySet()
        _pausedSnapshot.value = null
    }

    private fun readBestTimes(): Map<String, Long> = prefs.all
        .filterKeys { it.startsWith(KEY_BEST_PREFIX) }
        .mapNotNull { (k, v) -> (v as? Long)?.let { k.removePrefix(KEY_BEST_PREFIX) to it } }
        .toMap()

    private fun readSnapshot(): DrillSnapshot? = prefs.getString(KEY_SNAPSHOT, null)?.let {
        runCatching { json.decodeFromString(DrillSnapshot.serializer(), it) }.getOrNull()
    }

    private companion object {
        const val PREFS = "chagil_progress"
        const val KEY_BEST_PREFIX = "best."
        const val KEY_LIT = "lit_controls"
        const val KEY_SNAPSHOT = "paused_snapshot"
        val json = Json { ignoreUnknownKeys = true }
    }
}
