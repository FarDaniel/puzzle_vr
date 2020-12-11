package hu.szakdolgozat.puzzlevr.puzzle

import hu.szakdolgozat.puzzlevr.texturing.Texture
import hu.szakdolgozat.puzzlevr.texturing.TexturedMesh


class Player {
    //The position of the cursor, it's a FloatArray, because the OpenGl uses this,
    // and this way it doesn't need to be converted in every use.
    var position = FloatArray(4)

    //The Item which the player moves
    var grabbedItem: Item? = null

    //The Item which the player can rotate
    var rotatingItem: Item? = null
        set(item) {
            item?.let {
                //To be sure hte player can't move and rotate different Items at the same time.
                grabbedItem = null
            }
            field = item
        }

    //So the rotating move can be more realistic
    var rotationReference: Float = -1f
    var triggered = false
    lateinit var cursorTexture: Texture
    lateinit var cursorTexturedMesh: TexturedMesh

}