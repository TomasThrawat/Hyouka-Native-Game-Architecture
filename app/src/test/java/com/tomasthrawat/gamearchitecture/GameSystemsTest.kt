package com.tomasthrawat.gamearchitecture

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSystemsTest {
    @Test
    fun throttleAcceleratesPlayer() {
        val game = Game()
        val before = game.snapshot.player.speedKmh
        repeat(30) { game.update(GameInput(throttle = 1f), 1f / 60f) }
        assertTrue(game.snapshot.player.speedKmh > before)
    }

    @Test
    fun collisionClampsTrackLateralOffset() {
        val track = Track().definition
        val car = CarState("test", lateralOffset = 999f)
        Collision().resolve(car, track)
        assertEquals(track.halfWidthMeters - 1.2f, car.lateralOffset)
    }

    @Test
    fun raceReportsThreeLaps() {
        val game = Game()
        assertEquals(3, game.snapshot.race.totalLaps)
        assertEquals(1, game.snapshot.race.lap)
    }
}
