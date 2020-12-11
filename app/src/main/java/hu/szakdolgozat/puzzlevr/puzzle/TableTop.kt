package hu.szakdolgozat.puzzlevr.puzzle

import android.util.Log
import com.google.vr.sdk.audio.GvrAudioEngine
import hu.szakdolgozat.puzzlevr.interactor.PuzzleInteractor
import hu.szakdolgozat.puzzlevr.texturing.Texture
import hu.szakdolgozat.puzzlevr.texturing.TexturedMesh
import hu.szakdolgozat.puzzlevr.ui.vr.VRActivity
import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel
import java.io.IOException
import kotlin.random.Random

class TableTop(private val viewModel: VRViewModel?) {
    private var headRotation: FloatArray = FloatArray(4)
    private lateinit var cupTexture: Texture
    private lateinit var cupMesh: TexturedMesh
    var isLeftPressed = false
    var isRightPressed = false

    @Volatile
    private var sourceId = GvrAudioEngine.INVALID_ID
    private lateinit var gvrAudioEngine: GvrAudioEngine
    private val COLLISION_SOUND = "audio/impact.ogg"

    //The lowest coordinate in use on the given table
    private val tableHeight = -3.8f

    //Every Item on the given table
    val items = ArrayList<Item>()

    val playerOne = Player()
    val playerTwo = Player()
    var isMultiPlayer = false

    //Updating the Items on the board, applying gravity, and every movement.
    fun updateBoard() {

        playerOne.grabbedItem?.let {
            if (!isMultiPlayer) {
                //These are the rotating functions for bluetooth controllers' joystick
                if (isLeftPressed)
                    it.rotate(it.angle + 5f)
                if (isRightPressed)
                    it.rotate(it.angle - 5f)
                if (!isRightPressed && !isLeftPressed)
                    it.rotate(it.angle + headRotation[2] * -5f)
            }
            //updating the grabbed Item to the position of the cursor
            it.move(
                Position(
                    playerOne.position[0],
                    -3.0f,
                    playerOne.position[2]
                )
            )
        }
        //If having no grabbed item, but the trigger is pulled, trying to grab Items
        if (playerOne.grabbedItem == null && playerOne.triggered) {
            grab(playerOne)
        }
        //If having a grabbed item, or one that this player is moving,
        //but the trigger is not pulled, then release the Item
        if ((playerOne.grabbedItem != null || playerOne.rotatingItem != null)
            && !playerOne.triggered
        ) {
            release(playerOne, playerTwo)
        }
        if (isMultiPlayer) {
            //If the game is in multiplayer, then sending the player two informations,
            // for the other device
            viewModel?.messenger?.write(
                ("{"
                        + "\"type\": \"status\","
                        + "\"x\": ${playerOne.position[0]},"
                        + "\"z\": ${playerOne.position[2]},"
                        + "\"grabbedid\": ${playerOne.grabbedItem?.id ?: -1},"
                        + "\"triggered\": ${playerOne.triggered}"
                        + "}}*").toByteArray()
            )

            //updating the grabbed Item to the position of the cursor
            playerTwo.grabbedItem?.move(
                Position(
                    playerTwo.position[0],
                    -3.0f,
                    playerTwo.position[2]
                )
            )
            //If having no grabbed item, but the trigger is pulled, trying to grab Items
            //In this case only for rotating, picing up things happens on the other device
            if (playerTwo.grabbedItem == null && playerTwo.triggered) {
                grab(playerTwo)
            }
            //If having a grabbed item, or one that this player is moving,
            //but the trigger is not pulled, then release the Item
            if ((playerTwo.grabbedItem != null || playerTwo.rotatingItem != null)
                && !playerTwo.triggered
            ) {
                release(playerTwo, playerOne)
            }

            //If player one having a rotating item in their hand, rotating it.
            playerOne.rotatingItem?.let {
                rotate(playerOne, playerTwo, it)
            }

            //If player two having a rotating item in their hand, rotating it.
            playerTwo.rotatingItem?.let {
                rotate(playerTwo, playerOne, it)
            }

            //If player two releases the item, but player one holds onto it,
            // then it will be their grabbed piece
            if (playerOne.rotatingItem != null && playerTwo.grabbedItem == null) {
                playerOne.grabbedItem = playerOne.rotatingItem
                playerOne.rotatingItem = null
            }

            //If player one releases the item, but player two holds onto it,
            // then it will be their grabbed piece
            if (playerTwo.rotatingItem != null && playerOne.grabbedItem == null) {
                playerTwo.grabbedItem = playerTwo.rotatingItem
                playerTwo.rotatingItem = null
            }

        }

        //making the Items in the air fall
        activateGravity()
    }

