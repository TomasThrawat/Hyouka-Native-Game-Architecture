package com.tomasthrawat.gamearchitecture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import io.github.sceneview.SceneView
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Size
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.DynamicSkyNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.utils.colorOf

private const val ROAD_TILES = 20

@Composable
fun GameScene(
    modifier: Modifier,
    frame: GameSnapshot,
    track: Track,
    playerModelPath: String,
    rivalModelPath: String
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val context = LocalContext.current
    val assets = remember(context) { Glb(context) }

    val playerModel =
        if (assets.isGlb(playerModelPath)) {
            rememberModelInstance(modelLoader, playerModelPath)
        } else {
            null
        }

    val rivalOne =
        if (assets.isGlb(rivalModelPath)) {
            rememberModelInstance(modelLoader, rivalModelPath)
        } else {
            null
        }
    val rivalTwo =
        if (assets.isGlb(rivalModelPath)) {
            rememberModelInstance(modelLoader, rivalModelPath)
        } else {
            null
        }
    val rivalThree =
        if (assets.isGlb(rivalModelPath)) {
            rememberModelInstance(modelLoader, rivalModelPath)
        } else {
            null
        }

    val startPath = track.definition.startModel
    val startModel =
        if (assets.isGlb(startPath)) {
            rememberModelInstance(modelLoader, startPath)
        } else {
            null
        }

    val playerPose =
        track.pose(
            frame.player.progress,
            frame.player.lateralOffset
        )

    val headingRadians =
        Math.toRadians(playerPose.yawDegrees.toDouble())
    val forwardX =
        kotlin.math.sin(headingRadians).toFloat()
    val forwardZ =
        kotlin.math.cos(headingRadians).toFloat()

    val camera = rememberCameraNode(engine) {
        position = Position(
            x = playerPose.x - forwardX * 9f,
            y = 4.2f,
            z = playerPose.z - forwardZ * 9f
        )
    }

    camera.position = Position(
        x = playerPose.x - forwardX * 9f,
        y = 4.2f,
        z = playerPose.z - forwardZ * 9f
    )
    camera.lookAt(
        Position(
            playerPose.x,
            0.75f,
            playerPose.z
        )
    )

    val light = rememberMainLightNode(engine) {
        intensity = 110_000f
    }

    val groundMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            colorOf(Color(0.06f, 0.15f, 0.08f, 1f)),
            metallic = 0f,
            roughness = 0.96f
        )
    }
    val roadMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            colorOf(Color(0.055f, 0.06f, 0.07f, 1f)),
            metallic = 0f,
            roughness = 0.92f
        )
    }
    val kerbMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            colorOf(Color(0.72f, 0.05f, 0.04f, 1f)),
            metallic = 0f,
            roughness = 0.72f
        )
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
        DynamicSkyNode(
            timeOfDay = 15f,
            turbidity = 2.1f,
            sunIntensity = 110_000f
        )

        PlaneNode(
            size = Size(120f, 120f),
            materialInstance = groundMaterial,
            position = Position(y = -0.08f)
        )

        repeat(ROAD_TILES) { index ->
            val p = index.toFloat() / ROAD_TILES.toFloat()
            val pose = track.pose(p)

            CubeNode(
                size = Size(24.6f, 0.10f, 7.05f),
                materialInstance = roadMaterial,
                position = Position(pose.x, 0f, pose.z),
                rotation = Rotation(y = pose.yawDegrees)
            )

            val yaw = Math.toRadians(pose.yawDegrees.toDouble())
            val nx = -kotlin.math.cos(yaw).toFloat()
            val nz = kotlin.math.sin(yaw).toFloat()

            CubeNode(
                size = Size(0.75f, 0.11f, 7.05f),
                materialInstance = kerbMaterial,
                position = Position(
                    pose.x + nx * 12.1f,
                    0.02f,
                    pose.z + nz * 12.1f
                ),
                rotation = Rotation(y = pose.yawDegrees)
            )

            CubeNode(
                size = Size(0.75f, 0.11f, 7.05f),
                materialInstance = kerbMaterial,
                position = Position(
                    pose.x - nx * 12.1f,
                    0.02f,
                    pose.z - nz * 12.1f
                ),
                rotation = Rotation(y = pose.yawDegrees)
            )
        }

        startModel?.let { instance ->
            val pose = track.pose(0f)
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1f,
                position = Position(pose.x, 0f, pose.z),
                rotation = Rotation(y = pose.yawDegrees),
                autoAnimate = false
            )
        }

        playerModel?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 1f,
                position = Position(
                    playerPose.x,
                    0f,
                    playerPose.z
                ),
                rotation = Rotation(y = playerPose.yawDegrees),
                autoAnimate = false
            )
        }

        val rivals = listOf(
            rivalOne,
            rivalTwo,
            rivalThree
        )

        frame.opponents.forEachIndexed { index, opponent ->
            rivals[index]?.let { instance ->
                val pose = track.pose(
                    opponent.progress,
                    opponent.lateralOffset
                )
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1f,
                    position = Position(
                        pose.x,
                        0f,
                        pose.z
                    ),
                    rotation = Rotation(y = pose.yawDegrees),
                    autoAnimate = false
                )
            }
        }
    }
}
