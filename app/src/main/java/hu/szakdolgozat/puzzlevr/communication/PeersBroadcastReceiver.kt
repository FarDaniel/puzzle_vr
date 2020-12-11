package hu.szakdolgozat.puzzlevr.communication

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import android.view.View
import hu.szakdolgozat.puzzlevr.R
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.PeersMenuViewModel

class PeersBroadcastReceiver(
    private var manager: WifiP2pManager,
    private var channel: WifiP2pManager.Channel,
    private var viewModel: PeersMenuViewModel
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel, viewModel.peersListener)
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val info = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (info?.isConnected == true) {
                    manager.requestConnectionInfo(channel, viewModel.connectionInfoListener)
                } else {
                    viewModel.fragment.binding.textviewSearching.visibility = View.VISIBLE
                    viewModel.fragment.binding.textviewSearching.text = context.getString(R.string.disconnected)
                    viewModel.fragment.binding.progressbarSearching.visibility = View.GONE
                }
            }
        }

    }
}