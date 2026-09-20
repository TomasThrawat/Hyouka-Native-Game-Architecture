package com.tomasthrawat.gamearchitecture

import kotlin.math.abs

class Collision {
    fun resolve(
        car: CarState,
        track: TrackDefinition
    ): Boolean {
        val limit = (track.halfWidthMeters - 1.2f).coerceAtLeast(1f)
        val touchingBarrier = abs(car.lateralOffset) >= limit

        if (touchingBarrier) {
            car.speedMetersPerSecond *= 0.88f
            car.lateralOffset =
                car.lateralOffset.coerceIn(-limit, limit)
        }

        return touchingBarrier
    }

    fun carsOverlap(
        a: CarState,
        b: CarState,
        trackLengthMeters: Float
    ): Boolean {
        val progressDelta = abs(a.progress - b.progress)
        val loopDelta = minOf(progressDelta, 1f - progressDelta)
        val longitudinalDistance =
            loopDelta * trackLengthMeters.coerceAtLeast(1f)

        return longitudinalDistance < 4.5f &&
            abs(a.lateralOffset - b.lateralOffset) < 1.7f
    }
}
