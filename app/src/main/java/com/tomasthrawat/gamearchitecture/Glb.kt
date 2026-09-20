package com.tomasthrawat.gamearchitecture

import android.content.Context
import org.json.JSONArray

class Glb(private val context: Context) {
    fun exists(path: String): Boolean = runCatching {
        context.assets.open(path).use { true }
    }.getOrDefault(false)

    fun isGlb(path: String): Boolean = runCatching {
        context.assets.open(path).use { input ->
            val header = ByteArray(4)
            input.read(header) == 4 &&
                header.contentEquals(byteArrayOf(0x67, 0x6c, 0x54, 0x46))
        }
    }.getOrDefault(false)

    fun readCars(path: String = "cars.json"): List<CarDefinition> {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            CarDefinition(
                id = item.getString("id"),
                model = item.getString("model"),
                massKg = item.getDouble("massKg").toFloat(),
                maxSpeedKmh = item.getDouble("maxSpeedKmh").toFloat()
            )
        }
    }

    fun readTracks(path: String = "tracks.json"): List<TrackDefinition> {
        val text = context.assets.open(path).bufferedReader().use { it.readText() }
        val array = JSONArray(text)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TrackDefinition(
                id = item.getString("id"),
                name = item.getString("name"),
                lengthMeters = item.getDouble("lengthMeters").toFloat(),
                halfWidthMeters = item.getDouble("halfWidthMeters").toFloat(),
                halfDepthMeters = item.getDouble("halfDepthMeters").toFloat()
            )
        }
    }
}
