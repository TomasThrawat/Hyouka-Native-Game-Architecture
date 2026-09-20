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

private const val TRACK_TILES = 32
private const val ROAD_WIDTH = 8.6f
private const val TILE_LENGTH = 4.7f
private const val KERB_OFFSET = ROAD_WIDTH * 0.5f + 0.35f

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

    val playerModel = if (assets.isGlb(playerModelPath)) {
        rememberModelInstance(modelLoader, playerModelPath)
    } else null
    val rivalModel = if (assets.isGlb(rivalModelPath)) {
        rememberModelInstance(modelLoader, rivalModelPath)
    } else null
    val startModel = if (assets.isGlb(track.definition.startModel)) {
        rememberModelInstance(modelLoader, track.definition.startModel)
    } else null

    val playerPose = track.pose(frame.player.progress, frame.player.lateralOffset)
    val heading = Math.toRadians(playerPose.yawDegrees.toDouble())
    val forwardX = kotlin.math.sin(heading).toFloat()
    val forwardZ = kotlin.math.cos(heading).toFloat()

    val camera = rememberCameraNode(engine) {
        position = Position(
            x = playerPose.x - forwardX * 5.8f,
            y = 2.25f,
            z = playerPose.z - forwardZ * 5.8f
        )
    }
    camera.position = Position(
        x = playerPose.x - forwardX * 5.8f,
        y = 2.25f,
        z = playerPose.z - forwardZ * 5.8f
    )
    camera.lookAt(
        Position(
            x = playerPose.x + forwardX * 2.0f,
            y = 0.85f,
            z = playerPose.z + forwardZ * 2.0f
        )
    )

    val light = rememberMainLightNode(engine) { intensity = 110_000f }

    val groundMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            Color(0.035f, 0.12f, 0.055f, 1f),
            metallic = 0f,
            roughness = 0.98f
        )
    }
    val roadMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            Color(0.045f, 0.052f, 0.06f, 1f),
            metallic = 0f,
            roughness = 0.92f
        )
    }
    val redKerbMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            Color(0.82f, 0.045f, 0.035f, 1f),
            metallic = 0f,
            roughness = 0.7f
        )
    }
    val whiteKerbMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            Color(0.92f, 0.92f, 0.92f, 1f),
            metallic = 0f,
            roughness = 0.72f
        )
    }
    val laneMaterial = remember(materialLoader) {
        materialLoader.createColorInstance(
            Color(0.95f, 0.95f, 0.9f, 1f),
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
            turbidity = 2.0f,
            sunIntensity = 110_000f
        )
        PlaneNode(
            size = Size(180f, 180f),
            materialInstance = groundMaterial,
            position = Position(y = -0.08f)
        )

        repeat(TRACK_TILES) { index ->
            val p = (index.toFloat() + 0.5f) / TRACK_TILES.toFloat()
            val pose = track.pose(p)
            CubeNode(
                size = Size(ROAD_WIDTH, 0.10f, TILE_LENGTH),
                materialInstance = roadMaterial,
                position = Position(pose.x, 0f, pose.z),
                rotation = Rotation(y = pose.yawDegrees)
            )

            val yaw = Math.toRadians(pose.yawDegrees.toDouble())
            val sideX = -kotlin.math.cos(yaw).toFloat()
            val sideZ = kotlin.math.sin(yaw).toFloat()
            val kerbMaterial = if (index % 2 == 0) redKerbMaterial else whiteKerbMaterial

            CubeNode(
                size = Size(0.55f, 0.12f, TILE_LENGTH),
                materialInstance = kerbMaterial,
                position = Position(
                    pose.x + sideX * KERB_OFFSET,
                    0.03f,
                    pose.z + sideZ * KERB_OFFSET
                ),
                rotation = Rotation(y = pose.yawDegrees)
            )
            CubeNode(
                size = Size(0.55f, 0.12f, TILE_LENGTH),
                materialInstance = kerbMaterial,
                position = Position(
                    pose.x - sideX * KERB_OFFSET,
                    0.03f,
                    pose.z - sideZ * KERB_OFFSET
                ),
                rotation = Rotation(y = pose.yawDegrees)
            )

            if (index % 2 == 0) {
                CubeNode(
                    size = Size(0.12f, 0.025f, TILE_LENGTH * 0.55f),
                    materialInstance = laneMaterial,
                    position = Position(pose.x, 0.065f, pose.z),
                    rotation = Rotation(y = pose.yawDegrees)
                )
            }
        }

        startModel?.let { instance ->
            val pose = track.pose(0f)
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 5.5f,
                centerOrigin = Position(y = -1f),
                position = Position(pose.x, 0f, pose.z),
                rotation = Rotation(y = pose.yawDegrees),
                autoAnimate = false
            )
        }

        playerModel?.let { instance ->
            ModelNode(
                modelInstance = instance,
                scaleToUnits = 2.2f,
                centerOrigin = Position(y = -1f),
                position = Position(playerPose.x, 0f, playerPose.z),
                rotation = Rotation(y = playerPose.yawDegrees),
                autoAnimate = false
            )
        }

        frame.opponents.forEach { opponent ->
            rivalModel?.let { instance ->
                val pose = track.pose(opponent.progress, opponent.lateralOffset)
                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 2.0f,
                    centerOrigin = Position(y = -1f),
                    position = Position(pose.x, 0f, pose.z),
                    rotation = Rotation(y = pose.yawDegrees),
                    autoAnimate = false
                )
            }
        }
    }
}
