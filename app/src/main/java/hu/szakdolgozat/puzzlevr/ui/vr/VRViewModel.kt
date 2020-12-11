package hu.szakdolgozat.puzzlevr.ui.vr

import android.os.Handler
import android.os.Looper
import hu.szakdolgozat.puzzlevr.communication.ClientThread
import hu.szakdolgozat.puzzlevr.communication.InGameBroadcastReceiver
import hu.szakdolgozat.puzzlevr.communication.Messenger
import hu.szakdolgozat.puzzlevr.communication.ServerThread
import hu.szakdolgozat.puzzlevr.interactor.PuzzleInteractor
import hu.szakdolgozat.puzzlevr.puzzle.TableTop
import org.json.JSONObject
import java.net.InetAddress

class VRViewModel(isServer: Int, addressIfOwner: ByteArray?) {
    var messenger: Messenger? = null
    lateinit var tableTop: TableTop
    private var server: ServerThread? = null
    private var client: ClientThread? = null
    var isDone = false
    lateinit var interactor: PuzzleInteractor
    val broadcastReceiver = InGameBroadcastReceiver(this)

    //Handling messages
    val handler = Handler(Looper.getMainLooper()) {
        //If it's a message
        if (it.what == 1) {
            val buffer = it.obj as ByteArray
            val message = String(buffer, 0, it.arg1)
            //Splitting the buffer to get every message
            val arrivedJSONs = message.split('*')
            arrivedJSONs.forEach { rawData ->
                try {
                    //Splitting the message to get every information
                    val data = JSONObject(rawData)
                    when (data.getString("type")) {
                        //The status of the other player
                        "status" -> {
                            tableTop.playerTwo.position[0] = data.getDouble("x").toFloat()
                            tableTop.playerTwo.position[2] = data.getDouble("z").toFloat()
                            if (tableTop.playerTwo.grabbedItem == null) {
                                val id = data.getInt("grabbedid")
                                tableTop.grabItem(id, tableTop.playerTwo)
                            }
                            tableTop.playerTwo.triggered = data.getBoolean("triggered")
                        }
                        //Updated place of Item
                        "sync" -> {
                            tableTop.items.forEach { item ->
                                if (item.id == data.getInt("id")) {
                                    item.position.x = data.getDouble("x").toFloat()
                                    item.position.z = data.getDouble("z").toFloat()
                                    item.angle = data.getDouble("angle").toFloat()
                                }
                            }
                        }

                        //The other player disconnected
                        "disconnect" -> {
                            server?.socket?.close()
                            client?.socket?.close()
                            tableTop.isMultiPlayer = false
                        }
                    }
                } catch (exception: Exception) {

                }
            }
        }
        return@Handler true
    }

    init {
        //Starting the communication thread if needed
        when (isServer) {
            1 -> {
                server = ServerThread(this)
                server?.start()
            }
            0 -> {
                client = ClientThread(this, InetAddress.getByAddress(addressIfOwner))
                client?.start()
            }
        }
        tableTop = TableTop(this)

    }

    //Ending communication
    fun closeCommunication() {
        messenger?.write("{\"type:\" \"disconnect\"".toByteArray())
        server?.socket?.close()
        client?.socket?.close()
        tableTop.isMultiPlayer = false
    }

    fun loadObjects(activity: VRActivity) {
        interactor = PuzzleInteractor(activity, this)
        tableTop.loadObjects()
    }

}