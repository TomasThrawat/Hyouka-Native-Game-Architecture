package com.tomasthrawat.gamearchitecture

class Game(
    val track: Track = Track(),
    val playerCar: CarDefinition = CarDefinition(
        "starter_car",
        "models/starter_car.glb",
        1200f,
        310f
    ),
    val opponentCar: CarDefinition = CarDefinition(
        "rival_car",
        "models/rival_car.glb",
        1250f,
        295f
    ),
    private val totalLaps: Int = 3
) {
    private val physics = Physics()
    private val collision = Collision()
    private val ai = Ai(physics, collision)
    private val race = Race(totalLaps)

    private val player = CarState("player", playerCar.massKg, playerCar.maxSpeedKmh)
    private val opponents = listOf(
        CarState("rival_1", opponentCar.massKg, opponentCar.maxSpeedKmh, lateralOffset = -3f),
        CarState("rival_2", opponentCar.massKg, opponentCar.maxSpeedKmh, lateralOffset = 3f),
        CarState("rival_3", opponentCar.massKg, opponentCar.maxSpeedKmh)
    )

    var snapshot: GameSnapshot = buildSnapshot(race.update(player, opponents, 0f))
        private set

    fun update(input: GameInput, dtSeconds: Float) {
        if (snapshot.race.finished) return
        val dt = dtSeconds.coerceIn(0f, 0.05f)

        collision.resolve(player, track.definition)
        physics.update(player, input, track, dt)
        collision.resolve(player, track.definition)

        opponents.forEachIndexed { index, car ->
            ai.update(car, track, dt, index * 1.7f + car.progress * 8f)
        }

        opponents.forEach { opponent ->
            if (collision.carsOverlap(player, opponent, track.lengthMeters())) {
                player.speedMetersPerSecond *= 0.82f
                opponent.speedMetersPerSecond *= 0.94f
            }
        }

        snapshot = buildSnapshot(race.update(player, opponents, dt))
    }

    fun reset() {
        player.speedMetersPerSecond = 0f
        player.progress = 0f
        player.lateralOffset = 0f
        player.yawDegrees = 0f
        player.lap = 0

        opponents.forEachIndexed { index, car ->
            car.speedMetersPerSecond = 0f
            car.progress = 0f
            car.lateralOffset = when (index) {
                0 -> -3f
                1 -> 3f
                else -> 0f
            }
            car.yawDegrees = 0f
            car.lap = 0
        }

        race.reset()
        snapshot = buildSnapshot(race.update(player, opponents, 0f))
    }

    private fun buildSnapshot(raceFrame: RaceFrame) = GameSnapshot(
        player = player.toFrame(),
        opponents = opponents.map { it.toFrame() },
        race = raceFrame
    )

    private fun CarState.toFrame() = CarFrame(
        id = id,
        speedKmh = speedMetersPerSecond * 3.6f,
        progress = progress,
        lateralOffset = lateralOffset,
        yawDegrees = yawDegrees,
        lap = lap
    )
}
