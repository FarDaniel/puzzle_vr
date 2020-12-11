package hu.szakdolgozat.puzzlevr.ui.menu.peers_menu

import android.annotation.SuppressLint
import android.content.IntentFilter
import android.net.wifi.p2p.WifiP2pManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hu.szakdolgozat.puzzlevr.R
import hu.szakdolgozat.puzzlevr.databinding.FragmentPeersMenuBinding


class PeersMenuFragment : Fragment() {
    lateinit var binding: FragmentPeersMenuBinding
    private lateinit var viewModel: PeersMenuViewModel

    private val intentFilter = IntentFilter()
    lateinit var manager: WifiP2pManager
    lateinit var channel: WifiP2pManager.Channel
    private lateinit var recyclerView: RecyclerView

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        manager = activity?.getSystemService(AppCompatActivity.WIFI_P2P_SERVICE) as WifiP2pManager
        channel = manager.initialize(activity, activity?.mainLooper, null)
        viewModel = PeersMenuViewModel(this)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)


        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {

            override fun onSuccess() {
                binding.textviewSearching.text = getString(R.string.searching_for_players)
            }

            override fun onFailure(reasonCode: Int) {
                binding.textviewSearching.text = getString(R.string.searching_failed)
                binding.progressbarSearching.visibility = View.GONE
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPeersMenuBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        activity?.registerReceiver(viewModel.broadcastReceiver, intentFilter)
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(viewModel.broadcastReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.recyclerviewPeers
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.adapter = viewModel.peerAdapter
    }

}