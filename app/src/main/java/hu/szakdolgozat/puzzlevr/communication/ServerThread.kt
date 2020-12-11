package hu.szakdolgozat.puzzlevr.communication

import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class ServerThread(var viewModel: VRViewModel) : Thread() {
    private var serverSocket = ServerSocket()
    var socket: Socket? = null

    init {
        serverSocket.reuseAddress = true
        serverSocket.bind(InetSocketAddress(8888))
    }

    override fun run() {
        socket = serverSocket.accept()
        socket?.let { socket ->
            viewModel.messenger = Messenger(socket, viewModel)
        }
        viewModel.messenger?.start()
    }
}