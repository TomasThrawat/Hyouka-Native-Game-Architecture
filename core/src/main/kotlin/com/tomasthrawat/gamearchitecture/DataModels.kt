package com.tomasthrawat.gamearchitecture

data class CarDefinition(
    val id: String,
    val model: String,
    val massKg: Float,
    val maxSpeedKmh: Float
)

data class TrackDefinition(
    val id: String = "demo_circuit",
    val name: String = "Demo Circuit",
    val lengthMeters: Float = 430f,
    val halfWidthMeters: Float = 6f,
    val halfDepthMeters: Float = 30f
)

data class CarState(
    val id: String,
    val massKg: Float = 1200f,
    val maxSpeedKmh: Float = 310f,
    var speedMetersPerSecond: Float = 0f,
    var progress: Float = 0f,
    var lateralOffset: Float = 0f,
    var yawDegrees: Float = 0f,
    var lap: Int = 0
)

data class GameInput(
    val throttle: Float = 0f,
    val brake: Float = 0f,
    val steer: Float = 0f
)

data class CarFrame(
    val id: String,
    val speedKmh: Float,
    val progress: Float,
    val lateralOffset: Float,
    val yawDegrees: Float,
    val lap: Int
)

data class RaceFrame(
    val lap: Int,
    val totalLaps: Int,
    val position: Int,
    val finished: Boolean,
    val elapsedSeconds: Float
)

data class GameSnapshot(
    val player: CarFrame,
    val opponents: List<CarFrame>,
    val race: RaceFrame
)
