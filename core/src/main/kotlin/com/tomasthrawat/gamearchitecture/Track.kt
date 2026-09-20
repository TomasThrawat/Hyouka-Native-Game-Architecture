package com.tomasthrawat.gamearchitecture

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

data class TrackPose(
    val x: Float,
    val z: Float,
    val yawDegrees: Float
)

class Track(
    val definition: TrackDefinition = TrackDefinition()
) {
    private val radiusX = definition.halfDepthMeters.coerceAtLeast(18f)
    private val radiusZ = max(definition.halfWidthMeters * 3.2f, 16f)

    fun pose(progress: Float, lateralOffset: Float = 0f): TrackPose {
        val p = ((progress % 1f) + 1f) % 1f
        val angle = p * Math.PI * 2.0

        val centerX = cos(angle) * radiusX
        val centerZ = sin(angle) * radiusZ

        val tangentX = -sin(angle) * radiusX
        val tangentZ = cos(angle) * radiusZ
        val tangentLength = sqrt(tangentX * tangentX + tangentZ * tangentZ)

        val tx = tangentX / tangentLength
        val tz = tangentZ / tangentLength
        val nx = -tz
        val nz = tx

        return TrackPose(
            x = (centerX + nx * lateralOffset).toFloat(),
            z = (centerZ + nz * lateralOffset).toFloat(),
            yawDegrees = Math.toDegrees(atan2(tx.toDouble(), tz.toDouble())).toFloat()
        )
    }
}
