package com.tomasthrawat.gamearchitecture

import kotlin.math.abs

class Collision {
    fun resolve(car: CarState, track: TrackDefinition): Boolean {
        val limit = (track.halfWidthMeters - 1.2f).coerceAtLeast(1f)
        val clamped = car.lateralOffset.coerceIn(-limit, limit)
        val hit = abs(car.lateralOffset - clamped) > 0.001f
        if (hit) {
            car.lateralOffset = clamped
            car.speedMetersPerSecond *= 0.72f
        }
        return hit
    }

    fun carsOverlap(first: CarState, second: CarState, trackLengthMeters: Float): Boolean {
        val distance = abs(first.progress - second.progress)
        val wrapped = minOf(distance, 1f - distance)
        return wrapped * trackLengthMeters < 4.5f &&
            abs(first.lateralOffset - second.lateralOffset) < 2.8f
    }
}
