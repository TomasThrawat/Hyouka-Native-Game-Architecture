package com.tomasthrawat.gamearchitecture

import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            MaterialTheme {
                val game = remember { createGame(applicationContext) }
                val renderer = remember { DirectFilamentRenderer(applicationContext, game.track) }
                var input by remember { mutableStateOf(GameInput()) }
                var frame by remember { mutableStateOf(game.snapshot) }

                LaunchedEffect(renderer) {
                    renderer.loadModel("player", "models/starter_car.glb")
                    renderer.loadModel("rival_0", "models/rival_car.glb")
                    renderer.loadModel("rival_1", "models/rival_car.glb")
                    renderer.loadModel("rival_2", "models/rival_car.glb")

                    var last = 0L
                    while (isActive) {
                        val now = System.nanoTime()
                        val dt = if (last == 0L) 1f / 60f
                        else ((now - last) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                        last = now
                        game.update(input, dt)
                        val snapshot = game.snapshot
                        renderer.render(snapshot)
                        frame = snapshot
                        yield()
                    }
                }

                DisposableEffect(renderer) {
                    onDispose { renderer.destroy() }
                }

                Box(Modifier.fillMaxSize()) {
                    AndroidView(
                        factory = { renderer.surfaceView },
                        modifier = Modifier.fillMaxSize()
                    )
                    GameHud(
                        frame = frame,
                        onInput = { input = it },
                        onReset = {
                            game.reset()
                            val snapshot = game.snapshot
                            renderer.render(snapshot)
                            frame = snapshot
                        }
                    )
                }
            }
        }
    }

    private fun createGame(context: Context): Game {
        val glb = Glb(context)
        val cars = runCatching { glb.readCars() }.getOrDefault(emptyList())
        val tracks = runCatching { glb.readTracks() }.getOrDefault(emptyList())
        val fallback = Game()
        val player = cars.firstOrNull { it.id == "starter_car" } ?: fallback.playerCar
        val rival = cars.firstOrNull { it.id == "rival_car" } ?: fallback.opponentCar
        val track = tracks.firstOrNull() ?: fallback.track
        return Game(track = Track(track), playerCar = player, opponentCar = rival)
    }
}