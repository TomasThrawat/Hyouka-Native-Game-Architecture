package com.tomasthrawat.gamearchitecture

import android.content.Context

class Glb(private val context: Context) {
    fun assetPath(path: String): String {
        require(path.endsWith(".glb")) { "Expected a GLB asset: $path" }
        return path
    }

    fun exists(path: String): Boolean =
        runCatching { context.assets.open(path).close(); true }.getOrDefault(false)
}
