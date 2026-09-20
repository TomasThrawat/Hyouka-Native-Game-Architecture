package com.tomasthrawat.gamearchitecture

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class TrackPose(
    val x: Float,
    val z: Float,
    val yawDegrees: Float
)

class Track(
    val definition: TrackDefinition = TrackDefinition(
        id = "demo_circuit",
        name = "Demo Circuit",
        lengthMeters = 140f,
        halfWidthMeters = 12f,
        halfDepthMeters = 30f
    )
) {
    fun pose(progress: Float, lateralOffset: Float = 0f): TrackPose {
        val p = ((progress % 1f) + 1f) % 1f
        val angle = p * Math.PI * 2.0
        val radiusX = definition.halfDepthMeters.toDouble()
        val radiusZ = definition.halfWidthMeters.toDouble()
        val centerX = cos(angle) * radiusX
        val centerZ = sin(angle) * radiusZ
        val tangentX = -sin(angle)
        val tangentZ = cos(angle)
        val normalX = -tangentZ
        val normalZ = tangentX
        return TrackPose(
            x = (centerX + normalX * lateralOffset).toFloat(),
            z = (centerZ + normalZ * lateralOffset).toFloat(),
            yawDegrees = Math.toDegrees(atan2(tangentX, tangentZ)).toFloat()
        )
    }
}
