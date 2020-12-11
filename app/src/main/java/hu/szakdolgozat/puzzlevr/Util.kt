package hu.szakdolgozat.puzzlevr

import android.opengl.GLES20
import android.opengl.GLU
import android.text.TextUtils
import android.util.Log


// Utility functions.
internal object Util {
    private const val TAG = "Util"

    //Debug builds should fail quickly. Release versions of the app should have this disabled.
    private const val HALT_ON_GL_ERROR = true

    //Checks GLES20.glGetError and fails quickly if the state isn't GL_NO_ERROR.
    fun checkGlError(label: String) {
        var error = GLES20.glGetError()
        var lastError: Int
        if (error != GLES20.GL_NO_ERROR) {
            do {
                lastError = error
                Log.e(TAG, label + ": glError " + GLU.gluErrorString(lastError))
                error = GLES20.glGetError()
            } while (error != GLES20.GL_NO_ERROR)
            if (HALT_ON_GL_ERROR) {
                throw RuntimeException("glError " + GLU.gluErrorString(lastError))
            }
        }
    }

    //Builds a GL shader program from vertex & fragment shader code. The vertex and fragment shaders
    //are passed as arrays of strings in order to make debugging compilation issues easier.
    fun compileProgram(vertexCode: Array<String?>?, fragmentCode: Array<String?>?): Int {
        checkGlError("Start of compileProgram")
        // prepare shaders and OpenGL program
        val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
        GLES20.glShaderSource(vertexShader, TextUtils.join("\n", vertexCode!!))
        GLES20.glCompileShader(vertexShader)
        checkGlError("Compile vertex shader")
        val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
        GLES20.glShaderSource(fragmentShader, TextUtils.join("\n", fragmentCode!!))
        GLES20.glCompileShader(fragmentShader)
        checkGlError("Compile fragment shader")
        val program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)

        // Link and check for errors.
        GLES20.glLinkProgram(program)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            val errorMsg = """
                Unable to link shader program: 
                ${GLES20.glGetProgramInfoLog(program)}
                """.trimIndent()
            Log.e(TAG, errorMsg)
            if (HALT_ON_GL_ERROR) {
                throw RuntimeException(errorMsg)
            }
        }
        checkGlError("End of compileProgram")
        return program
    }
}
