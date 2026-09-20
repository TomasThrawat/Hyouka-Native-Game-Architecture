package com.tomasthrawat.gamearchitecture

class Track(
    val definition: TrackDefinition
) {
    fun spawnPosition(): CarState = CarState()
}
