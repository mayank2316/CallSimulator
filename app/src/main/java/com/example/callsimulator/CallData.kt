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
    val status: String = "Scheduled",
    val photoUri: String = ""
)

data class CallerProfile(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val name: String,
    val number: String = "",
    val ringtoneUri: String = "",
    val vibration: Boolean = true,
    val photoUri: String = ""
)

object CallStore {
    private const val PREFS = "call_simulator"
    private const val CALLS = "calls"
    private const val SETTINGS = "settings"
    private const val PROFILES = "profiles"

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
                    status = o.optString("status", "Scheduled"),
                    photoUri = o.optString("photoUri", "")
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
                put("vibration", c.vibration); put("status", c.status); put("photoUri", c.photoUri)
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

    fun profiles(context: Context): List<CallerProfile> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(PROFILES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                CallerProfile(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    label = o.optString("label", o.optString("name", "Caller")),
                    name = o.optString("name", ""),
                    number = o.optString("number", ""),
                    ringtoneUri = o.optString("ringtoneUri", ""),
                    vibration = o.optBoolean("vibration", true),
                    photoUri = o.optString("photoUri", "")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveProfile(context: Context, profile: CallerProfile) {
        val list = profiles(context).filterNot { it.id == profile.id } + profile
        val arr = JSONArray()
        list.forEach { p -> arr.put(JSONObject().apply {
            put("id", p.id); put("label", p.label); put("name", p.name); put("number", p.number)
            put("ringtoneUri", p.ringtoneUri); put("vibration", p.vibration); put("photoUri", p.photoUri)
        }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PROFILES, arr.toString()).apply()
    }

    fun removeProfile(context: Context, id: String) {
        val arr = JSONArray()
        profiles(context).filterNot { it.id == id }.forEach { p -> arr.put(JSONObject().apply {
            put("id", p.id); put("label", p.label); put("name", p.name); put("number", p.number)
            put("ringtoneUri", p.ringtoneUri); put("vibration", p.vibration); put("photoUri", p.photoUri)
        }) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(PROFILES, arr.toString()).apply()
    }

    fun setting(context: Context, key: String, default: String = ""): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, default) ?: default

    fun setSetting(context: Context, key: String, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, value).apply()
    }
}
