package com.tomasthrawat.gamearchitecture

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val game = remember {
                createGame(applicationContext)
            }
            val renderer = remember {
                ComposeGameRenderer(game.snapshot)
            }
            var input by remember {
                mutableStateOf(GameInput())
            }

            LaunchedEffect(game, renderer) {
                var lastNanos = 0L

                while (isActive) {
                    withFrameNanos { now ->
                        val dt = if (lastNanos == 0L) {
                            1f / 60f
                        } else {
                            ((now - lastNanos) / 1_000_000_000f)
                                .coerceIn(0f, 0.05f)
                        }

                        lastNanos = now
                        game.update(input, dt)
                        renderer.render(game.snapshot)
                    }
                }
            }

            MaterialTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    GameScene(
                        modifier = Modifier.fillMaxSize(),
                        frame = renderer.frame,
                        track = game.track,
                        playerModelPath = game.playerCar.model,
                        rivalModelPath = game.opponentCar.model
                    )

                    GameHud(
                        frame = renderer.frame,
                        onInput = { input = it },
                        onReset = {
                            game.reset()
                            input = GameInput()
                            renderer.render(game.snapshot)
                        }
                    )
                }
            }
        }
    }

    private fun createGame(context: Context): Game {
        val glb = Glb(context)

        val cars = runCatching {
            glb.readCars()
        }.getOrDefault(emptyList())

        val tracks = runCatching {
            glb.readTracks()
        }.getOrDefault(emptyList())

        val fallback = Game()

        val player =
            cars.firstOrNull { it.id == "starter_car" }
                ?: fallback.playerCar
        val rival =
            cars.firstOrNull { it.id == "rival_car" }
                ?: fallback.opponentCar
        val trackDefinition =
            tracks.firstOrNull()
                ?: fallback.track.definition

        return Game(
            track = Track(trackDefinition),
            playerCar = player,
            opponentCar = rival
        )
    }
}
