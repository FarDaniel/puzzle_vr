package hu.szakdolgozat.puzzlevr.ui.menu.peers_menu

import android.annotation.SuppressLint
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager

class Peer(val device: WifiP2pDevice, val viewModel: PeersMenuViewModel) {

    @SuppressLint("MissingPermission")
    fun connect(manager: WifiP2pManager, channel: WifiP2pManager.Channel): Boolean {
        val config = WifiP2pConfig()
        config.deviceAddress = device.deviceAddress

        var success = true


        //loading the VR Activity if the connection is live
        if (viewModel.isReconnect)
            viewModel.startVR()
        else
            manager.connect(channel, config, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    success = true
                }

                override fun onFailure(reason: Int) {
                    success = false
                }
            })

        return success
    }
}