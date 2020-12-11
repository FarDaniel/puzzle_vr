package hu.szakdolgozat.puzzlevr.ui.menu.peers_menu

import android.content.Intent
import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import android.view.View
import hu.szakdolgozat.puzzlevr.communication.PeersBroadcastReceiver
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.adapter.PeerAdapter
import hu.szakdolgozat.puzzlevr.ui.vr.VRActivity
import java.net.InetAddress

class PeersMenuViewModel(val fragment: PeersMenuFragment) {
    private var groupOwnerAddress: InetAddress? = null
    var peersListener: WifiP2pManager.PeerListListener
    var connectionInfoListener: WifiP2pManager.ConnectionInfoListener
    var peerAdapter = PeerAdapter(fragment.manager, fragment.channel, this)
    val broadcastReceiver = PeersBroadcastReceiver(fragment.manager, fragment.channel, this)
    private var isServer = false
    var isReconnect = false

    init {

        peersListener = WifiP2pManager.PeerListListener { peers ->
            val refreshedPeers = peers.deviceList

            peerAdapter.setItems(refreshedPeers)
            if (peerAdapter.itemCount == 0)
                Log.e("peersMenuViewModel", "no device")
            else
                fragment.binding.textviewSearching.visibility = View.GONE
        }

        connectionInfoListener = WifiP2pManager.ConnectionInfoListener { info ->
            groupOwnerAddress = info?.groupOwnerAddress

            if ((info?.groupFormed) == true && info.isGroupOwner) {
                isServer = true
                this.startVR()
            } else if (info?.groupFormed!!) {
                isServer = false
                Thread.sleep(1000)
                this.startVR()
            }
        }

    }

    //Starting the VR Activity with preloaded data
    fun startVR() {
        fragment.activity?.let {
            val intent = if (isServer) {
                Intent(it, VRActivity::class.java).apply {
                    putExtra("isServer", 1)
                }
            } else {
                Intent(it, VRActivity::class.java).apply {
                    putExtra("isServer", 0)
                    putExtra("address", groupOwnerAddress?.address)
                }
            }
            isReconnect = true
            it.startActivity(intent)
        }
    }
}