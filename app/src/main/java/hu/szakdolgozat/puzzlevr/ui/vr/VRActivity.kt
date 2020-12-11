package hu.szakdolgozat.puzzlevr.ui.vr

import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.opengl.GLES20
import android.opengl.Matrix
import android.os.Bundle
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import com.google.vr.ndk.base.Properties
import com.google.vr.ndk.base.Properties.PropertyType
import com.google.vr.ndk.base.Value
import com.google.vr.sdk.base.*
import hu.szakdolgozat.puzzlevr.R
import hu.szakdolgozat.puzzlevr.Util
import hu.szakdolgozat.puzzlevr.puzzle.Item
import hu.szakdolgozat.puzzlevr.puzzle.Player
import hu.szakdolgozat.puzzlevr.texturing.Texture
import hu.szakdolgozat.puzzlevr.texturing.TexturedMesh
import javax.microedition.khronos.egl.EGLConfig


class VRActivity : GvrActivity(), GvrView.StereoRenderer {
    private var intentFilter = IntentFilter()
    private val Z_NEAR = 0.01f
    private val Z_FAR = 10.0f

    private val DEFAULT_FLOOR_HEIGHT = -4.0f
    private val OBJECT_VERTEX_SHADER_CODE: Array<String?>? = arrayOf(
        "uniform mat4 u_MVP;",
        "attribute vec4 a_Position;",
        "attribute vec2 a_UV;",
        "varying vec2 v_UV;",
        "",
        "void main() {",
        "  v_UV = a_UV;",
        "  gl_Position = u_MVP * a_Position;",
        "}"
    )
    private val OBJECT_FRAGMENT_SHADER_CODE: Array<String?>? = arrayOf(
        "precision mediump float;",
        "varying vec2 v_UV;",
        "uniform sampler2D u_Texture;",
        "",
        "void main() {",
        "  // The y coordinate of this sample's textures is reversed compared to",
        "  // what OpenGL expects, so we invert the y coordinate.",
        "  gl_FragColor = texture2D(u_Texture, vec2(v_UV.x, 1.0 - v_UV.y));",
        "}"
    )

    private lateinit var room: TexturedMesh
    private lateinit var roomTex: Texture
    private lateinit var roomTexWon: Texture

    private var objectProgram = 0
    var objectPositionParam = 0
    var objectUvParam = 0
    private var objectModelViewProjectionParam = 0

    private lateinit var gvrProperties: Properties

    lateinit var viewModel: VRViewModel

    private val floorHeight = Value()
    private val headRotation = FloatArray(4)
    private val modelViewProjection = FloatArray(16)
    private val visionTarget = FloatArray(16)
    private val modelRoom = FloatArray(16)
    private val modelView = FloatArray(16)
    private val headView = FloatArray(16)
    private val camera = FloatArray(16)
    private val view = FloatArray(16)
    private var isServer = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeGvrView()

        when (intent.getIntExtra("isServer", -1)) {
            1 -> {
                //It's the server
                viewModel = VRViewModel(1, null)
                viewModel.tableTop.isMultiPlayer = true
                this.isServer = true
            }
            0 -> {
                //It's the client
                val address = intent.getByteArrayExtra("address")
                viewModel = VRViewModel(0, address)
                viewModel.tableTop.isMultiPlayer = true
                this.isServer = false
            }
            else -> {
                //It's a single player game
                viewModel = VRViewModel(-1, null)
                viewModel.tableTop.isMultiPlayer = false

            }
        }

