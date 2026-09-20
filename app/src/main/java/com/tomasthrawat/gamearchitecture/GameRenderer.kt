package com.tomasthrawat.gamearchitecture

/**
 * Rendering boundary.
 *
 * Keep the renderer dependent on game state, not the reverse.
 * A concrete OpenGL ES or 3D-engine implementation can be placed here.
 */
interface GameRenderer {
    fun render(game: Game, deltaSeconds: Float)
}
