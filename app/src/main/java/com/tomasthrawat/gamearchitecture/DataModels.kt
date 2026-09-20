package com.tomasthrawat.gamearchitecture

data class CarDefinition(
    val id: String,
    val model: String,
    val mass: Float,
    val maxSpeedKmh: Float
)

data class TrackDefinition(
    val id: String,
    val name: String,
    val model: String,
    val lapLengthMeters: Float
)

data class CarState(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f,
    var yaw: Float = 0f,
    var speedMetersPerSecond: Float = 0f
)

data class GameInput(
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val steer: Float = 0f
)
