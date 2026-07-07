package com.fylgja.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "habit_cache")

class HabitStore(private val context: Context) {

    private val habitsKey = stringPreferencesKey("habits_json")

    private fun habitToJson(h: Habit): JSONObject = JSONObject().apply {
        put("name", h.name)
        put("icon", h.icon)
        put("color", h.color)
        put("type", h.type)
        put("clocks", JSONArray().apply {
            h.clockEntries.forEach { ce -> put(JSONObject().apply {
                put("date", ce.date); put("time", ce.time); put("full", ce.full)
            })}
        })
    }

    private fun habitFromJson(obj: JSONObject): Habit = Habit(
        name = obj.getString("name"),
        icon = obj.optString("icon", "📝"),
        color = obj.optString("color", "#3498db"),
        type = obj.optString("type", "counter"),
        clockEntries = obj.optJSONArray("clocks")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                ClockEntry(o.getString("date"), o.getString("time"), o.getString("full"))
            }
        } ?: emptyList()
    )

    fun saveHabits(habits: List<Habit>) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            context.dataStore.edit { prefs ->
                val arr = JSONArray()
                habits.forEach { arr.put(habitToJson(it)) }
                prefs[habitsKey] = arr.toString()
            }
        }
    }

    fun habitsFlow(): Flow<List<Habit>> = context.dataStore.data.map { prefs ->
        prefs[habitsKey]?.let { json ->
            val arr = JSONArray(json)
            (0 until arr.length()).map { habitFromJson(arr.getJSONObject(it)) }
        } ?: emptyList()
    }
}