    private fun rotate(player: Player, otherPlayer: Player, item: Item) {
        var rotation =
            Position(player.position).getAngleWithPoint(
                otherPlayer.position[0],
                otherPlayer.position[2]
            )
        if (rotation < 0)
            rotation += 360
        if (player.rotationReference > 0f && !Position(player.position).isNear(
                Position(
                    otherPlayer.position
                ), 0.5f
            )
        ) {
            item.rotate(
                item.angle
                        +
                        rotation - player.rotationReference,
                item
            )
        }
        player.rotationReference = rotation
    }

    private fun activateGravity() {
        items.forEach { item ->
            item.fall(0.05f + item.height * 0.1f)
        }
    }

    //Creating the objects and their textures and meshes on the table.
    fun loadObjects() {
        try {
            viewModel?.interactor?.let { interactor: PuzzleInteractor ->

                //Loading the assets of the puzzle game
                items.addAll(interactor.getPuzzle())

                items.forEach {
                    it.move(Position(0f, -3.2f, 0f))
                    it.rotate(Random.nextInt(0, 361).toFloat())
                    it.drop()
                }

                //Loading the textured Mesh for the trophy
                cupMesh = interactor.getCupMesh()
                //Loading the texture for the trophy
                cupTexture = interactor.getCupTexture()
                //Loading the texture and mesh for the players' cursors.
                playerOne.cursorTexture = interactor.getCursorTexture(true)
                playerOne.cursorTexturedMesh = interactor.getCursorMesh()
                playerTwo.cursorTexture = interactor.getCursorTexture(false)
                playerTwo.cursorTexturedMesh = interactor.getCursorMesh()
            }

        } catch (e: IOException) {
            Log.e(
                "TableTop",
                "Unable to initialize objects",
                e
            )
        }
    }

    private fun grab(player: Player) {
        //Detecting, if the player is this devices own.
        val isPlayerOne = (player == playerOne)
        if (items.size > 0) {
            items.forEach { item ->

                //If an Item is near the cursor of the player they grab it
                if (item.position.isNear(Position(player.position))) {
                    if (isPlayerOne) {
                        //If this device's player, and the item is holded or connected to a holded,
                        //than rotating it
                        if (playerTwo.grabbedItem?.isConnectedTo(item) == true) {
                            player.rotatingItem = playerTwo.grabbedItem
                            return
                        } else {
                            //If this devices player grabs, than let they grab the item
                            if (player.rotatingItem == null &&
                                playerTwo.grabbedItem?.isConnectedTo(
                                    item
                                    //null check
                                ) != true
                            ) {
                                player.grabbedItem = item
                            }
                        }
                    } else {
                        //if the other player, than they can only grab items here for rotation
                        if (playerOne.grabbedItem?.isConnectedTo(item) == true) {
                            player.rotatingItem = item
                            return
                        }
                    }
                }

            }
            //Let the items stacked on the grabbed one fall,
            //and putting the grabbed item to the end of the generating row
            player.grabbedItem?.let {
                val index = items.indexOf(it) + 1

                for (i in index until items.size) {
                    if (items[i].position.isNear(it.position, 1f)) {
                        items[i].drop()
                    }
                }

                it.getConnectedItems(it).forEach { founded ->
                    items.remove(founded)
                    items.add(founded)
                }
                items.remove(it)
                items.add(it)
                it.sync()
            }
        }
    }

