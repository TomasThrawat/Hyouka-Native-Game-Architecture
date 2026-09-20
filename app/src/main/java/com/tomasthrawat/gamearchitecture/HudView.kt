package com.tomasthrawat.gamearchitecture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

interface HudController {
    fun submitInput(input: GameInput)
}

@Composable
fun GameHud(
    frame: GameSnapshot,
    onInput: (GameInput) -> Unit,
    onReset: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        color = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LAP " + frame.race.lap + "/" + frame.race.totalLaps +
                    "   POS " + frame.race.position +
                    "   " + frame.player.speedKmh.toInt() + " KM/H"
            )
            HoldButton("BRAKE") { pressed ->
                onInput(GameInput(brake = if (pressed) 1f else 0f))
            }
            HoldButton("LEFT") { pressed ->
                onInput(GameInput(steer = if (pressed) -1f else 0f))
            }
            HoldButton("ACCEL") { pressed ->
                onInput(GameInput(throttle = if (pressed) 1f else 0f))
            }
            HoldButton("RIGHT") { pressed ->
                onInput(GameInput(steer = if (pressed) 1f else 0f))
            }
            Button(onClick = onReset, colors = ButtonDefaults.buttonColors()) {
                Text("RESET")
            }
        }
    }
}

@Composable
private fun HoldButton(label: String, onStateChanged: (Boolean) -> Unit) {
    Button(
        onClick = {},
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    onStateChanged(true)
                    try {
                        awaitRelease()
                    } finally {
                        onStateChanged(false)
                    }
                }
            )
        }
    ) {
        Text(label)
    }
}
