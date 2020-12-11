package hu.szakdolgozat.puzzlevr.puzzle

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class NeighborItem(private val relativePosition: Position, val owner: Item, val ownItem: Item) {
    private var actualPosition = Position()
    var found = false

    private fun check() {
        //Checking if the two items needs to connect
        countPosition()
        if (ownItem.position.isNear(actualPosition) && abs(
                sin(Math.toRadians((owner.angle).toDouble())) - sin(
                    Math.toRadians((ownItem.angle).toDouble())
                )
            ) < 0.2
        ) {
            found = true
            owner.connect(ownItem)
            //Moving the owner to the other item "popping" them together
            ownItem.rotate(ownItem.angle)
            ownItem.move(ownItem.position)
            ownItem.sync()
            owner.sync()
        }
    }

    private fun countPosition() {
        // Calculating the exact position, where should the item be
        // (if the owner is rotated, we need to rotate the item relative to the owners pivot point)
        val tempPos = Position(
            ((relativePosition.x) * cos(Math.toRadians(owner.angle.toDouble()))
                    - ((relativePosition.z) * sin(Math.toRadians(owner.angle.toDouble())))).toFloat(),
            relativePosition.y + owner.position.y,
            ((relativePosition.z) * cos(Math.toRadians(owner.angle.toDouble()))
                    + ((relativePosition.x) * sin(Math.toRadians(owner.angle.toDouble())))).toFloat()
        )
        actualPosition = owner.position.addOnSurface(tempPos)
    }

    fun move() {
        countPosition()
        ownItem.move(actualPosition, owner)

    }

    fun drop() {
        if (!found) {
            //checking if it should be found or not, in every drop if it isn't found yet
            check()
        } else {
            // dropping all of it's NeighborItems
            ownItem.drop(owner)
        }
    }

    fun delete() {
        //We know which Item has this NeighborItem, so we can delete it, by itself,
        // and itt will be removed, from everywhere
        owner.neighborItems.remove(this)
        val iterator = ownItem.neighborItems.iterator()
        for (wp in iterator)
            if (wp.ownItem == owner)
                iterator.remove()
    }

    fun rotate(angle: Float) {
        ownItem.rotate(angle, owner)
    }

    fun getConnectedItems(): Collection<Item> {
        return ownItem.getConnectedItems(owner)
    }

    fun sync() {
        ownItem.sync(owner)
    }

    fun stopFalling() {
        ownItem.stopFalling(owner)
    }

}