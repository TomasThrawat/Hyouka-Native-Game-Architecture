package com.tomasthrawat.gamearchitecture

interface Sound {
    fun playEngine(speedMetersPerSecond: Float)
    fun playCollision()
    fun playUi()
    fun stopAll()
}
