package hu.szakdolgozat.puzzlevr.communication

import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class ClientThread(val viewModel: VRViewModel, inetAddress: InetAddress) : Thread() {
    val socket = Socket()
    private val hostAddress = inetAddress.hostAddress ?: ""

    override fun run() {
        socket.connect(InetSocketAddress(hostAddress, 8888), 2000)
        viewModel.messenger = Messenger(socket, viewModel)

        viewModel.messenger?.start()
    }
}