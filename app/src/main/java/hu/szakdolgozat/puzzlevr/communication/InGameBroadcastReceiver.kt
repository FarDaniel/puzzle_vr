package hu.szakdolgozat.puzzlevr.communication

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pManager
import hu.szakdolgozat.puzzlevr.ui.vr.VRViewModel

class InGameBroadcastReceiver(val viewModel: VRViewModel) : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)

                if (state != WifiP2pManager.WIFI_P2P_STATE_ENABLED){
                    //Disconnecting the communication on each side
                    //closing the socket
                    viewModel.closeCommunication()
                }
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val info = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                if (info?.isConnected == false) {
                    //Disconnecting the communication on each side
                    //closing the socket
                    viewModel.closeCommunication()
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
            }
        }

    }
}