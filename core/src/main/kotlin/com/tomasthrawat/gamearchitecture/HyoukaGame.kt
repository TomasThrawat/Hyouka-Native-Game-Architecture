package com.tomasthrawat.gamearchitecture

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class HyoukaGame : ApplicationAdapter() {
    private lateinit var game: Game
    private lateinit var modelBatch: ModelBatch
    private lateinit var hudBatch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var camera: PerspectiveCamera
    private lateinit var environment: Environment

    private lateinit var roadModel: Model
    private lateinit var groundModel: Model
    private lateinit var markerModel: Model
    private lateinit var playerBodyModel: Model
    private lateinit var rivalBodyModel: Model
    private lateinit var cabinModel: Model

    private val bodies = ArrayList<ModelInstance>(4)
    private val cabins = ArrayList<ModelInstance>(4)
    private val markers = ArrayList<ModelInstance>(32)

    private val hudProjection = Matrix4()

    override fun create() {
        game = Game()
        modelBatch = ModelBatch()
        hudBatch = SpriteBatch()
        font = BitmapFont().also { it.data.setScale(1.25f) }

        environment = Environment().apply {
            set(ColorAttribute.createAmbientLight(0.58f, 0.6f, 0.64f, 1f))
            add(DirectionalLight().set(0.95f, 0.9f, 0.82f, -0.55f, -1f, -0.35f))
        }

        camera = PerspectiveCamera(
            58f,
            max(Gdx.graphics.width, 1).toFloat(),
            max(Gdx.graphics.height, 1).toFloat()
        ).apply {
            near = 0.1f
            far = 220f
        }

        val builder = ModelBuilder()
        val attrs = Usage.Position.toLong() or Usage.Normal.toLong()

        groundModel = builder.createBox(
            110f, 0.2f, 110f,
            Material(ColorAttribute.createDiffuse(Color(0.035f, 0.07f, 0.045f, 1f))),
            attrs
        )
        roadModel = buildRoad(builder, game.track.definition, attrs)
        markerModel = builder.createBox(
            0.32f, 0.06f, 2.8f,
            Material(ColorAttribute.createDiffuse(Color(0.95f, 0.78f, 0.12f, 1f))),
            attrs
        )
        playerBodyModel = builder.createBox(
            2.15f, 0.72f, 4.1f,
            Material(ColorAttribute.createDiffuse(Color(0.14f, 0.66f, 1f, 1f))),
            attrs
        )
        rivalBodyModel = builder.createBox(
            2.15f, 0.72f, 4.1f,
            Material(ColorAttribute.createDiffuse(Color(0.85f, 0.16f, 0.13f, 1f))),
            attrs
        )
        cabinModel = builder.createBox(
            1.6f, 0.5f, 1.9f,
            Material(ColorAttribute.createDiffuse(Color(0.06f, 0.07f, 0.09f, 1f))),
            attrs
        )

        repeat(4) { index ->
            bodies += ModelInstance(if (index == 0) playerBodyModel else rivalBodyModel)
            cabins += ModelInstance(cabinModel)
        }

        repeat(32) { index ->
            val pose = game.track.pose(index / 32f, 0f)
            markers += ModelInstance(markerModel).also {
                it.transform
                    .setToTranslation(pose.x, 0.16f, pose.z)
                    .rotate(Vector3.Y, pose.yawDegrees)
            }
        }

        repositionCars()
        updateCamera()
    }

    override fun render() {
        game.update(readInput(), Gdx.graphics.deltaTime.coerceIn(0f, 0.05f))
        repositionCars()
        updateCamera()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        Gdx.gl.glClearColor(0.02f, 0.028f, 0.045f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.begin(camera)
        modelBatch.render(ModelInstance(groundModel), environment)
        modelBatch.render(ModelInstance(roadModel), environment)
        markers.forEach { modelBatch.render(it, environment) }
        bodies.indices.forEach {
            modelBatch.render(bodies[it], environment)
            modelBatch.render(cabins[it], environment)
        }
        modelBatch.end()

        drawHud()
    }

    private fun drawHud() {
        val width = max(Gdx.graphics.width, 1).toFloat()
        val height = max(Gdx.graphics.height, 1).toFloat()
        hudProjection.setToOrtho2D(0f, 0f, width, height)
        hudBatch.projectionMatrix = hudProjection

        val frame = game.snapshot
        hudBatch.begin()
        font.draw(hudBatch, "SPEED " + frame.player.speedKmh.toInt() + " KM/H", 24f, height - 24f)
        font.draw(hudBatch, "LAP " + frame.race.lap + "/" + frame.race.totalLaps + "   POS " + frame.race.position + "/4", 24f, height - 56f)
        font.draw(
            hudBatch,
            if (frame.race.finished) "FINISH" else "TOUCH LEFT / RIGHT = STEER   CENTER = GO",
            24f,
            34f
        )
        hudBatch.end()
    }

    private fun readInput(): GameInput {
        val width = max(Gdx.graphics.width, 1).toFloat()
        val height = max(Gdx.graphics.height, 1).toFloat()

        var steer = 0f
        var throttle = 0f
        var brake = 0f

        if (Gdx.input.isTouched) {
            val x = Gdx.input.getX().toFloat()
            val y = Gdx.input.getY().toFloat()
            when {
                x < width * 0.32f -> steer = -1f
                x > width * 0.68f -> steer = 1f
                y > height * 0.5f -> throttle = 1f
                else -> brake = 1f
            }
        }

        if (Gdx.input.isKeyPressed(Keys.LEFT)) steer = -1f
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) steer = 1f
        if (Gdx.input.isKeyPressed(Keys.UP)) throttle = 1f
        if (Gdx.input.isKeyPressed(Keys.DOWN)) brake = 1f
        if (Gdx.input.isKeyJustPressed(Keys.R)) game.reset()

        return GameInput(throttle = throttle, brake = brake, steer = steer)
    }

    private fun repositionCars() {
        val player = game.snapshot.player
        applyCar(bodies[0], cabins[0], game.track.pose(player.progress, player.lateralOffset))

        game.snapshot.opponents.forEachIndexed { index, frame ->
            applyCar(
                bodies[index + 1],
                cabins[index + 1],
                game.track.pose(frame.progress, frame.lateralOffset)
            )
        }
    }

    private fun applyCar(body: ModelInstance, cabin: ModelInstance, pose: TrackPose) {
        body.transform
            .setToTranslation(pose.x, 0.58f, pose.z)
            .rotate(Vector3.Y, pose.yawDegrees)
        cabin.transform
            .setToTranslation(pose.x, 1.02f, pose.z)
            .rotate(Vector3.Y, pose.yawDegrees)
    }

    private fun updateCamera() {
        val frame = game.snapshot.player
        val pose = game.track.pose(frame.progress, frame.lateralOffset)
        val yaw = Math.toRadians(pose.yawDegrees.toDouble())
        val fx = sin(yaw).toFloat()
        val fz = cos(yaw).toFloat()

        camera.position.set(
            pose.x - fx * 11f,
            7.2f,
            pose.z - fz * 11f
        )
        camera.lookAt(pose.x, 0.8f, pose.z)
        camera.up.set(Vector3.Y)
        camera.update()
    }

    private fun buildRoad(builder: ModelBuilder, def: TrackDefinition, attrs: Long): Model {
        val model = builder.begin()
        val material = Material(ColorAttribute.createDiffuse(Color(0.12f, 0.13f, 0.15f, 1f)))
        val part: MeshPartBuilder = builder.part("road", GL20.GL_TRIANGLES, attrs, material)

        val radiusX = def.halfDepthMeters.coerceAtLeast(18f)
        val radiusZ = max(def.halfWidthMeters * 3.2f, 16f)
        val width = def.halfWidthMeters
        val segments = 96
        val normal = Vector3(0f, 1f, 0f)
        val outer0 = Vector3()
        val inner0 = Vector3()
        val outer1 = Vector3()
        val inner1 = Vector3()

        for (i in 0 until segments) {
            val a0 = i * Math.PI * 2.0 / segments
            val a1 = (i + 1) * Math.PI * 2.0 / segments

            setEdge(outer0, radiusX, radiusZ, width, a0, true)
            setEdge(inner0, radiusX, radiusZ, width, a0, false)
            setEdge(outer1, radiusX, radiusZ, width, a1, true)
            setEdge(inner1, radiusX, radiusZ, width, a1, false)

            part.rect(outer0, outer1, inner1, inner0, normal)
        }

        return builder.end()
    }

    private fun setEdge(out: Vector3, rx: Float, rz: Float, width: Float, angle: Double, outer: Boolean) {
        val cx = cos(angle).toFloat() * rx
        val cz = sin(angle).toFloat() * rz
        val tx = (-sin(angle) * rx).toFloat()
        val tz = (cos(angle) * rz).toFloat()
        val len = sqrt(tx * tx + tz * tz)
        val nx = -tz / len
        val nz = tx / len
        val side = if (outer) 1f else -1f

        out.set(cx + nx * width * side, 0.08f, cz + nz * width * side)
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = max(width, 1).toFloat()
        camera.viewportHeight = max(height, 1).toFloat()
        camera.update()
    }

    override fun dispose() {
        modelBatch.dispose()
        hudBatch.dispose()
        font.dispose()
        roadModel.dispose()
        groundModel.dispose()
        markerModel.dispose()
        playerBodyModel.dispose()
        rivalBodyModel.dispose()
        cabinModel.dispose()
    }
}
