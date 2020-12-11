package hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.viewholder

import android.net.wifi.p2p.WifiP2pManager
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import hu.szakdolgozat.puzzlevr.databinding.ItemPotencialPeerBinding
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.Peer

class PeerViewHolder(
    private val binding: ItemPotencialPeerBinding,
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel
) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(peer: Peer) {
        binding.textviewName.text = peer.device.deviceName

        binding.executePendingBindings()

        initListener(peer)
    }

    private fun initListener(peer: Peer) {
        //Connecting to the Peer, if the user taps on the listItem
        itemView.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            binding.textviewConnection.visibility = View.VISIBLE
            if (!peer.connect(manager, channel)) {
                binding.progressBar.visibility = View.INVISIBLE
            }

        }
    }

}