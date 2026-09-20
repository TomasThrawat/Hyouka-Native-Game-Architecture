package com.tomasthrawat.gamearchitecture

import kotlin.math.abs

class Collision {
    fun resolve(car: CarState, track: TrackDefinition) {
        val limit = track.halfWidthMeters - 1.2f
        if (car.lateralOffset <= -limit || car.lateralOffset >= limit) {
            car.speedMetersPerSecond *= 0.88f
        }
        car.lateralOffset = car.lateralOffset.coerceIn(-limit, limit)
    }

    fun carsOverlap(a: CarState, b: CarState): Boolean {
        val progressDistance = abs(a.progress - b.progress)
        val circularDistance = minOf(progressDistance, 1f - progressDistance)
        return circularDistance < 0.008f &&
            abs(a.lateralOffset - b.lateralOffset) < 1.6f
    }
}
