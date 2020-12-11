package hu.szakdolgozat.puzzlevr.ui.menu.main_menu

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import hu.szakdolgozat.puzzlevr.databinding.FragmentMainMenuBinding
import com.gun0912.tedpermission.PermissionListener
import com.gun0912.tedpermission.TedPermission
import hu.szakdolgozat.puzzlevr.R
import hu.szakdolgozat.puzzlevr.ui.vr.VRActivity


class MainMenuViewModel(
    private val activity: FragmentActivity?,
    private val navController: NavController,
    val binding: FragmentMainMenuBinding
) {
    private var wifiManager =
        activity?.applicationContext?.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private var locationManager =
        activity?.applicationContext?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val isMultiPlayerReady: Boolean
        get() {
            return wifiManager.isWifiEnabled && locationManager.isLocationEnabled
        }

    //Starting the VR game in single player
    fun startGame() {
        binding.progressBarMain.visibility = View.VISIBLE
        activity?.let {
            val intent = Intent(it, VRActivity::class.java).apply {
                putExtra("isServer", -1)
            }
            it.startActivity(intent)
        }
    }

    fun searchForPeers() {
        //checking if the permissions are granted or not.If they are,
        // than starting the activity,
        // if they aren't the notify the user abaut it.
        activity?.let {
            if (ActivityCompat.checkSelfPermission(
                    it.applicationContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                //Having permission for accessing fine location, checking for other permissions
                if (isMultiPlayerReady) {
                    binding.progressBarMain.visibility = View.VISIBLE
                    navController.navigate(R.id.action_mainMenuFragment_to_peersMenuFragment)
                } else {
                    binding.popupWarning.visibility = View.VISIBLE

                    if (!wifiManager.isWifiEnabled) {
                        //Wifi is not enabled
                        binding.buttonWifi.visibility = View.VISIBLE
                        binding.textviewWarning.text =
                            activity.resources.getString(R.string.enable_Wifi)
                    } else
                        binding.buttonWifi.visibility = View.GONE

                    if (!locationManager.isLocationEnabled) {
                        //location is not enabled
                        binding.buttonLocation.visibility = View.VISIBLE
                        if (!wifiManager.isWifiEnabled) {
                            //Location and wifi are not enabled
                            binding.textviewWarning.text =
                                activity.resources.getString(R.string.enable_Wifi_and_location)
                        } else {
                            //Only the location is not enabled
                            binding.textviewWarning.text =
                                activity.resources.getString(R.string.enable_location)
                        }
                    } else {
                        binding.buttonLocation.visibility = View.GONE
                    }
                }
            } else {
                //No permission for accessing fine location
                requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    //Asking for missing permission
    fun requestPermission(permission: String?) {

        val permissionListener = object : PermissionListener {
            override fun onPermissionGranted() {
            }

            override fun onPermissionDenied(deniedPermissions: MutableList<String>?) {
            }

        }

        TedPermission.with(activity)
            .setPermissionListener(permissionListener)
            .setPermissions(permission)
            .check()
    }

    //Loading info panel fragment
    fun openInfoScreen() {
        navController.navigate(R.id.action_mainMenuFragment_to_info_panel)
    }

}
