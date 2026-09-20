package com.tomasthrawat.gamearchitecture
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.sqrt

data class TrackPose(val x: Float,val z: Float,val yawDegrees: Float)

class Track(val definition: TrackDefinition = TrackDefinition()) {
    private var layout = 0
    private val layouts = listOf(
        listOf(-72f to -18f,-36f to -18f,0f to -18f,36f to -18f,68f to -8f,68f to 24f,44f to 48f,4f to 58f,-34f to 48f,-62f to 24f),
        listOf(-78f to -8f,-42f to -8f,-10f to -8f,18f to 4f,48f to 30f,26f to 58f,-12f to 52f,-42f to 32f,-66f to 48f,-82f to 28f),
        listOf(-78f to -26f,-34f to -26f,10f to -18f,58f to -4f,60f to 24f,32f to 48f,-2f to 36f,-28f to 14f,-58f to 22f,-72f to 52f,-26f to 72f,26f to 64f,72f to 42f)
    )
    fun selectLayout(i:Int){ layout=i.coerceIn(0,2) }
    fun waypoints()=layouts[layout]
    fun pose(progress:Float,lateralOffset:Float=0f):TrackPose{
        val p=layouts[layout]; val n=p.size; val u=((progress%1f)+1f)%1f*n
        val i=floor(u).toInt()%n; val t=u-floor(u)
        val a=p[(i-1+n)%n]; val b=p[i]; val c=p[(i+1)%n]; val d=p[(i+2)%n]
        fun cr(x:Float,y:Float,z:Float,w:Float)=0.5f*((2f*y)+(-x+z)*t+(2f*x-5f*y+4f*z-w)*t*t+(-x+3f*y-3f*z+w)*t*t*t)
        val x=cr(a.first,b.first,c.first,d.first); val z=cr(a.second,b.second,c.second,d.second)
        val tx=cr(a.first,b.first,c.first,d.first+0.001f)-x; val tz=cr(a.second,b.second,c.second,d.second+0.001f)-z
        val l=sqrt(tx*tx+tz*tz).coerceAtLeast(0.001f); val nx=-tz/l; val nz=tx/l
        return TrackPose(x+nx*lateralOffset,z+nz*lateralOffset,Math.toDegrees(atan2(tx.toDouble(),tz.toDouble())).toFloat())
    }
}
