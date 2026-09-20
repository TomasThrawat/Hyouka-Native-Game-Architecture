package com.tomasthrawat.gamearchitecture

interface Sound {
    fun playEngine(speedMetersPerSecond: Float)
    fun playCollision()
    fun playUi()
    fun stopAll()
}

class SilentSound : Sound {
    override fun playEngine(speedMetersPerSecond: Float) = Unit
    override fun playCollision() = Unit
    override fun playUi() = Unit
    override fun stopAll() = Unit
}
