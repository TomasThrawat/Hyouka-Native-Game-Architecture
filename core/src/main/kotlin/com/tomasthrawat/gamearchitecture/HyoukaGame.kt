package com.tomasthrawat.gamearchitecture

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx
import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

class HyoukaGame : ApplicationAdapter() {
    private lateinit var game: Game
    private lateinit var manager: SceneManager
    private lateinit var camera: PerspectiveCamera
    private lateinit var batch: SpriteBatch
    private lateinit var font: BitmapFont
    private lateinit var shapes: ShapeRenderer
    private val assets = mutableListOf<SceneAsset>()
    private val fallbackModels = mutableListOf<Model>()
    private val cars = mutableListOf<Scene>()
    private val roads = mutableListOf<Scene>()
    private lateinit var roadModel: Model
    private lateinit var roadStripeModel: Model
    private lateinit var roadCurbModel: Model
    private val roadInstances = mutableListOf<ModelInstance>()
    private val roadStripeInstances = mutableListOf<ModelInstance>()
    private val roadCurbInstances = mutableListOf<ModelInstance>()
    private var trackIndex = 0

    override fun create() {
        game = Game()

        val cfg = PBRShaderProvider.createDefaultConfig()
        cfg.numDirectionalLights = 2
        cfg.numPointLights = 0
        cfg.numSpotLights = 0
        cfg.numBones = 0

        manager = SceneManager(
            PBRShaderProvider.createDefault(cfg),
            PBRShaderProvider.createDefaultDepth(
                PBRShaderProvider.createDefaultDepthConfig()
            )
        )

        manager.setAmbientLight(0.55f)
        manager.environment.add(
            DirectionalLightEx().set(
                Color.WHITE,
                Vector3(-1f, -3f, -2f),
                4f
            )
        )
        manager.environment.add(
            DirectionalLightEx().set(
                Color.WHITE,
                Vector3(1f, -1f, 1f),
                1.25f
            )
        )

        camera = PerspectiveCamera(
            62f,
            max(Gdx.graphics.width, 1).toFloat(),
            max(Gdx.graphics.height, 1).toFloat()
        )
        camera.near = 0.1f
        camera.far = 420f

        batch = SpriteBatch()
        font = BitmapFont()
        shapes = ShapeRenderer()
        buildProceduralRoadModels()

        loadAssets()
        selectTrack(0)
    }

    private fun loadAssets() {
        val names = listOf(
            "player",
            "rival1",
            "rival2"
        )

        val loaded = mutableMapOf<String, SceneAsset>()
        val loader = GLBLoader()

        for (name in names) {
            val path = "models/racing/" + name + ".glb"
            try {
                val file = Gdx.files.internal(path)
                check(file.exists()) { "Missing asset: " + path }

                val asset = loader.load(file)
                loaded[name] = asset
                assets += asset
                Gdx.app.log("HyoukaGame", "GLB loaded: " + path)
            } catch (e: Exception) {
                Gdx.app.error(
                    "HyoukaGame",
                    "GLB load failed, using fallback: " + path,
                    e
                )
            }
        }

        fun fallback(name: String): Scene {
            val model = ModelBuilder().createBox(
                4f,
                1f,
                10f,
                Material(ColorAttribute.createDiffuse(Color.GRAY)),
                VertexAttributes.Usage.Position.toLong() or
                    VertexAttributes.Usage.Normal.toLong()
            )
            fallbackModels += model
            Gdx.app.log("HyoukaGame", "Fallback model created: " + name)
            return Scene(ModelInstance(model))
        }

        fun add(name: String): Scene {
            val scene = loaded[name]?.let { Scene(it.scene) } ?: fallback(name)
            manager.addScene(scene)
            return scene
        }

        repeat(4) {
            cars += add(
                when {
                    it == 0 -> "player"
                    it % 2 == 0 -> "rival2"
                    else -> "rival1"
                }
            )
        }

        repeat(18) { roads += add("straight") }
        repeat(8) { roads += add("corner90") }
        repeat(3) { roads += add("hairpin") }
        repeat(6) { roads += add("tyre_wall") }
        repeat(3) { roads += add("grandstand") }
        repeat(1) { roads += add("start_gantry") }

        Gdx.app.log(
            "HyoukaGame",
            "Assets ready: " + loaded.size + "/" + names.size + " GLB files"
        )
    }

