package hu.szakdolgozat.puzzlevr.interactor

import hu.szakdolgozat.puzzlevr.puzzle.Item
import hu.szakdolgozat.puzzlevr.puzzle.Position
import hu.szakdolgozat.puzzlevr.texturing.Texture
import hu.szakdolgozat.puzzlevr.texturing.TexturedMesh
import hu.szakdolgozat.puzzlevr.ui.vr.VRActivity
import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel
import org.json.JSONArray
import org.json.JSONObject

class PuzzleInteractor(private val activity: VRActivity, val viewModel: VRViewModel?) {
    private val puzzleMeshes = ArrayList<TexturedMesh>()
    private var pieceHeight = 0.05f

    init {
        //Rectangular
        puzzleMeshes.add(
            TexturedMesh(
                activity,
                "objects/karpit_rectangle.obj",
                activity.objectPositionParam,
                activity.objectUvParam
            )
        )

        //triangular
        puzzleMeshes.add(
            TexturedMesh(
                activity,
                "objects/karpit_triangular.obj",
                activity.objectPositionParam,
                activity.objectUvParam
            )
        )
    }

    fun getPuzzle(): ArrayList<Item> {
        val out = ArrayList<Item>()
        val puzzleData = JSONObject(
            activity.assets.open("puzzle.json")
                .bufferedReader().readText()
        ).getJSONObject("puzzle")
        pieceHeight = puzzleData.getDouble("pieceheight").toFloat()
        val pieces = puzzleData.getJSONArray("pieces")
        val neighbors = puzzleData.getJSONArray("neighbors")

        out.addAll(getPieces(pieces))

        for (i in 0 until neighbors.length()) {
            val ownerId = neighbors.getJSONObject(i).getInt("onwerid")
            val ownItemId = neighbors.getJSONObject(i).getInt("ownid")
            val relativePos = neighbors.getJSONObject(i).getString("relativepos")
            out.forEach { owner ->
                if (owner.id == ownerId) {
                    out.forEach { ownItem ->
                        if (ownItem.id == ownItemId)
                            owner.addNeighborItem(getRelativePosition(relativePos), ownItem)
                    }
                }
            }
        }
        return out
    }

    fun getCupMesh(): TexturedMesh {
        return TexturedMesh(
            activity,
            "objects/cup.obj",
            activity.objectPositionParam,
            activity.objectUvParam
        )
    }

    fun getCupTexture(): Texture {
        return Texture(activity, "textures/cup_bake.png")
    }

    fun getCursorTexture(isOwn: Boolean): Texture {
        return if (isOwn) {
            Texture(activity, "textures/P1Texture.png")
        } else {
            Texture(activity, "textures/P2Texture.png")
        }
    }

    fun getCursorMesh(): TexturedMesh {
        return TexturedMesh(
            activity,
            "objects/cursore.obj",
            activity.objectPositionParam,
            activity.objectUvParam
        )
    }

    private fun getPieces(piecesJSON: JSONArray): ArrayList<Item> {
        val out = ArrayList<Item>()
        for (i in 0 until piecesJSON.length()) {
            val newTarget = FloatArray(16)
            val texture = Texture(activity, piecesJSON.getJSONObject(i).getString("texture"))
            var mesh: TexturedMesh = puzzleMeshes[0]
            val offset = piecesJSON.getJSONObject(i).getDouble("offset").toFloat()
            val id = piecesJSON.getJSONObject(i).getInt("id")
            when (piecesJSON.getJSONObject(i).getString("form")) {
                "rect" -> {
                    mesh = puzzleMeshes[0]
                }
                "tri" -> {
                    mesh = puzzleMeshes[1]
                }
            }
            val newItem =
                Item(newTarget, texture, mesh, offset, id, viewModel, pieceHeight)
            out.add(
                newItem
            )
        }
        return out
    }

    private fun getRelativePosition(direction: String): Position {
        return when (direction) {
            "left" -> {
                Position.getDirectionVector(Position.Direction.LEFT, 1f)
            }
            "right" -> {
                Position.getDirectionVector(Position.Direction.RIGHT, 1f)
            }
            "down" -> {
                Position.getDirectionVector(Position.Direction.DOWN, 1f)
            }
            "up" -> {
                Position.getDirectionVector(Position.Direction.UP, 1f)
            }
            else -> {
                val cordinates = direction.split(";")
                Position(cordinates[0].toFloat(), cordinates[1].toFloat(), cordinates[2].toFloat())
            }
        }
    }

}