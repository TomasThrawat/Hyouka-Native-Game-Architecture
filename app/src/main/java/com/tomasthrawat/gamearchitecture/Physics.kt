package com.tomasthrawat.gamearchitecture

class Physics {
    fun update(car: CarState, input: GameInput, dt: Float) {
        val throttle = input.throttle.coerceIn(0f, 1f)
        val brake = input.brake.coerceIn(0f, 1f)

        val acceleration = throttle * 12f
        val braking = brake * 22f

        car.speedMetersPerSecond += (acceleration - braking) * dt
        car.speedMetersPerSecond *= (1f - 0.8f * dt).coerceAtLeast(0f)
        car.speedMetersPerSecond = car.speedMetersPerSecond.coerceAtLeast(0f)

        car.yaw += input.steer.coerceIn(-1f, 1f) *
            (1.2f + car.speedMetersPerSecond * 0.08f) * dt

        val yaw = Math.toRadians(car.yaw.toDouble())
        car.x += kotlin.math.sin(yaw).toFloat() * car.speedMetersPerSecond * dt
        car.z += kotlin.math.cos(yaw).toFloat() * car.speedMetersPerSecond * dt
    }
}
