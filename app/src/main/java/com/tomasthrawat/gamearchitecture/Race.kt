package com.tomasthrawat.gamearchitecture

class Race {
    private var elapsedSeconds = 0f

    fun update(player: CarState, dt: Float) {
        elapsedSeconds += dt
        // Add checkpoints, laps, ranking and finish conditions here.
    }
}
