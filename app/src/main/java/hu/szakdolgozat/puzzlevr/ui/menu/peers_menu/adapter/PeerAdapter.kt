package hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import hu.szakdolgozat.puzzlevr.databinding.ItemPotencialPeerBinding
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.Peer
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.PeersMenuViewModel
import hu.szakdolgozat.puzzlevr.ui.menu.peers_menu.viewholder.PeerViewHolder

class PeerAdapter(
    private val manager: WifiP2pManager,
    private val channel: WifiP2pManager.Channel,
    val viewModel: PeersMenuViewModel
) : RecyclerView.Adapter<PeerViewHolder>() {
    private val items: ArrayList<Peer> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ItemPotencialPeerBinding.inflate(layoutInflater, parent, false)
        return PeerViewHolder(binding, manager, channel)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Setting Items not in use, later it can be useful
    fun setItems(items: List<Peer>) {
        this.items.clear()
        items.forEach { item ->
            this.items.add(item)
        }
    }

    //Setting items from devices
    fun setItems(items: Collection<WifiP2pDevice>) {
        this.items.clear()
        items.forEach { item ->
            this.items.add(Peer(item, viewModel))
        }
        notifyDataSetChanged()
    }
}