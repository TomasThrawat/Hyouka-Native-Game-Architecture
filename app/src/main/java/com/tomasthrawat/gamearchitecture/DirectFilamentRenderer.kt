package com.tomasthrawat.gamearchitecture

import android.content.Context
import android.opengl.Matrix
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import com.google.android.filament.filamat.MaterialBuilder
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.FilamentAsset
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

class DirectFilamentRenderer(
    context: Context,
    private val track: Track
) : GameRenderer {
    val surfaceView = SurfaceView(context)
    var frame: GameSnapshot = GameSnapshot(
        player = CarFrame("player", 0f, 0f, 0f, 0f, 0),
        opponents = emptyList(),
        race = RaceFrame(0, 3, 1, false, 0f)
    )
        private set

    private val engine: Engine
    private val renderer: Renderer
    private val scene: Scene
    private val view: View
    private val camera: Camera
    private val uiHelper: UiHelper
    private var swapChain: SwapChain? = null
    private val materialProvider: UbershaderProvider
    private val assetLoader: AssetLoader
    private val resourceLoader: ResourceLoader
    private val assets = mutableMapOf<String, FilamentAsset>()
    private val ready = IntArray(128)
    private val matrices = mutableMapOf<String, FloatArray>()

    private val roadMaterial: Material
    private val roadInstance: MaterialInstance
    private val roadVertexBuffer: VertexBuffer
    private val roadIndexBuffer: IndexBuffer
    private val roadEntity: Int
    private val lineMaterial: Material
    private val lineInstance: MaterialInstance
    private val lineVertexBuffer: VertexBuffer
    private val lineIndexBuffer: IndexBuffer
    private val lineEntity: Int

    init {
        Filament.init()
        MaterialBuilder.init()
        engine = Engine.create(Engine.Backend.OPENGL)
        renderer = engine.createRenderer()
        scene = engine.createScene()
        view = engine.createView()
        camera = engine.createCamera(engine.entityManager.create())
        view.scene = scene
        view.camera = camera
        view.renderQuality = view.renderQuality.apply {
            hdrColorBuffer = View.QualityLevel.MEDIUM
        }
        view.dynamicResolutionOptions = view.dynamicResolutionOptions.apply {
            enabled = true
            quality = View.QualityLevel.MEDIUM
        }
        view.antiAliasing = View.AntiAliasing.FXAA

        materialProvider = UbershaderProvider(engine)
        assetLoader = AssetLoader(engine, materialProvider, EntityManager.get())
        resourceLoader = ResourceLoader(engine, true)

        val light = EntityManager.get().create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.94f, 0.88f)
            .intensity(100_000.0f)
            .direction(0.5f, -1.0f, -0.7f)
            .castShadows(true)
            .build(engine, light)
        scene.addEntity(light)

        scene.skybox = Skybox.Builder()
            .color(0.025f, 0.045f, 0.075f, 1.0f)
            .build(engine)

        val road = buildColorMaterial("road", 0.045f, 0.052f, 0.065f, 1f)
        roadMaterial = road.first
        roadInstance = road.second
        val roadMesh = buildStripMesh(track, track.definition.halfWidthMeters, ROAD_SEGMENTS)
        roadVertexBuffer = roadMesh.first
        roadIndexBuffer = roadMesh.second
        roadEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-80f, -0.2f, -80f, 80f, 0.2f, 80f))
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, roadVertexBuffer, roadIndexBuffer)
            .material(0, roadInstance)
            .culling(false)
            .receiveShadows(true)
            .build(engine, roadEntity)
        scene.addEntity(roadEntity)

        val line = buildColorMaterial("center-line", 0.92f, 0.82f, 0.28f, 1f)
        lineMaterial = line.first
        lineInstance = line.second
        val lineMesh = buildStripMesh(track, 0.055f, ROAD_SEGMENTS)
        lineVertexBuffer = lineMesh.first
        lineIndexBuffer = lineMesh.second
        lineEntity = EntityManager.get().create()
        RenderableManager.Builder(1)
            .boundingBox(Box(-80f, -0.01f, -80f, 80f, 0.05f, 80f))
            .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, lineVertexBuffer, lineIndexBuffer)
            .material(0, lineInstance)
            .culling(false)
            .build(engine, lineEntity)
        scene.addEntity(lineEntity)

        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
        uiHelper.renderCallback = object : UiHelper.RendererCallback {
            override fun onNativeWindowChanged(surface: Surface) {
                swapChain?.let { engine.destroySwapChain(it) }
                swapChain = engine.createSwapChain(surface)
            }
            override fun onDetachedFromSurface() {
                swapChain?.let {
                    engine.destroySwapChain(it)
                    swapChain = null
                }
            }
            override fun onResized(width: Int, height: Int) {
                view.viewport = Viewport(0, 0, width, height)
                if (height > 0) {
                    camera.setProjection(60.0, width.toDouble() / height.toDouble(), 0.05, 500.0, Camera.Fov.VERTICAL)
                }
            }
        }
        uiHelper.attachTo(surfaceView)
    }

    override fun render(frame: GameSnapshot) {
        this.frame = frame
        updateAssetTransforms(frame)
        updateCamera(frame)
        val now = System.nanoTime()
        if (uiHelper.isReadyToRender && swapChain != null && renderer.beginFrame(swapChain!!, now)) {
            renderer.render(view)
            renderer.endFrame()
        }
    }

    private fun updateAssetTransforms(frame: GameSnapshot) {
        transform("player", frame.player.progress, frame.player.lateralOffset, frame.player.yawDegrees, 2.2f)
        frame.opponents.forEachIndexed { index, car ->
            transform("rival_$index", car.progress, car.lateralOffset, car.yawDegrees, 2.0f)
        }
        finalizePendingAssets()
    }

    private fun transform(id: String, progress: Float, lateral: Float, yaw: Float, scale: Float) {
        val asset = assets[id] ?: return
        val pose = track.pose(progress, lateral)
        val matrix = matrices.getOrPut(id) { FloatArray(16) }
        Matrix.setIdentityM(matrix, 0)
        Matrix.translateM(matrix, 0, pose.x, 0.02f, pose.z)
        Matrix.rotateM(matrix, 0, yaw, 0f, 1f, 0f)
        Matrix.scaleM(matrix, 0, scale, scale, scale)
        engine.transformManager.setTransform(engine.transformManager.getInstance(asset.root), matrix)
    }

    private fun finalizePendingAssets() {
        resourceLoader.asyncUpdateLoad()
        assets.values.forEach { asset ->
            var count = asset.popRenderables(ready)
            while (count > 0) {
                scene.addEntities(ready.copyOf(count))
                count = asset.popRenderables(ready)
            }
            scene.addEntities(asset.lightEntities)
        }
    }

    private fun updateCamera(frame: GameSnapshot) {
        val pose = track.pose(frame.player.progress, frame.player.lateralOffset)
        val yaw = Math.toRadians(frame.player.yawDegrees.toDouble())
        val forwardX = sin(yaw).toFloat()
        val forwardZ = cos(yaw).toFloat()
        camera.lookAt(
            pose.x - forwardX * 6.0, 2.6, pose.z - forwardZ * 6.0,
            pose.x + forwardX * 2.0, 0.8, pose.z + forwardZ * 2.0,
            0.0, 1.0, 0.0
        )
    }

    fun loadModel(id: String, path: String) {
        if (assets.containsKey(id)) return
        val bytes = surfaceView.context.assets.open(path).use { it.readBytes() }
        val asset = assetLoader.createAsset(ByteBuffer.wrap(bytes)) ?: return
        resourceLoader.asyncBeginLoad(asset)
        asset.releaseSourceData()
        assets[id] = asset
    }

    fun destroy() {
        uiHelper.detach()
        resourceLoader.asyncCancelLoad()
        resourceLoader.evictResourceData()
        assets.values.forEach {
            scene.removeEntities(it.entities)
            assetLoader.destroyAsset(it)
        }
        assets.clear()
        engine.destroyEntity(roadEntity)
        engine.destroyEntity(lineEntity)
        engine.destroyRenderer(renderer)
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyCameraComponent(camera.entity)
        EntityManager.get().destroy(camera.entity)
        engine.destroyVertexBuffer(roadVertexBuffer)
        engine.destroyIndexBuffer(roadIndexBuffer)
        engine.destroyMaterialInstance(roadInstance)
        engine.destroyMaterial(roadMaterial)
        engine.destroyVertexBuffer(lineVertexBuffer)
        engine.destroyIndexBuffer(lineIndexBuffer)
        engine.destroyMaterialInstance(lineInstance)
        engine.destroyMaterial(lineMaterial)
        resourceLoader.destroy()
        assetLoader.destroy()
        materialProvider.destroyMaterials()
        materialProvider.destroy()
        engine.destroy()
        MaterialBuilder.shutdown()
    }

    private fun buildColorMaterial(name: String, r: Float, g: Float, b: Float, a: Float): Pair<Material, MaterialInstance> {
        val pkg = MaterialBuilder()
            .name(name)
            .platform(MaterialBuilder.Platform.MOBILE)
            .targetApi(MaterialBuilder.TargetApi.OPENGL)
            .shading(MaterialBuilder.Shading.UNLIT)
            .uniformParameter(MaterialBuilder.UniformType.FLOAT4, "color")
            .material(
                "void material(inout MaterialInputs material) {" +
                    "prepareMaterial(material);" +
                    "material.baseColor = materialParams.color;" +
                "}"
            )
            .build(engine)
        check(pkg.isValid) { "Filament material compilation failed: $name" }
        val buffer = pkg.buffer
        val material = Material.Builder().payload(buffer, buffer.remaining()).build(engine)
        val instance = material.createInstance()
        instance.setParameter("color", r, g, b, a)
        return material to instance
    }

    private fun buildStripMesh(track: Track, halfWidth: Float, segments: Int): Pair<VertexBuffer, IndexBuffer> {
        val vertices = ByteBuffer.allocateDirect(segments * 2 * 3 * 4).order(java.nio.ByteOrder.nativeOrder())
        val indices = ByteBuffer.allocateDirect(segments * 6 * 2).order(java.nio.ByteOrder.nativeOrder())
        repeat(segments) { i ->
            val p = i.toFloat() / segments
            val a = track.pose(p, -halfWidth)
            val b = track.pose(p, halfWidth)
            vertices.putFloat(a.x).putFloat(0f).putFloat(a.z)
            vertices.putFloat(b.x).putFloat(0f).putFloat(b.z)
            val j = (i * 2).toShort()
            val n = (((i + 1) % segments) * 2).toShort()
            indices.putShort(j).putShort(n).putShort((n + 1).toShort())
            indices.putShort(j).putShort((n + 1).toShort()).putShort((j + 1).toShort())
        }
        vertices.flip()
        indices.flip()
        val vb = VertexBuffer.Builder()
            .vertexCount(segments * 2)
            .bufferCount(1)
            .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3)
            .build(engine)
        vb.setBufferAt(engine, 0, vertices.asFloatBuffer())
        val ib = IndexBuffer.Builder()
            .indexCount(segments * 6)
            .bufferType(IndexBuffer.Builder.IndexType.USHORT)
            .build(engine)
        ib.setBuffer(engine, indices.asShortBuffer())
        return vb to ib
    }

    companion object { private const val ROAD_SEGMENTS = 96 }
}