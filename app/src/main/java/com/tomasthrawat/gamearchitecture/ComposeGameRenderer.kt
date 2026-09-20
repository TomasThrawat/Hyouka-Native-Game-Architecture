package com.tomasthrawat.gamearchitecture

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ComposeGameRenderer(
    initialFrame: GameSnapshot
) : GameRenderer {
    var frame: GameSnapshot by mutableStateOf(initialFrame)
        private set

    override fun render(frame: GameSnapshot) {
        this.frame = frame
    }
}
