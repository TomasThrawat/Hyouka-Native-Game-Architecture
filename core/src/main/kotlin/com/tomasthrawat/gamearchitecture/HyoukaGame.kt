package com.tomasthrawat.gamearchitecture

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import net.mgsx.gltf.loaders.glb.GLBLoader
import net.mgsx.gltf.scene3d.scene.Scene
import net.mgsx.gltf.scene3d.scene.SceneAsset
import net.mgsx.gltf.scene3d.scene.SceneManager
import net.mgsx.gltf.scene3d.shaders.PBRShaderProvider
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class HyoukaGame:ApplicationAdapter(){
    private lateinit var game:Game
    private lateinit var manager:SceneManager
    private lateinit var camera:PerspectiveCamera
    private lateinit var batch:SpriteBatch
    private lateinit var font:BitmapFont
    private lateinit var shapes:ShapeRenderer
    private val assets=mutableListOf<SceneAsset>()
    private val cars=mutableListOf<Scene>()
    private val roads=mutableListOf<Scene>()
    private var trackIndex=0

    override fun create(){
        game=Game()
        val cfg=PBRShaderProvider.createDefaultConfig()
        cfg.numDirectionalLights=2; cfg.numPointLights=0; cfg.numSpotLights=0; cfg.numBones=0
        manager=SceneManager(PBRShaderProvider.createDefault(cfg), PBRShaderProvider.createDefaultDepth(PBRShaderProvider.createDefaultDepthConfig()))
        camera=PerspectiveCamera(62f,max(Gdx.graphics.width,1).toFloat(),max(Gdx.graphics.height,1).toFloat())
        camera.near=.1f; camera.far=420f
        batch=SpriteBatch(); font=BitmapFont(); shapes=ShapeRenderer()
        loadAssets(); selectTrack(0)
    }

    private fun loadAssets(){
        val names=listOf("player","rival1","rival2","straight","corner90","hairpin","grandstand","tyre_wall","start_gantry")
        val loaded=names.associateWith{n->GLBLoader().load(Gdx.files.internal("models/racing/"+n+".glb")).also{assets+=it}}
        fun add(n:String)=Scene(loaded.getValue(n).scene).also{manager.addScene(it)}
        repeat(4){cars+=add(if(it==0)"player" else if(it%2==0)"rival2" else "rival1")}
        repeat(18){roads+=add("straight")}; repeat(8){roads+=add("corner90")}
        repeat(3){roads+=add("hairpin")}; repeat(6){roads+=add("tyre_wall")}
        repeat(3){roads+=add("grandstand")}; repeat(1){roads+=add("start_gantry")}
    }

    private fun selectTrack(i:Int){trackIndex=i.coerceIn(0,2); game.track.selectLayout(trackIndex); game.reset(); layoutRoad()}

    private fun layoutRoad(){
        val p=game.track.waypoints()
        for(i in p.indices){
            val a=p[i]; val b=p[(i+1)%p.size]; val dx=b.first-a.first; val dz=b.second-a.second
            val len=sqrt(dx*dx+dz*dz); val yaw=Math.toDegrees(atan2(dx.toDouble(),dz.toDouble())).toFloat()
            roads[i%18].modelInstance.transform.setToTranslation((a.first+b.first)/2f,0f,(a.second+b.second)/2f).rotate(Vector3.Y,yaw).scale(1f,1f,(len/20f).coerceIn(.8f,3f))
            roads[18+(i%8)].modelInstance.transform.setToTranslation(b.first,0f,b.second).rotate(Vector3.Y,yaw)
            if(i%3==1) roads[26+((i/3)%6)].modelInstance.transform.setToTranslation(b.first+if(i%2==0)8f else -8f,0f,b.second)
        }
        roads[32].modelInstance.transform.setToTranslation(p[2].first,0f,p[2].second)
        roads[33].modelInstance.transform.setToTranslation(p[4].first+10f,0f,p[4].second+10f)
        roads[34].modelInstance.transform.setToTranslation(p[7].first-10f,0f,p[7].second+10f)
        roads[35].modelInstance.transform.setToTranslation(p[0].first,0f,p[0].second)
    }

    override fun render(){
        if(Gdx.input.justTouched()){
            val x=Gdx.input.x.toFloat(); val w=Gdx.graphics.width.toFloat()
            if(Gdx.graphics.height-Gdx.input.y<95f && x>w-330f) selectTrack(((x-(w-330f))/110f).toInt())
        }
        game.update(input(),Gdx.graphics.deltaTime.coerceIn(0f,.05f)); updateCars(); updateCamera()
        Gdx.gl.glClearColor(.015f,.02f,.028f,1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        manager.camera=camera; manager.update(Gdx.graphics.deltaTime); manager.render(); hud()
    }

    private fun input():GameInput{
        val w=Gdx.graphics.width.toFloat(); val h=Gdx.graphics.height.toFloat(); var s=0f; var t=0f; var b=0f
        if(Gdx.input.isTouched){
            val x=Gdx.input.x.toFloat(); val y=h-Gdx.input.y
            if(y< h*.4f){if(x<w*.3f)s=-1f;if(x>w*.7f)s=1f}
            if(x>w*.36f&&x<w*.64f){if(y>h*.52f)t=1f else b=1f}
        }
        if(Gdx.input.isKeyPressed(Keys.LEFT))s=-1f;if(Gdx.input.isKeyPressed(Keys.RIGHT))s=1f
        if(Gdx.input.isKeyPressed(Keys.UP))t=1f;if(Gdx.input.isKeyPressed(Keys.DOWN))b=1f
        if(Gdx.input.isKeyJustPressed(Keys.R))game.reset()
        return GameInput(t,b,s)
    }

    private fun updateCars(){
        val f=listOf(game.snapshot.player)+game.snapshot.opponents
        cars.forEachIndexed{i,c->val p=game.track.pose(f[i].progress,f[i].lateralOffset);c.modelInstance.transform.setToTranslation(p.x,.2f,p.z).rotate(Vector3.Y,p.yawDegrees).scale(1.05f,1.05f,1.05f)}
    }

    private fun updateCamera(){
        val p=game.track.pose(game.snapshot.player.progress,game.snapshot.player.lateralOffset); val r=Math.toRadians(p.yawDegrees.toDouble())
        val fx=sin(r).toFloat(); val fz=cos(r).toFloat()
        camera.position.set(p.x-fx*13f,7.5f,p.z-fz*13f);camera.lookAt(p.x+fx*8f,1f,p.z+fz*8f);camera.up.set(Vector3.Y);camera.update()
    }

    private fun hud(){
        val w=Gdx.graphics.width.toFloat(); val h=Gdx.graphics.height.toFloat(); val f=game.snapshot
        batch.projectionMatrix=Matrix4().setToOrtho2D(0f,0f,w,h); batch.begin(); font.color=Color.WHITE
        font.draw(batch,"SPEED "+f.player.speedKmh.toInt()+" KM/H",28f,h-26f)
        font.draw(batch,"LAP "+(f.race.lap+1)+"/"+f.race.totalLaps+"  POS "+f.race.position+"/4",28f,h-58f)
        font.draw(batch,"TRACK "+(trackIndex+1),w-315f,h-26f);batch.end()
        shapes.projectionMatrix=Matrix4().setToOrtho2D(0f,0f,w,h);shapes.begin(ShapeRenderer.ShapeType.Filled)
        shapes.setColor(.04f,.06f,.08f,.85f);shapes.rect(25f,25f,155f,155f);shapes.rect(w-180f,25f,155f,155f);shapes.rect(w*.39f,25f,w*.22f,105f)
        shapes.setColor(.1f,.5f,.85f,.9f);shapes.circle(103f,103f,45f,24);shapes.circle(w-103f,103f,45f,24)
        shapes.setColor(.2f,.8f,.95f,.9f);shapes.rect(w*.44f,48f,w*.12f,65f);shapes.end()
        batch.begin();font.data.setScale(1.1f);font.draw(batch,"<",96f,110f);font.draw(batch,">",w-110f,110f);font.draw(batch,"ACCEL",w*.465f,88f)
        font.data.setScale(.9f);font.draw(batch,"1",w-305f,h-58f);font.draw(batch,"2",w-195f,h-58f);font.draw(batch,"3",w-85f,h-58f);font.draw(batch,"BRAKE",w*.45f,158f);batch.end()
    }

    override fun resize(w:Int,h:Int){camera.viewportWidth=max(w,1).toFloat();camera.viewportHeight=max(h,1).toFloat();camera.update()}
    override fun dispose(){manager.dispose();assets.forEach{it.dispose()};batch.dispose();font.dispose();shapes.dispose()}
}
