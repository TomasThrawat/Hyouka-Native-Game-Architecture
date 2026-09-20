package com.tomasthrawat.gamearchitecture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.utils.colorOf

@Composable
fun GameScene(
    modifier: Modifier,
    frame: GameSnapshot,
    track: Track = Track()
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val camera = rememberCameraNode(engine) {
        position = Position(x = 0f, y = 22f, z = 36f)
        lookAt(Position(y = 0f))
    }
    val light = rememberMainLightNode(engine) { intensity = 110_000f }

    val grass = remember(materialLoader) {
        materialLoader.createColorInstance(
            colorOf(Color(0.08f, 0.16f, 0.09f, 1f)),
            metallic = 0f,
            roughness = 0.95f
        )
    }
    val road = remember(materialLoader) {
        materialLoader.createColorInstance(
            colorOf(Color(0.08f, 0.09f, 0.10f, 1f)),
            metallic = 0f,
            roughness = 0.9f
        )
    }
    val playerMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color(0.85f, 0.1f, 0.08f, 1f)))
    }
    val aiMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(colorOf(Color(0.08f, 0.35f, 0.9f, 1f)))
    }

    SceneView(
        modifier = modifier,
        engine = engine,
        modelLoader = modelLoader,
        materialLoader = materialLoader,
        cameraNode = camera,
        mainLightNode = light,
        autoCenterContent = false,
        autoFitContent = false,
        cameraManipulator = null
    ) {
        DynamicSkyNode(timeOfDay = 15f, turbidity = 2f, sunIntensity = 110_000f)

        PlaneNode(
            size = Size(100f, 100f),
            materialInstance = grass,
            position = Position(y = -0.1f)
        )

        CubeNode(
            size = Size(track.definition.halfDepthMeters * 2f + 4f, 0.08f, 8f),
            materialInstance = road,
            position = Position(z = track.definition.halfWidthMeters)
        )
        CubeNode(
            size = Size(track.definition.halfDepthMeters * 2f + 4f, 0.08f, 8f),
            materialInstance = road,
            position = Position(z = -track.definition.halfWidthMeters)
        )
        CubeNode(
            size = Size(8f, 0.08f, track.definition.halfWidthMeters * 2f),
            materialInstance = road,
            position = Position(x = track.definition.halfDepthMeters)
        )
        CubeNode(
            size = Size(8f, 0.08f, track.definition.halfWidthMeters * 2f),
            materialInstance = road,
            position = Position(x = -track.definition.halfDepthMeters)
        )

        val playerPose = track.pose(frame.player.progress, frame.player.lateralOffset)
        CubeNode(
            size = Size(1.7f, 0.8f, 3.2f),
            materialInstance = playerMaterial,
            position = Position(playerPose.x, 0.45f, playerPose.z),
            rotation = Rotation(y = playerPose.yawDegrees)
        )

        frame.opponents.forEachIndexed { index, opponent ->
            val pose = track.pose(opponent.progress, opponent.lateralOffset)
            CubeNode(
                size = Size(1.6f, 0.75f, 3f),
                materialInstance = aiMaterial,
                position = Position(pose.x, 0.42f + index * 0.02f, pose.z),
                rotation = Rotation(y = pose.yawDegrees)
            )
        }
    }
}