    fun calculateFallUntil(item: Item) {
        val index = items.indexOf(item)
        item.fallUntil = tableHeight
        for (i in 0 until index) {
            if (items[i].position.isNear(item.position, 0.9f) &&
                !items[i].isConnectedTo(item) &&
                !items[i].isConnectedTo(playerOne.grabbedItem) &&
                !items[i].isConnectedTo(playerTwo.grabbedItem)
            )
                item.fallUntil = items[i].fallUntil + items[i].height
        }
    }

    //Make a player release an item
    fun release(player: Player, otherPlayer: Player) {
        player.grabbedItem?.drop()
        //The other players item can cover the released one
        if (otherPlayer.grabbedItem != null) {
            putOnTop(otherPlayer.grabbedItem?.id)
        }

        player.grabbedItem = null
        player.rotatingItem = null
    }

    //The puzzle piece which one connected the last one calls this to end the game
    fun done() {
        viewModel?.isDone = true
        val cup = Item(FloatArray(16), cupTexture, cupMesh, 0f, getNextId(), viewModel, 0.75f)
        if (!isMultiPlayer) {
            items.add(cup)
            cup.move(Position(0f, 3f, -3f))
            cup.drop()
        } else {
            val cup2 = Item(FloatArray(16), cupTexture, cupMesh, 0f, getNextId(), viewModel, 0.75f)
            items.add(cup)
            items.add(cup2)
            cup.move(Position(-1f, 3f, -3f))
            cup2.move(Position(1f, 3f, -3f))
            cup.drop()
            cup2.drop()
        }
    }

    private fun getNextId(): Int {
        var nextId = items.size
        var found = false

        while (!found) {
            found = true
            items.forEach { item ->
                if (item.id == nextId)
                    found = false
            }
            nextId++
        }

        return nextId
    }

    fun initializeAudioEngine(VRActivity: VRActivity) {
        gvrAudioEngine =
            GvrAudioEngine(VRActivity, GvrAudioEngine.RenderingMode.BINAURAL_HIGH_QUALITY)
    }

    //To calculate from where we hearing the droping pieces.
    fun setHeadRotation(headRotation: FloatArray) {
        this.headRotation = headRotation
        gvrAudioEngine.setHeadRotation(
            headRotation[0], headRotation[1], headRotation[2], headRotation[3]
        )
        // Regular update call to GVR audio engine.
        gvrAudioEngine.update()
    }

    //Add a colliding sound
    fun collideWithTable(position: Position) {
        Thread {
            gvrAudioEngine.setSoundObjectPosition(
                sourceId, position.x, position.y, position.z
            )

            sourceId = gvrAudioEngine.createStereoSound(COLLISION_SOUND)
            gvrAudioEngine.playSound(sourceId, false)
        }
            .start()
    }

    //loading colliding sound
    fun initSounds() {
        Thread {
            gvrAudioEngine.preloadSoundFile(COLLISION_SOUND)
            sourceId =
                gvrAudioEngine.createSoundObject(COLLISION_SOUND)
        }
            .start()
    }

    //Puting item to the end of generating order
    private fun putOnTop(id: Int?) {
        if (id != null) {
            for (i in 0 until items.size) {
                if (items[i].id == id) {
                    val item = items[i]
                    items.remove(item)
                    items.add(item)
                }
            }
        }
    }

    //grabbing an item from an another class, so we can send data about
    //what the other player is holding.
    fun grabItem(id: Int, player: Player): Item? {
        if (id == -1) {
            return null
        } else {
            items.forEach {
                if (it.id == id && !it.isConnectedTo(playerOne.grabbedItem)) {
                    player.grabbedItem = it
                    if (items.indexOf(it) != items.size - 1) {
                        val index = items.indexOf(it) + 1

                        for (i in index until items.size) {
                            if (items[i].position.isNear(it.position, 1f)) {
                                items[i].drop()
                            }
                        }

                        it.getConnectedItems(it).forEach { founded ->
                            items.remove(founded)
                            items.add(founded)
                        }
                        items.remove(it)
                        items.add(it)

                    }
                    return it
                }
            }
        }
        return null
    }
}
