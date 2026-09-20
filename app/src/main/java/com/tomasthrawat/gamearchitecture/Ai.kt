package com.tomasthrawat.gamearchitecture

import kotlin.math.sin

class Ai(
    private val physics: Physics,
    private val collision: Collision
) {
    fun update(
        car: CarState,
        track: TrackDefinition,
        dt: Float,
        phase: Float
    ) {
        val targetKmh =
            (car.maxSpeedKmh - 25f) + 8f * sin(phase)
        val currentKmh = car.speedMetersPerSecond * 3.6f
        val throttle =
            if (currentKmh < targetKmh) 1f else 0.25f
        val steer =
            (-car.lateralOffset /
                track.halfWidthMeters.coerceAtLeast(1f) * 0.85f)
                .coerceIn(-1f, 1f)

        physics.update(
            car = car,
            input = GameInput(
                throttle = throttle,
                steer = steer
            ),
            track = track,
            dt = dt
        )
        collision.resolve(car, track)
    }
}
