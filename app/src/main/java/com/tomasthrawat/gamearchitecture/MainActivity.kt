package com.tomasthrawat.gamearchitecture

import android.app.Activity
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Attach the concrete game renderer/scene/UI here.
        // The architecture deliberately keeps those concerns separate.
    }
}
