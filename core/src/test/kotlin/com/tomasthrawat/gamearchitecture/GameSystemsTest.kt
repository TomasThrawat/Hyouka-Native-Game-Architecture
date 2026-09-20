package com.tomasthrawat.gamearchitecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSystemsTest {
    @Test
    fun throttleAcceleratesPlayer() {
        val game = Game()
        val before = game.snapshot.player.speedKmh
        repeat(60) { game.update(GameInput(throttle = 1f), 1f / 60f) }
        assertTrue(game.snapshot.player.speedKmh > before)
        assertTrue(game.snapshot.player.speedKmh <= game.playerCar.maxSpeedKmh)
    }

    @Test
    fun steeringClampsToTrackBounds() {
        val game = Game()
        repeat(120) { game.update(GameInput(throttle = 1f, steer = 1f), 1f / 60f) }
        assertTrue(game.snapshot.player.lateralOffset <= game.track.definition.halfWidthMeters - 1.2f + 0.0001f)
    }

    @Test
    fun collisionClampsTrackLateralOffset() {
        val track = Track().definition
        val car = CarState("test", lateralOffset = 999f)
        Collision().resolve(car, track)
        assertEquals(track.halfWidthMeters - 1.2f, car.lateralOffset, 0.0001f)
    }

    @Test
    fun raceStartsAtLapOne() {
        val game = Game()
        assertEquals(3, game.snapshot.race.totalLaps)
        assertEquals(1, game.snapshot.race.lap)
        assertEquals(1, game.snapshot.race.position)
    }

    @Test
    fun trackPoseWrapsProgress() {
        val track = Track()
        val a = track.pose(0f)
        val b = track.pose(1f)
        assertEquals(a.x, b.x, 0.0001f)
        assertEquals(a.z, b.z, 0.0001f)
        assertEquals(a.yawDegrees, b.yawDegrees, 0.0001f)
    }

    @Test
    fun trackLengthMatchesRenderedPathScale() {
        val track = Track()
        assertTrue(track.lengthMeters() > 300f)
        assertTrue(track.lengthMeters() < 500f)
        assertTrue(track.sampledCenterline(8).size >= 80)
    }

    @Test
    fun carsOverlapUsesTrackDistance() {
        val a = CarState("a")
        val b = CarState("b", progress = 0.01f)
        assertTrue(Collision().carsOverlap(a, b, 140f))
    }
}
