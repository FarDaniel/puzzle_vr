package hu.szakdolgozat.puzzlevr.ui.menu.info_panel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import hu.szakdolgozat.puzzlevr.R
import hu.szakdolgozat.puzzlevr.databinding.FragmentInfoPanelBinding

class InfoPanelFragment : Fragment() {

    private lateinit var navController: NavController
    lateinit var binding: FragmentInfoPanelBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentInfoPanelBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)
        binding.constraintlayoutInfopanel.setOnClickListener {
            next()
        }
    }

    fun next() {
        //Navigating to next fragment
        navController.navigate(R.id.action_info_panel_to_mainMenuFragment)
    }

}
