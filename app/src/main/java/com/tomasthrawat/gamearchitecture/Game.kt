package com.tomasthrawat.gamearchitecture

class Game(
    private val physics: Physics,
    private val collision: Collision,
    private val ai: Ai,
    private val race: Race
) {
    val player = CarState()

    fun update(input: GameInput, dtSeconds: Float) {
        physics.update(player, input, dtSeconds)
        collision.resolve(player)
        ai.update(dtSeconds)
        race.update(player, dtSeconds)
    }
}
