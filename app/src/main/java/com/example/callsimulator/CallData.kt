package com.example.callsimulator

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class SimCall(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String = "",
    val timeMillis: Long,
    val ringtoneUri: String = "",
    val vibration: Boolean = true,
    val status: String = "Scheduled"
)

object CallStore {
    private const val PREFS = "call_simulator"
    private const val CALLS = "calls"
    private const val SETTINGS = "settings"

    fun all(context: Context): List<SimCall> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CALLS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                SimCall(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    name = o.optString("name", "Unknown caller"),
                    number = o.optString("number", ""),
                    timeMillis = o.optLong("timeMillis", 0L),
                    ringtoneUri = o.optString("ringtoneUri", ""),
                    vibration = o.optBoolean("vibration", true),
                    status = o.optString("status", "Scheduled")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun save(context: Context, calls: List<SimCall>) {
        val arr = JSONArray()
        calls.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("number", c.number)
                put("timeMillis", c.timeMillis); put("ringtoneUri", c.ringtoneUri)
                put("vibration", c.vibration); put("status", c.status)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(CALLS, arr.toString()).apply()
    }

    fun upsert(context: Context, call: SimCall) {
        val list = all(context).filterNot { it.id == call.id } + call
        save(context, list)
    }

    fun updateStatus(context: Context, id: String, status: String) {
        save(context, all(context).map { if (it.id == id) it.copy(status = status) else it })
    }

    fun remove(context: Context, id: String) = save(context, all(context).filterNot { it.id == id })

    fun setting(context: Context, key: String, default: String = ""): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, default) ?: default

    fun setSetting(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}
