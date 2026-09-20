package com.tomasthrawat.gamearchitecture

import kotlin.math.sin

class Ai(
    private val physics: Physics,
    private val collision: Collision
) {
    fun update(car: CarState, track: TrackDefinition, dt: Float, phase: Float) {
        val targetKmh = 245f + 25f * sin(phase)
        val currentKmh = car.speedMetersPerSecond * 3.6f
        val throttle = if (currentKmh < targetKmh) 1f else 0.35f
        val steer = -car.lateralOffset / track.halfWidthMeters * 0.85f
        physics.update(car, GameInput(throttle = throttle, steer = steer), track, dt)
        collision.resolve(car, track)
    }
}
