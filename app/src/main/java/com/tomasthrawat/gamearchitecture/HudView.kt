package com.tomasthrawat.gamearchitecture

/**
 * UI boundary.
 *
 * A Compose implementation can observe Game state and emit GameInput.
 * It should not mutate renderer nodes directly.
 */
interface HudView {
    fun submitInput(input: GameInput)
}
