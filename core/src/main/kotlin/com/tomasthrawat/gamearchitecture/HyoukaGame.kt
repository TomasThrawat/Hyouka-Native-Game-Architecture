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
    private var roadModel: Model? = null
    private var roadStripeModel: Model? = null
    private var roadCurbModel: Model? = null
    private var roadInstance: ModelInstance? = null
    private var roadStripeInstance: ModelInstance? = null
    private var roadCurbInstance: ModelInstance? = null
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


        Gdx.app.log(
            "HyoukaGame",
            "Assets ready: " + loaded.size + "/" + names.size + " GLB files"
        )
    }

    private fun selectTrack(i: Int) {
        trackIndex = i.coerceIn(0, 2)
        game.track.selectLayout(trackIndex)
        game.reset()
        rebuildProceduralRoadModels()
    }

    private fun rebuildProceduralRoadModels() {
        roadInstance?.let {
            manager.getRenderableProviders().removeValue(it, true)
        }
        roadStripeInstance?.let {
            manager.getRenderableProviders().removeValue(it, true)
        }
        roadCurbInstance?.let {
            manager.getRenderableProviders().removeValue(it, true)
        }

        roadModel?.dispose()
        roadStripeModel?.dispose()
        roadCurbModel?.dispose()

        roadInstance = null
        roadStripeInstance = null
        roadCurbInstance = null

        val roadMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.16f, 0.17f, 0.18f, 1f))
        )
        val stripeMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.92f, 0.92f, 0.88f, 1f))
        )
        val curbMaterial = Material(
            PBRColorAttribute.createBaseColorFactor(Color(0.88f, 0.10f, 0.08f, 1f))
        )

        val points = game.track.sampledCenterline(24)
        val halfWidth = game.track.definition.halfWidthMeters

        roadModel = buildTrackStrip(points, halfWidth, 0.0f, roadMaterial, "road")
        roadStripeModel = buildTrackStrip(
            points, 0.11f, 0.045f, stripeMaterial, "center"
        )
        roadCurbModel = buildTrackCurbs(
            points, halfWidth + 0.04f, 0.075f, curbMaterial
        )

        roadInstance = ModelInstance(roadModel)
        roadStripeInstance = ModelInstance(roadStripeModel)
        roadCurbInstance = ModelInstance(roadCurbModel)

        manager.getRenderableProviders().add(roadInstance)
        manager.getRenderableProviders().add(roadStripeInstance)
        manager.getRenderableProviders().add(roadCurbInstance)

        Gdx.app.log(
            "HyoukaGame",
            "Track mesh rebuilt: layout=" + (trackIndex + 1) +
                " samples=" + points.size +
                " length=" + game.track.lengthMeters()
        )
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
        val attributes =
            VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong()
        val part = builder.part(id, GL20.GL_TRIANGLES, attributes, material)

        fun info(x: Float, z: Float) =
            MeshPartBuilder.VertexInfo().setPos(x, y, z).setNor(0f, 1f, 0f)

        fun edge(i: Int): Pair<Pair<Float, Float>, Pair<Float, Float>> {
            val n = points.size
            val prev = points[(i - 1 + n) % n]
            val cur = points[i]
            val next = points[(i + 1) % n]

            var tx = next.first - prev.first
            var tz = next.second - prev.second
            var len = sqrt(tx * tx + tz * tz)
            if (len < 0.001f) {
                tx = next.first - cur.first
                tz = next.second - cur.second
                len = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            }

            tx /= len
            tz /= len
            val nx = -tz
            val nz = tx

            return Pair(
                cur.first + nx * halfWidth to cur.second + nz * halfWidth,
                cur.first - nx * halfWidth to cur.second - nz * halfWidth
            )
        }

        for (i in points.indices) {
            val a = edge(i)
            val b = edge((i + 1) % points.size)

            part.rect(
                info(a.second.first, a.second.second),
                info(a.first.first, a.first.second),
                info(b.first.first, b.first.second),
                info(b.second.first, b.second.second)
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
        val attributes =
            VertexAttributes.Usage.Position.toLong() or
                VertexAttributes.Usage.Normal.toLong()
        val part = builder.part("curbs", GL20.GL_TRIANGLES, attributes, material)

        fun info(x: Float, z: Float) =
            MeshPartBuilder.VertexInfo().setPos(x, y, z).setNor(0f, 1f, 0f)

        fun edge(i: Int): Pair<Pair<Float, Float>, Pair<Float, Float>> {
            val n = points.size
            val prev = points[(i - 1 + n) % n]
            val cur = points[i]
            val next = points[(i + 1) % n]

            var tx = next.first - prev.first
            var tz = next.second - prev.second
            var len = sqrt(tx * tx + tz * tz)
            if (len < 0.001f) {
                tx = next.first - cur.first
                tz = next.second - cur.second
                len = sqrt(tx * tx + tz * tz).coerceAtLeast(0.001f)
            }

            tx /= len
            tz /= len
            val nx = -tz
            val nz = tx

            return Pair(
                cur.first + nx * halfWidth to cur.second + nz * halfWidth,
                cur.first - nx * halfWidth to cur.second - nz * halfWidth
            )
        }

        for (i in points.indices) {
            val a = edge(i)
            val b = edge((i + 1) % points.size)

            val lDx = b.first.first - a.first.first
            val lDz = b.first.second - a.first.second
            val rDx = b.second.first - a.second.first
            val rDz = b.second.second - a.second.second

            part.rect(
                info(a.first.first - lDx * 0.02f, a.first.second - lDz * 0.02f),
                info(a.first.first + lDx * 0.02f, a.first.second + lDz * 0.02f),
                info(b.first.first + lDx * 0.02f, b.first.second + lDz * 0.02f),
                info(b.first.first - lDx * 0.02f, b.first.second - lDz * 0.02f)
            )
            part.rect(
                info(a.second.first + rDx * 0.02f, a.second.second + rDz * 0.02f),
                info(a.second.first - rDx * 0.02f, a.second.second - rDz * 0.02f),
                info(b.second.first - rDx * 0.02f, b.second.second - rDz * 0.02f),
                info(b.second.first + rDx * 0.02f, b.second.second + rDz * 0.02f)
            )
        }

        return builder.end()
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
        roadModel?.dispose()
        roadStripeModel?.dispose()
        roadCurbModel?.dispose()
    }
}
