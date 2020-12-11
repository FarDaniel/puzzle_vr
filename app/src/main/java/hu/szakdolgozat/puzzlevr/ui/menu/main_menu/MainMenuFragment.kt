package hu.szakdolgozat.puzzlevr.ui.menu.main_menu

import android.Manifest
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import hu.szakdolgozat.puzzlevr.databinding.FragmentMainMenuBinding
import hu.szakdolgozat.puzzlevr.R


class MainMenuFragment : Fragment() {

    lateinit var binding: FragmentMainMenuBinding
    lateinit var viewModel: MainMenuViewModel
    private lateinit var navController: NavController
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            prefs = it.getSharedPreferences("hu.szakdolgozat.puzzlevr", MODE_PRIVATE)
        }
        navController = Navigation.findNavController(view)
        viewModel = MainMenuViewModel(activity, navController, binding)
        binding.viewModel = viewModel
        activity?.let {
            //Checking permission for reading external storage.
            if (ActivityCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                //Requesting the permission if not granted
                viewModel.requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        //Checking if it's the first start since installing.
        //In that case opening info panel
        if (prefs.getBoolean("firstrun", true)) {

            prefs.edit().putBoolean("firstrun", false).apply()
            navController.navigate(R.id.action_mainMenuFragment_to_info_panel)
        }
        viewModel.binding.progressBarMain.visibility = View.GONE
    }
}