        // Initialize 3D audio engine.
        viewModel.tableTop.initializeAudioEngine(this)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    }

    private fun initializeGvrView() {
        setContentView(R.layout.activity_vr)
        val gvrView = findViewById<View>(R.id.gvr_view) as GvrView
        gvrView.setEGLConfigChooser(8, 8, 8, 8, 16, 8)
        gvrView.setRenderer(this)
        gvrView.setTransitionViewEnabled(true)

        // Enable Cardboard-trigger feedback with Daydream headsets.
        gvrView.enableCardboardTriggerEmulation()
        if (gvrView.setAsyncReprojectionEnabled(true)) {
            // Async reprojection decouples the app framerate from the display framerate,
            // allowing immersive interaction even at the throttled clockrates set by
            // sustained performance mode.
            AndroidCompat.setSustainedPerformanceMode(this, true)
        }
        setGvrView(gvrView)
        gvrProperties = gvrView.gvrApi.currentProperties
    }

    override fun onNewFrame(headTransform: HeadTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, 1.0f, 0.0f)

        headTransform.getForwardVector(viewModel.tableTop.playerOne.position, 0)

        //setting the maximal distance of the cursor from the player
        for (i in 0..2) {
            viewModel.tableTop.playerOne.position[i] = viewModel.tableTop.playerOne.position[i] * 4
        }

        if (gvrProperties.get(PropertyType.TRACKING_FLOOR_HEIGHT, floorHeight)) {
            // The floor height can change each frame when tracking system detects a new floor position.
            Matrix.setIdentityM(modelRoom, 0)
            Matrix.translateM(modelRoom, 0, 0f, floorHeight.asFloat(), 0f)
        } // else the device doesn't support floor height detection so DEFAULT_FLOOR_HEIGHT is used.

        headTransform.getHeadView(headView, 0)
        headTransform.getQuaternion(headRotation, 0)

        //Needid at the sounds
        viewModel.tableTop.setHeadRotation(headRotation)
        //Updating the Items on the board, applying gravity, and every movement.
        viewModel.tableTop.updateBoard()



        Util.checkGlError("onNewFrame")
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(viewModel.broadcastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(viewModel.broadcastReceiver)
    }

    override fun onDestroy() {
        viewModel.closeCommunication()
        super.onDestroy()
    }

    override fun onDrawEye(eye: Eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // The clear color doesn't matter here because it's completely obscured by
        // the room. However, the color buffer is still cleared because it may
        // improve performance.
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.eyeView, 0, camera, 0)

        // Build the ModelView and ModelViewProjection matrices
        // for calculating the position of the target object.
        val perspective: FloatArray = eye.getPerspective(
            Z_NEAR,
            Z_FAR
        )

        // Set modelView for the room, so it's drawn in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelRoom, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
        //drawing out the room around the player
        drawRoom()

        //Drawing the cursor of player one
        drawCursor(viewModel.tableTop.playerOne, perspective)

        //If it's a multiplayer game drawing the cursor of player two too
        if (viewModel.tableTop.isMultiPlayer) {
            drawCursor(viewModel.tableTop.playerTwo, perspective)
        }


        viewModel.tableTop.items.forEach { item ->

            Matrix.multiplyMM(modelView, 0, view, 0, item.target, 0)
            Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)
            //Drawing every Item, the order is important because there can be invisible parts.
            drawItem(item)

        }
    }

    //Don't needed functions, but GVrActivity needs them
    override fun onFinishFrame(viewPort: Viewport?) {}

    override fun onSurfaceChanged(p0: Int, p1: Int) {}

    override fun onSurfaceCreated(p0: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        objectProgram = Util.compileProgram(
            OBJECT_VERTEX_SHADER_CODE,
            OBJECT_FRAGMENT_SHADER_CODE
        )

        objectPositionParam = GLES20.glGetAttribLocation(objectProgram, "a_Position")
        objectUvParam = GLES20.glGetAttribLocation(objectProgram, "a_UV")
        objectModelViewProjectionParam = GLES20.glGetUniformLocation(objectProgram, "u_MVP")

        Util.checkGlError("Object program params")

        Matrix.setIdentityM(modelRoom, 0)
        Matrix.translateM(
            modelRoom,
            0,
            0f,
            DEFAULT_FLOOR_HEIGHT,
            0f
        )

        Util.checkGlError("onSurfaceCreated")

        //Initializing the sound engine
        viewModel.tableTop.initSounds()
        //Making the Items for every table (only have one right now)
        viewModel.loadObjects(this)

        try {
            //Loading room texture
            room = TexturedMesh(this, "objects/CubeRoom.obj", objectPositionParam, objectUvParam)
            roomTex = Texture(this, "textures/CubeRoom_BakedDiffuse.png")
            roomTexWon = Texture(this, "textures/CubeRoom_Won_BakedDiffuse.png")
        } catch (exception: Exception) {
            Log.e("Main", "Loading the room")
        }

    }

    override fun onRendererShutdown() {
        floorHeight.close()
    }

    private fun drawCursor(player: Player, perspective: FloatArray) {

        Matrix.setIdentityM(visionTarget, 0)
        Matrix.translateM(visionTarget, 0, player.position[0], -3.4f, player.position[2])

        Matrix.multiplyMM(modelView, 0, view, 0, visionTarget, 0)
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0)

        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        player.cursorTexture.bind()

        player.cursorTexturedMesh.draw()

        Util.checkGlError("drawCursor")
    }

    private fun drawItem(item: Item) {
        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        item.texture.bind()

        item.mesh.draw()

        Util.checkGlError("drawCursor")
    }

    private fun drawRoom() {
        GLES20.glUseProgram(objectProgram)
        GLES20.glUniformMatrix4fv(objectModelViewProjectionParam, 1, false, modelViewProjection, 0)
        val texture = if (!viewModel.isDone) roomTex else roomTexWon
        texture.bind()

        room.draw()

        Util.checkGlError("drawRoom")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        //Making player one triggered if clicking any button
        viewModel.tableTop.playerOne.triggered = true
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        //Making player one not triggered if releasing any button
        viewModel.tableTop.playerOne.triggered = false
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        return if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK
            && event.action == MotionEvent.ACTION_MOVE
        ) {
            viewModel.tableTop.isLeftPressed = event.getAxisValue(MotionEvent.AXIS_X) < -0.5f
            viewModel.tableTop.isRightPressed = event.getAxisValue(MotionEvent.AXIS_X) > 0.5f
            true
        } else {
            return super.onGenericMotionEvent(event)
        }
    }

    override fun onCardboardTrigger() {
        //Changing the state of trigger, on pull
        super.onCardboardTrigger()
        viewModel.tableTop.playerOne.triggered = !viewModel.tableTop.playerOne.triggered
    }
}
