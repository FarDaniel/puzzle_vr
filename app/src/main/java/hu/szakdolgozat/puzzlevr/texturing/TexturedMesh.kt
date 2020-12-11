package hu.szakdolgozat.puzzlevr.texturing

import android.content.Context
import android.opengl.GLES20
import de.javagl.obj.ObjData
import de.javagl.obj.ObjReader
import de.javagl.obj.ObjUtils
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class TexturedMesh {
    private var vertices: FloatBuffer
    private var uv: FloatBuffer
    private var indices: ShortBuffer
    private var positionAttrib = 0
    private var uvAttrib = 0

    @Throws(IOException::class)
    constructor(context: Context, objFilePath: String, positionAttrib: Int, uvAttrib: Int) {
        val objInputStream = context.assets.open(objFilePath)
        val obj = ObjUtils.convertToRenderable(ObjReader.read(objInputStream))
        objInputStream.close()

        val intIndices = ObjData.getFaceVertexIndices(obj, 3)
        vertices = ObjData.getVertices(obj)
        uv = ObjData.getTexCoords(obj, 2)

        // Convert int indices to shorts (GLES doesn't support int indices)
        indices = ByteBuffer.allocateDirect(2 * intIndices.limit())
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
        while (intIndices.hasRemaining()) {
            indices.put(intIndices.get().toShort())
        }
        indices.rewind()
        this.positionAttrib = positionAttrib
        this.uvAttrib = uvAttrib
    }


    //Draws the mesh. Before this is called, u_MVP should be set with glUniformMatrix4fv(), and a
    //texture should be bound to GL_TEXTURE0.
    fun draw() {
        GLES20.glEnableVertexAttribArray(positionAttrib)
        GLES20.glVertexAttribPointer(positionAttrib, 3, GLES20.GL_FLOAT, false, 0, vertices)
        GLES20.glEnableVertexAttribArray(uvAttrib)
        GLES20.glVertexAttribPointer(uvAttrib, 2, GLES20.GL_FLOAT, false, 0, uv)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indices.limit(),
            GLES20.GL_UNSIGNED_SHORT,
            indices
        )
    }
}