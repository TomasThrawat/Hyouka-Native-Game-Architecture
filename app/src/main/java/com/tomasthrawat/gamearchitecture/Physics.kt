package com.tomasthrawat.gamearchitecture

import kotlin.math.sin

class Physics {
    fun update(car: CarState, input: GameInput, track: TrackDefinition, dt: Float) {
        val step = dt.coerceIn(0f, 0.05f)
        val throttle = input.throttle.coerceIn(0f, 1f)
        val brake = input.brake.coerceIn(0f, 1f)
        val acceleration = throttle * 18f
        val braking = brake * 30f
        val drag = if (throttle > 0f) 0.018f else 0.55f

        car.speedMetersPerSecond += (acceleration - braking - car.speedMetersPerSecond * drag) * step
        car.speedMetersPerSecond = car.speedMetersPerSecond.coerceIn(0f, 86f)

        val speedFactor = (car.speedMetersPerSecond / 40f).coerceIn(0.15f, 1.5f)
        car.yawDegrees += input.steer.coerceIn(-1f, 1f) * 95f * speedFactor * step

        val yaw = Math.toRadians(car.yawDegrees.toDouble())
        car.lateralOffset += sin(yaw).toFloat() * car.speedMetersPerSecond * step * 0.035f
        car.lateralOffset = car.lateralOffset.coerceIn(
            -track.halfWidthMeters + 1.2f,
            track.halfWidthMeters - 1.2f
        )

        var nextProgress = car.progress + car.speedMetersPerSecond * step / track.lengthMeters
        while (nextProgress >= 1f) {
            nextProgress -= 1f
            car.lap += 1
        }
        car.progress = nextProgress
        if (car.speedMetersPerSecond < 0.01f) car.speedMetersPerSecond = 0f
    }
}