    private fun selectTrack(i: Int) {
        trackIndex = i.coerceIn(0, 2)
        game.track.selectLayout(trackIndex)
        game.reset()
        layoutRoad()
    }

    private fun buildProceduralRoadModels() {
        val roadMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.16f, 0.17f, 0.18f, 1f))
        )
        val stripeMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.92f, 0.92f, 0.88f, 1f))
        )
        val curbMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.88f, 0.10f, 0.08f, 1f))
        )

        val points = game.track.waypoints()
        roadModel = buildTrackStrip(points, 5.2f, 0.0f, roadMaterial, "road")
        roadStripeModel = buildTrackStrip(points, 0.10f, 0.065f, stripeMaterial, "center")
        roadCurbModel = buildTrackCurbs(points, 5.05f, 0.075f, curbMaterial)

        manager.getRenderableProviders().add(ModelInstance(roadModel))
        manager.getRenderableProviders().add(ModelInstance(roadStripeModel))
        manager.getRenderableProviders().add(ModelInstance(roadCurbModel))
    }

    private fun buildTrackStrip(
        points: List<Pair<Float, Float>>,
        halfWidth: Float,
        y: Float,
        material: Material,
        id: String
    ): Model {
        val builder = ModelBuilder()
        builder.begin()
        val attributes = VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong()
        val part = builder.part(id, GL20.GL_TRIANGLES, attributes, material)

        fun pointInfo(x: Float, z: Float) =
            MeshPartBuilder.VertexInfo().setPos(x, y, z).setNor(0f, 1f, 0f)

        for (i in points.indices) {
            val prev = points[(i - 1 + points.size) % points.size]
            val cur = points[i]
            val next = points[(i + 1) % points.size]

            val prevDx = cur.first - prev.first
            val prevDz = cur.second - prev.second
            val nextDx = next.first - cur.first
            val nextDz = next.second - cur.second

            val prevLen = sqrt(prevDx * prevDx + prevDz * prevDz).coerceAtLeast(0.001f)
            val nextLen = sqrt(nextDx * nextDx + nextDz * nextDz).coerceAtLeast(0.001f)

            val tx = prevDx / prevLen + nextDx / nextLen
            val tz = prevDz / prevLen + nextDz / nextLen
            val tLen = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            val nx = -tz / tLen
            val nz = tx / tLen

            val leftX = cur.first + nx * halfWidth
            val leftZ = cur.second + nz * halfWidth
            val rightX = cur.first - nx * halfWidth
            val rightZ = cur.second - nz * halfWidth

            val nextPrev = next
            val nextNext = points[(i + 2) % points.size]
            val ndx1 = nextPrev.first - cur.first
            val ndz1 = nextPrev.second - cur.second
            val ndx2 = nextNext.first - nextPrev.first
            val ndz2 = nextNext.second - nextPrev.second
            val l1 = sqrt(ndx1 * ndx1 + ndz1 * ndz1).coerceAtLeast(0.001f)
            val l2 = sqrt(ndx2 * ndx2 + ndz2 * ndz2).coerceAtLeast(0.001f)
            val ntx = ndx1 / l1 + ndx2 / l2
            val ntz = ndz1 / l1 + ndz2 / l2
            val ntLen = sqrt(ntx * ntx + ntz * ntz).coerceAtLeast(0.001f)
            val nnx = -ntz / ntLen
            val nnz = ntx / ntLen

            val nextLeftX = nextPrev.first + nnx * halfWidth
            val nextLeftZ = nextPrev.second + nnz * halfWidth
            val nextRightX = nextPrev.first - nnx * halfWidth
            val nextRightZ = nextPrev.second - nnz * halfWidth

            part.rect(
                pointInfo(rightX, rightZ),
                pointInfo(leftX, leftZ),
                pointInfo(nextLeftX, nextLeftZ),
                pointInfo(nextRightX, nextRightZ)
            )
        }

        return builder.end()
    }

    private fun buildTrackCurbs(
        points: List<Pair<Float, Float>>,
        halfWidth: Float,
        y: Float,
        material: Material
    ): Model {
        val builder = ModelBuilder()
        builder.begin()
        val attributes = VertexAttributes.Usage.Position.toLong() or VertexAttributes.Usage.Normal.toLong()
        val part = builder.part("curbs", GL20.GL_TRIANGLES, attributes, material)

        fun info(x: Float, z: Float) =
            MeshPartBuilder.VertexInfo().setPos(x, y, z).setNor(0f, 1f, 0f)

        val curbWidth = 0.22f

        for (i in points.indices) {
            val cur = points[i]
            val next = points[(i + 1) % points.size]
            val dx = next.first - cur.first
            val dz = next.second - cur.second
            val len = sqrt(dx * dx + dz * dz).coerceAtLeast(0.001f)
            val nx = -dz / len
            val nz = dx / len

            val lx1 = cur.first + nx * halfWidth
            val lz1 = cur.second + nz * halfWidth
            val lx2 = next.first + nx * halfWidth
            val lz2 = next.second + nz * halfWidth
            val rx1 = cur.first - nx * halfWidth
            val rz1 = cur.second - nz * halfWidth
            val rx2 = next.first - nx * halfWidth
            val rz2 = next.second - nz * halfWidth

            part.rect(
                info(lx1 - nx * curbWidth, lz1 - nz * curbWidth),
                info(lx1 + nx * curbWidth, lz1 + nz * curbWidth),
                info(lx2 + nx * curbWidth, lz2 + nz * curbWidth),
                info(lx2 - nx * curbWidth, lz2 - nz * curbWidth)
            )
            part.rect(
                info(rx1 + nx * curbWidth, rz1 + nz * curbWidth),
                info(rx1 - nx * curbWidth, rz1 - nz * curbWidth),
                info(rx2 - nx * curbWidth, rz2 - nz * curbWidth),
                info(rx2 + nx * curbWidth, rz2 + nz * curbWidth)
            )
        }

        return builder.end()
    }

    private fun layoutRoad() {
        Gdx.app.log("HyoukaGame", "Continuous track mesh ready")
    }

    override fun render() {
        if (Gdx.input.justTouched()) {
            val x = Gdx.input.x.toFloat()
            val w = Gdx.graphics.width.toFloat()
            if (Gdx.graphics.height - Gdx.input.y < 95f && x > w - 330f) {
                selectTrack(((x - (w - 330f)) / 110f).toInt())
            }
        }

        game.update(input(), Gdx.graphics.deltaTime.coerceIn(0f, 0.05f))
        updateCars()
        updateCamera()

        Gdx.gl.glClearColor(0.015f, 0.02f, 0.028f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        manager.camera = camera
        manager.update(Gdx.graphics.deltaTime)
        manager.render()
        hud()
    }

    private fun input(): GameInput {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        var s = 0f
        var t = 0f
        var b = 0f

        if (Gdx.input.isTouched) {
            val x = Gdx.input.x.toFloat()
            val y = h - Gdx.input.y

            if (y < h * 0.4f) {
                if (x < w * 0.3f) s = -1f
                if (x > w * 0.7f) s = 1f
            }

            if (x > w * 0.36f && x < w * 0.64f) {
                if (y > h * 0.52f) t = 1f else b = 1f
            }
        }

        if (Gdx.input.isKeyPressed(Keys.LEFT)) s = -1f
        if (Gdx.input.isKeyPressed(Keys.RIGHT)) s = 1f
        if (Gdx.input.isKeyPressed(Keys.UP)) t = 1f
        if (Gdx.input.isKeyPressed(Keys.DOWN)) b = 1f
        if (Gdx.input.isKeyJustPressed(Keys.R)) game.reset()

        return GameInput(t, b, s)
    }

    private fun updateCars() {
        val frames = listOf(game.snapshot.player) + game.snapshot.opponents
        cars.forEachIndexed { i, car ->
            val p = game.track.pose(
                frames[i].progress,
                frames[i].lateralOffset
            )
            car.modelInstance.transform
                .setToTranslation(p.x, 0.2f, p.z)
                .rotate(Vector3.Y, p.yawDegrees)
                .scale(1.05f, 1.05f, 1.05f)
        }
    }

    private fun updateCamera() {
        val p = game.track.pose(
            game.snapshot.player.progress,
            game.snapshot.player.lateralOffset
        )
        val r = Math.toRadians(p.yawDegrees.toDouble())
        val fx = sin(r).toFloat()
        val fz = cos(r).toFloat()

        camera.position.set(
            p.x - fx * 13f,
            7.5f,
            p.z - fz * 13f
        )
        camera.lookAt(
            p.x + fx * 8f,
            1f,
            p.z + fz * 8f
        )
        camera.up.set(Vector3.Y)
        camera.update()
    }

    private fun hud() {
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()
        val f = game.snapshot

        batch.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, w, h)
        batch.begin()
        font.color = Color.WHITE
        font.draw(batch, "SPEED " + f.player.speedKmh.toInt() + " KM/H", 28f, h - 26f)
        font.draw(
            batch,
            "LAP " + (f.race.lap + 1) + "/" + f.race.totalLaps + "  POS " + f.race.position + "/4",
            28f,
            h - 58f
        )
        font.draw(batch, "TRACK " + (trackIndex + 1), w - 315f, h - 26f)
        batch.end()

        shapes.projectionMatrix = Matrix4().setToOrtho2D(0f, 0f, w, h)
        shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(0.04f, 0.06f, 0.08f, 0.85f)
        shapes.rect(25f, 25f, 155f, 155f)
        shapes.rect(w - 180f, 25f, 155f, 155f)
        shapes.rect(w * 0.39f, 25f, w * 0.22f, 105f)
        shapes.setColor(0.1f, 0.5f, 0.85f, 0.9f)
        shapes.circle(103f, 103f, 45f, 24)
        shapes.circle(w - 103f, 103f, 45f, 24)
        shapes.setColor(0.2f, 0.8f, 0.95f, 0.9f)
        shapes.rect(w * 0.44f, 48f, w * 0.12f, 65f)
        shapes.end()

        batch.begin()
        font.data.setScale(1.1f)
        font.draw(batch, "<", 96f, 110f)
        font.draw(batch, ">", w - 110f, 110f)
        font.draw(batch, "ACCEL", w * 0.465f, 88f)
        font.data.setScale(0.9f)
        font.draw(batch, "1", w - 305f, h - 58f)
        font.draw(batch, "2", w - 195f, h - 58f)
        font.draw(batch, "3", w - 85f, h - 58f)
        font.draw(batch, "BRAKE", w * 0.45f, 158f)
        batch.end()
    }

    override fun resize(w: Int, h: Int) {
        camera.viewportWidth = max(w, 1).toFloat()
        camera.viewportHeight = max(h, 1).toFloat()
        camera.update()
    }

    override fun dispose() {
        manager.dispose()
        assets.forEach { it.dispose() }
        fallbackModels.forEach { it.dispose() }
        batch.dispose()
        font.dispose()
        shapes.dispose()
        roadModel.dispose()
        roadStripeModel.dispose()
        roadCurbModel.dispose()
    }
}
