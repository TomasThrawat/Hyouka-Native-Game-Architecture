package com.tomasthrawat.gamearchitecture

/**
 * Visual scene boundary.
 *
 * Camera, lights, nodes, models and environment belong here.
 * Do not put race rules or physics calculations in this class.
 */
interface Scene {
    fun updateFrom(game: Game)
}
