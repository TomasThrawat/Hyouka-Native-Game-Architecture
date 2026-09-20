package com.tomasthrawat.gamearchitecture

class Race(private val totalLaps: Int = 3) {
    private var elapsedSeconds = 0f

    fun update(player: CarState, opponents: List<CarState>, dt: Float): RaceFrame {
        elapsedSeconds += dt
        val all = buildList {
            add(player)
            addAll(opponents)
        }
        val playerScore = player.lap + player.progress
        val position = 1 + all.count { it !== player && it.lap + it.progress > playerScore }
        return RaceFrame(
            lap = (player.lap + 1).coerceAtMost(totalLaps),
            totalLaps = totalLaps,
            position = position,
            finished = player.lap >= totalLaps,
            elapsedSeconds = elapsedSeconds
        )
    }

    fun reset() {
        elapsedSeconds = 0f
    }
}
