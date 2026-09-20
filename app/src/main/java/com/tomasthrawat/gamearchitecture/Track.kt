package com.tomasthrawat.gamearchitecture

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class TrackPose(val x: Float, val z: Float, val yawDegrees: Float)

class Track(
    val definition: TrackDefinition = TrackDefinition(
        id = "demo_circuit",
        name = "Demo Circuit",
        lengthMeters = 260f,
        halfWidthMeters = 12f,
        halfDepthMeters = 30f
    )
) {
    fun pose(progress: Float, lateralOffset: Float = 0f): TrackPose {
        val p = ((progress % 1f) + 1f) % 1f
        val angle = p * Math.PI * 2.0
        val radiusX = definition.halfDepthMeters
        val radiusZ = definition.halfWidthMeters
        val x = (cos(angle) * radiusX).toFloat()
        val z = (sin(angle) * radiusZ).toFloat()
        val tangentX = (-sin(angle)).toFloat()
        val tangentZ = cos(angle).toFloat()
        val normalX = -tangentZ
        val normalZ = tangentX
        return TrackPose(
            x = x + normalX * lateralOffset,
            z = z + normalZ * lateralOffset,
            yawDegrees = Math.toDegrees(
                atan2(tangentX.toDouble(), tangentZ.toDouble())
            ).toFloat()
        )
    }
}
