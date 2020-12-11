package hu.szakdolgozat.puzzlevr.puzzle

import android.opengl.Matrix
import hu.szakdolgozat.puzzlevr.texturing.Texture
import hu.szakdolgozat.puzzlevr.texturing.TexturedMesh
import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel

class Item(
    val target: FloatArray,
    var texture: Texture,
    val mesh: TexturedMesh,
    private val rotationOffset: Float,
    val id: Int,
    private val viewModel: VRViewModel?,
    val height: Float,
    var neighborItems: ArrayList<NeighborItem> = ArrayList()
) {

    var position = Position()
    var angle = 0f
    var fallUntil = -3.8f
    private var wasFalling = true
    private var isFalling = true

    fun move(position: Position, from: Item = this) {
        this.position = position
        Matrix.setIdentityM(target, 0)
        Matrix.translateM(target, 0, this.position.x, this.position.y, this.position.z)
        Matrix.rotateM(target, 0, -angle + rotationOffset, 0f, 1f, 0f)

        for (i in 0 until neighborItems.size)
            if (neighborItems[i].found && neighborItems[i].ownItem != from)
                neighborItems[i].move()
    }

    fun fall(distance: Float) {
        if (isFalling) {
            val y = position.y - distance

            if (y > fallUntil) {
                position.y = y
            } else {
                if (wasFalling)
                    viewModel?.tableTop?.collideWithTable(this.position)
                position.y = fallUntil
                isFalling = false
                stopFalling()
            }
            Matrix.setIdentityM(target, 0)
            Matrix.translateM(target, 0, position.x, position.y, position.z)
            Matrix.rotateM(target, 0, -angle + rotationOffset, 0f, 1f, 0f)
        }
        wasFalling = isFalling
    }

    //Used when connected items falling together
    fun stopFalling(from: Item = this) {
        fallUntil = position.y
        isFalling = false

        for (i in 0 until neighborItems.size)
            if (neighborItems[i].found && neighborItems[i].ownItem != from) {
                neighborItems[i].stopFalling()
            }
    }

    fun addNeighborItem(pos: Position, item: Item) {
        item.neighborItems.add(NeighborItem(pos.reverseOnSurface(), item, this))
        neighborItems.add(NeighborItem(pos, this, item))
    }

    fun connect(item: Item) {
        //Searching out this item from the other item's wanteds,
        // it's a function which is called when a wanted item made a one way connection,
        // so it can be a two way one.
        for (i in 0 until item.neighborItems.size) {
            if (item.neighborItems[i].ownItem == this)
                item.neighborItems[i].found = true
        }

        //if all the items are connected, the puzzle is done
        if (mergeWantedItems(item))
            viewModel?.tableTop?.done()
    }

    fun drop(from: Item = this) {
        sync(this)
        viewModel?.tableTop?.calculateFallUntil(this)
        isFalling = (fallUntil != position.y)

        var i = 0
        val size = neighborItems.size
        //droping all the neighbor items of this item (the neighborItem class handels it)
        while (i < size) {
            if (neighborItems[i].ownItem != from) {
                neighborItems[i].drop()
                //While we drop the neighborItems, it's possible that some will be deleted.
                //In this case we can restart the droping process
                if (size != neighborItems.size) {
                    return
                }
            }
            i++
        }

    }

    private fun mergeWantedItems(with: Item): Boolean {
        // Checking, if we have cycles
        val neighborItemsOfGroup = with.takeWantedItemsOfGroup(this)
        val neighborItemsOfOwnGroup = this.takeWantedItemsOfGroup(with)

        //Checking every Item's NeighborItem in both groups
        var i = 0
        while (i < neighborItemsOfGroup.size) {
            var j = 0
            while (j < neighborItemsOfOwnGroup.size) {
                if (neighborItemsOfGroup[i].ownItem == neighborItemsOfOwnGroup[j].ownItem ||
                    neighborItemsOfGroup[i].ownItem == neighborItemsOfOwnGroup[j].owner
                ) {
                    neighborItemsOfGroup[i].delete()
                    drop()
                }
                j++
            }
            i++
        }
        //Checking if we have any not connected Item left, for what we're waiting for.
        val unitedGroup = takeWantedItemsOfGroup()
        unitedGroup.forEach {
            if (!it.found)
                return false
        }
        return true
    }

    private fun takeWantedItemsOfGroup(from: Item = this): ArrayList<NeighborItem> {
        //recursively getting all the neighbor items, of a group
        val out = ArrayList<NeighborItem>()

        neighborItems.forEach {
            if (!it.found) {
                out.add(it)
            } else {
                if (it.ownItem != from)
                    out.addAll(it.ownItem.takeWantedItemsOfGroup(this))
            }
        }
        return out
    }

    fun rotate(angle: Float, from: Item = this) {
        this.angle = angle

        //All the Items connected to a rotated one, have to be rotated to the exact same angle
        for (i in 0 until neighborItems.size)
            if (neighborItems[i].found && neighborItems[i].ownItem != from) {
                neighborItems[i].rotate(angle)
            }
    }

    fun isConnectedTo(item: Item?): Boolean {
        //checking if this Item and the checked Item is connected, or not.
        return if (item != null) {
            var connected = (this.id == item.id)
            val connecteds = getConnectedItems(this)
            connecteds.forEach {
                if (it.id == item.id)
                    connected = true
            }
            connected
        } else
            false
    }

    fun getConnectedItems(from: Item): ArrayList<Item> {
        //Recursive function, to get every piece that is connected to this one.
        val foundedItems = ArrayList<Item>()

        for (i in 0 until neighborItems.size) {
            if (neighborItems[i].found && neighborItems[i].ownItem != from) {
                foundedItems.add(neighborItems[i].ownItem)
                foundedItems.addAll(neighborItems[i].getConnectedItems())
            }
        }
        return foundedItems
    }

    fun sync(from: Item = this) {
        if (viewModel?.tableTop?.isMultiPlayer == true) {
            //Sending out a massage to the other player abaut an items exact location.
            viewModel.messenger?.write(
                ("{"
                        + "\"type\": \"sync\","
                        + "\"id\": ${id},"
                        + "\"x\": ${position.x},"
                        + "\"z\": ${position.z},"
                        + "\"angle\": $angle"
                        + "}}*").toByteArray()
            )

            //repeating this for every connected Item too
            for (i in 0 until neighborItems.size)
                if (neighborItems[i].found && neighborItems[i].ownItem != from) {
                    neighborItems[i].sync()
                }

        }
    }
}