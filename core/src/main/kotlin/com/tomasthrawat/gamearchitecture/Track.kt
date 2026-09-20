package com.tomasthrawat.gamearchitecture

import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

data class TrackPose(
    val x: Float,
    val z: Float,
    val yawDegrees: Float
)

class Track(val definition: TrackDefinition = TrackDefinition()) {
    private var layout = 0
    private val layouts = listOf(
        listOf(
            -72f to -18f, -36f to -18f, 0f to -18f, 36f to -18f,
            68f to -8f, 68f to 24f, 44f to 48f, 4f to 58f,
            -34f to 48f, -62f to 24f
        ),
        listOf(
            -78f to -8f, -42f to -8f, -10f to -8f, 18f to 4f,
            48f to 30f, 26f to 58f, -12f to 52f, -42f to 32f,
            -66f to 48f, -82f to 28f
        ),
        listOf(
            -78f to -26f, -34f to -26f, 10f to -18f, 58f to -4f,
            60f to 24f, 32f to 48f, -2f to 36f, -28f to 14f,
            -58f to 22f, -72f to 52f, -26f to 72f, 26f to 64f,
            72f to 42f
        )
    )

    private val lengthCache = FloatArray(layouts.size)

    fun selectLayout(i: Int) {
        layout = i.coerceIn(0, layouts.lastIndex)
    }

    fun waypoints(): List<Pair<Float, Float>> = layouts[layout]

    fun lengthMeters(): Float {
        if (lengthCache[layout] > 0f) return lengthCache[layout]

        val points = sampledCenterline(32)
        var length = 0f
        for (i in points.indices) {
            val a = points[i]
            val b = points[(i + 1) % points.size]
            val dx = b.first - a.first
            val dz = b.second - a.second
            length += sqrt(dx * dx + dz * dz)
        }

        lengthCache[layout] = length.coerceAtLeast(1f)
        return lengthCache[layout]
    }

    fun sampledCenterline(samplesPerSegment: Int = 24): List<Pair<Float, Float>> {
        val samples = samplesPerSegment.coerceAtLeast(2)
        val count = layouts[layout].size * samples
        return List(count) { index ->
            val progress = index.toFloat() / count.toFloat()
            val pose = pose(progress)
            pose.x to pose.z
        }
    }

    fun pose(progress: Float, lateralOffset: Float = 0f): TrackPose {
        val p = layouts[layout]
        val n = p.size
        val u = ((progress % 1f) + 1f) % 1f * n
        val i = floor(u).toInt() % n
        val t = u - floor(u)

        val a = p[(i - 1 + n) % n]
        val b = p[i]
        val c = p[(i + 1) % n]
        val d = p[(i + 2) % n]

        fun cr(x: Float, y: Float, z: Float, w: Float): Float {
            return 0.5f * (
                (2f * y) +
                    (-x + z) * t +
                    (2f * x - 5f * y + 4f * z - w) * t * t +
                    (-x + 3f * y - 3f * z + w) * t * t * t
            )
        }

        fun derivative(x: Float, y: Float, z: Float, w: Float): Float {
            return 0.5f * (
                (-x + z) +
                    2f * (2f * x - 5f * y + 4f * z - w) * t +
                    3f * (-x + 3f * y - 3f * z + w) * t * t
            )
        }

        val x = cr(a.first, b.first, c.first, d.first)
        val z = cr(a.second, b.second, c.second, d.second)

        var tx = derivative(a.first, b.first, c.first, d.first)
        var tz = derivative(a.second, b.second, c.second, d.second)
        var tangentLength = sqrt(tx * tx + tz * tz)

        if (tangentLength < 0.001f) {
            tx = c.first - b.first
            tz = c.second - b.second
            tangentLength = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
        }

        val nx = -tz / tangentLength
        val nz = tx / tangentLength
        val yaw = Math.toDegrees(atan2(tx.toDouble(), tz.toDouble())).toFloat()

        return TrackPose(
            x + nx * lateralOffset,
            z + nz * lateralOffset,
            yaw
        )
    }
}
