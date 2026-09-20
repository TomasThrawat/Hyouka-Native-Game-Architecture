package com.tomasthrawat.gamearchitecture

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun GameHud(
    frame: GameSnapshot,
    onInput: (GameInput) -> Unit,
    onReset: () -> Unit
) {
    var throttlePressed by remember { mutableStateOf(false) }
    var brakePressed by remember { mutableStateOf(false) }
    var steerLeftPressed by remember { mutableStateOf(false) }
    var steerRightPressed by remember { mutableStateOf(false) }

    fun emitInput() {
        val steer = when {
            steerLeftPressed && !steerRightPressed -> -1f
            steerRightPressed && !steerLeftPressed -> 1f
            else -> 0f
        }

        onInput(
            GameInput(
                throttle = if (throttlePressed) 1f else 0f,
                brake = if (brakePressed) 1f else 0f,
                steer = steer
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp)
    ) {
        Surface(
            modifier = Modifier.align(Alignment.TopCenter),
            color = Color.Black.copy(alpha = 0.64f),
            shape = RoundedCornerShape(18.dp)
        ) {
            Text(
                modifier = Modifier.padding(
                    horizontal = 18.dp,
                    vertical = 10.dp
                ),
                text = "LAP " + frame.race.lap +
                    "/" + frame.race.totalLaps +
                    "   POS " + frame.race.position +
                    "   " + frame.player.speedKmh.toInt() +
                    " KM/H",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(top = 52.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            HoldButton("BRAKE") { pressed ->
                brakePressed = pressed
                emitInput()
            }
            HoldButton("LEFT") { pressed ->
                steerLeftPressed = pressed
                emitInput()
            }
            HoldButton("ACCEL") { pressed ->
                throttlePressed = pressed
                emitInput()
            }
            HoldButton("RIGHT") { pressed ->
                steerRightPressed = pressed
                emitInput()
            }
            Button(
                onClick = {
                    throttlePressed = false
                    brakePressed = false
                    steerLeftPressed = false
                    steerRightPressed = false
                    onInput(GameInput())
                    onReset()
                },
                colors = ButtonDefaults.buttonColors()
            ) {
                Text("RESET")
            }
        }
    }
}

@Composable
private fun HoldButton(
    label: String,
    onStateChanged: (Boolean) -> Unit
) {
    Button(
        onClick = {},
        modifier = Modifier.pointerInput(label) {
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
