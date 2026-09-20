package com.tomasthrawat.gamearchitecture

class Game(
    private val track: Track = Track(),
    private val totalLaps: Int = 3
) {
    private val physics = Physics()
    private val collision = Collision()
    private val ai = Ai(physics, collision)
    private val race = Race(totalLaps)

    private val player = CarState("player", progress = 0.02f)
    private val opponents = listOf(
        CarState("rival_1", progress = 0.00f, lateralOffset = -3f),
        CarState("rival_2", progress = 0.985f, lateralOffset = 3f),
        CarState("rival_3", progress = 0.97f)
    )

    var snapshot: GameSnapshot = buildSnapshot(race.update(player, opponents, 0f))
        private set

    fun update(input: GameInput, dtSeconds: Float) {
        val dt = dtSeconds.coerceIn(0f, 0.05f)
        physics.update(player, input, track.definition, dt)
        collision.resolve(player, track.definition)

        opponents.forEachIndexed { index, car ->
            ai.update(car, track.definition, dt, index * 1.7f + car.progress * 8f)
        }

        opponents.forEach { opponent ->
            if (collision.carsOverlap(player, opponent)) {
                player.speedMetersPerSecond *= 0.82f
                opponent.speedMetersPerSecond *= 0.94f
            }
        }

        snapshot = buildSnapshot(race.update(player, opponents, dt))
    }

    fun reset() {
        player.speedMetersPerSecond = 0f
        player.progress = 0.02f
        player.lateralOffset = 0f
        player.yawDegrees = 0f
        player.lap = 0
        opponents.forEachIndexed { index, car ->
            car.speedMetersPerSecond = 0f
            car.progress = when (index) {
                0 -> 0f
                1 -> 0.985f
                else -> 0.97f
            }
            car.lateralOffset = if (index == 0) -3f else if (index == 1) 3f else 0f
            car.yawDegrees = 0f
            car.lap = if (car.progress > 0.9f) -1 else 0
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
