package com.tomasthrawat.gamearchitecture

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val game = remember { Game() }
            var frame by remember { mutableStateOf(game.snapshot) }
            var input by remember { mutableStateOf(GameInput()) }

            LaunchedEffect(game) {
                var lastNanos = 0L
                while (isActive) {
                    withFrameNanos { now ->
                        val dt = if (lastNanos == 0L) {
                            1f / 60f
                        } else {
                            ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                        }
                        lastNanos = now
                        game.update(input, dt)
                        frame = game.snapshot
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                GameScene(Modifier.fillMaxSize(), frame)
                GameHud(
                    frame = frame,
                    onInput = { input = it },
                    onReset = {
                        game.reset()
                        input = GameInput()
                        frame = game.snapshot
                    }
                )
            }
        }
    }
}
