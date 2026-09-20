package com.tomasthrawat.gamearchitecture

import kotlin.math.sin

class Ai(private val physics: Physics, private val collision: Collision) {
    fun update(car: CarState, track: Track, dt: Float, phase: Float) {
        val targetKmh = (car.maxSpeedKmh - 25f) + 8f * sin(phase)
        val currentKmh = car.speedMetersPerSecond * 3.6f
        val throttle = if (currentKmh < targetKmh) 1f else 0.25f
        val steer =
            (-car.lateralOffset /
                track.definition.halfWidthMeters.coerceAtLeast(1f) * 0.85f)
                .coerceIn(-1f, 1f)

        physics.update(
            car,
            GameInput(throttle = throttle, steer = steer),
            track,
            dt
        )
        collision.resolve(car, track.definition)
    }
}
