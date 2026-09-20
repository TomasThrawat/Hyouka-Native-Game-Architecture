package com.tomasthrawat.gamearchitecture

class Game(
    val track: Track = Track(),
    val playerCar: CarDefinition = CarDefinition(
        id = "starter_car",
        model = "models/starter_car.glb",
        massKg = 1200f,
        maxSpeedKmh = 310f
    ),
    val opponentCar: CarDefinition = CarDefinition(
        id = "rival_car",
        model = "models/rival_car.glb",
        massKg = 1250f,
        maxSpeedKmh = 295f
    ),
    private val totalLaps: Int = 3,
    private val sound: Sound = SilentSound()
) {
    private val physics = Physics()
    private val collision = Collision()
    private val ai = Ai(physics, collision)
    private val race = Race(totalLaps)

    private val player = CarState(
        id = "player",
        massKg = playerCar.massKg,
        maxSpeedKmh = playerCar.maxSpeedKmh
    )

    private val opponents = listOf(
        CarState(
            id = "rival_1",
            massKg = opponentCar.massKg,
            maxSpeedKmh = opponentCar.maxSpeedKmh,
            lateralOffset = -3f
        ),
        CarState(
            id = "rival_2",
            massKg = opponentCar.massKg,
            maxSpeedKmh = opponentCar.maxSpeedKmh,
            lateralOffset = 3f
        ),
        CarState(
            id = "rival_3",
            massKg = opponentCar.massKg,
            maxSpeedKmh = opponentCar.maxSpeedKmh
        )
    )

    private var collisionCooldown = 0f

    var snapshot: GameSnapshot =
        buildSnapshot(race.update(player, opponents, 0f))
        private set

    fun update(input: GameInput, dtSeconds: Float) {
        if (snapshot.race.finished) return

        val dt = dtSeconds.coerceIn(0f, 0.05f)
        collisionCooldown =
            (collisionCooldown - dt).coerceAtLeast(0f)

        var hit = collision.resolve(
            player,
            track.definition
        )
        physics.update(
            player,
            input,
            track.definition,
            dt
        )
        hit =
            collision.resolve(player, track.definition) || hit

        opponents.forEachIndexed { index, car ->
            ai.update(
                car = car,
                track = track.definition,
                dt = dt,
                phase = index * 1.7f + car.progress * 8f
            )
        }

        opponents.forEach { opponent ->
            if (collision.carsOverlap(
                    player,
                    opponent,
                    track.definition.lengthMeters
                )
            ) {
                player.speedMetersPerSecond *= 0.82f
                opponent.speedMetersPerSecond *= 0.94f
                hit = true
            }
        }

        if (hit && collisionCooldown <= 0f) {
            sound.playCollision()
            collisionCooldown = 0.16f
        }

        sound.playEngine(player.speedMetersPerSecond)
        snapshot =
            buildSnapshot(race.update(player, opponents, dt))
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

        collisionCooldown = 0f
        race.reset()
        sound.stopAll()
        snapshot =
            buildSnapshot(race.update(player, opponents, 0f))
    }

    private fun buildSnapshot(
        raceFrame: RaceFrame
    ) = GameSnapshot(
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
