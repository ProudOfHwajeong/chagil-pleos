package com.chagil.app.engine

import android.content.Context
import kotlinx.serialization.json.Json

/**
 * Loads every JSON file in `assets/quests`. A broken file never crashes the app: it is skipped and reported in [errors].
 * Quests are sorted by `order`, then id.
 */
class QuestRepository(private val source: () -> List<Pair<String, String>>) {

    constructor(context: Context) : this({ readAssets(context.applicationContext) })

    private val loaded: Pair<List<Quest>, List<String>> by lazy { load() }

    /** All quests, open and coming-soon. */
    fun all(): List<Quest> = loaded.first

    fun get(id: String): Quest? = all().firstOrNull { it.id == id }

    /** File-level problems (parse errors, validation issues) for the log panel. */
    val errors: List<String> get() = loaded.second

    private fun load(): Pair<List<Quest>, List<String>> {
        val errors = mutableListOf<String>()
        val files = try {
            source()
        } catch (t: Throwable) {
            errors += "퀘스트 폴더를 읽지 못함 · ${t.message}"
            emptyList()
        }
        val quests = files.mapNotNull { (name, text) ->
            try {
                parse(text).also { q -> q.validate().forEach { errors += "$name · $it" } }
            } catch (t: Throwable) {
                errors += "$name · 읽을 수 없음 · ${t.message?.take(120)}"
                null
            }
        }.distinctBy { it.id }.sortedWith(compareBy({ it.order }, { it.id }))
        return quests to errors
    }

    companion object {
        const val ASSET_DIR = "quests"

        val json: Json = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            isLenient = true
        }

        fun parse(text: String): Quest = json.decodeFromString(Quest.serializer(), text)

        private fun readAssets(context: Context): List<Pair<String, String>> {
            val names = context.assets.list(ASSET_DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
            return names.map { name ->
                name to context.assets.open("$ASSET_DIR/$name").bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        }
    }
}
