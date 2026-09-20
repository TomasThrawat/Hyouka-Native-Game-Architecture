package com.tomasthrawat.gamearchitecture

class Physics {
    fun update(car: CarState, input: GameInput, track: Track, dt: Float) {
        val step = dt.coerceIn(0f, 0.05f)
        val throttle = input.throttle.coerceIn(0f, 1f)
        val brake = input.brake.coerceIn(0f, 1f)
        val maxSpeed = (car.maxSpeedKmh / 3.6f).coerceAtLeast(1f)
        val speedRatio = (car.speedMetersPerSecond / maxSpeed).coerceIn(0f, 1.2f)
        val massFactor = (1200f / car.massKg).coerceIn(0.75f, 1.25f)

        val engineAcceleration =
            throttle * 18f * massFactor *
                (1f - speedRatio * 0.55f).coerceAtLeast(0.2f)
        val braking = brake * 32f
        val rollingDrag = if (throttle > 0.01f) 0.08f else 0.42f
        val aeroDrag = car.speedMetersPerSecond * car.speedMetersPerSecond * 0.0022f

        car.speedMetersPerSecond +=
            (engineAcceleration - braking - rollingDrag - aeroDrag) * step
        car.speedMetersPerSecond = car.speedMetersPerSecond.coerceIn(0f, maxSpeed)

        val steer = input.steer.coerceIn(-1f, 1f)
        car.lateralOffset +=
            (2.6f + car.speedMetersPerSecond * 0.115f) * steer * step

        val pose = track.pose(car.progress, car.lateralOffset)
        car.yawDegrees =
            pose.yawDegrees +
                (steer * (6f + car.speedMetersPerSecond * 0.24f))
                    .coerceIn(-18f, 18f)

        var progress =
            car.progress +
                car.speedMetersPerSecond * step /
                track.definition.lengthMeters.coerceAtLeast(1f)

        while (progress >= 1f) {
            progress -= 1f
            car.lap += 1
        }

        car.progress = progress
        if (car.speedMetersPerSecond < 0.01f) car.speedMetersPerSecond = 0f
    }
}
