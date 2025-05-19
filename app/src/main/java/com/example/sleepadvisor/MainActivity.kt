package com.example.sleepadvisor

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SleepStageRecord
import androidx.lifecycle.lifecycleScope
import com.example.sleepadvisor.presentation.navigation.AppNavigation
import com.example.sleepadvisor.presentation.screens.sleep.SleepScreen
import com.example.sleepadvisor.presentation.screens.sleep.SleepViewModel
import com.example.sleepadvisor.presentation.viewmodel.SleepAnalysisViewModel
import com.example.sleepadvisor.ui.theme.SleepAdvisorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PROVIDER_PACKAGE_NAME = "com.google.android.apps.healthdata"
    }
    
    @Inject
    lateinit var healthConnectClient: HealthConnectClient
    
    private val sleepViewModel: SleepViewModel by viewModels()
    private val sleepAnalysisViewModel: SleepAnalysisViewModel by viewModels()

    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(SleepStageRecord::class)
    )

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        if (grantResults.values.all { it }) {
            Log.d(TAG, "All permissions granted")
            sleepViewModel.onPermissionsGranted()
        } else {
            Log.d(TAG, "Some permissions denied")
            sleepViewModel.onPermissionsDenied()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val availabilityStatus = HealthConnectClient.getSdkStatus(this, PROVIDER_PACKAGE_NAME)
        when (availabilityStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                sleepViewModel.onPermissionsDenied()
                return
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                sleepViewModel.onPermissionsDenied()
                val uriString = "market://details?id=$PROVIDER_PACKAGE_NAME&url=healthconnect%3A%2F%2Fonboarding"
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    setPackage("com.android.vending")
                    data = android.net.Uri.parse(uriString)
                    putExtra("overlay", true)
                    putExtra("callerId", packageName)
                })
                return
            }
        }

        setContent {
            SleepAdvisorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = sleepViewModel,
                        sleepAnalysisViewModel = sleepAnalysisViewModel
                    )
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            checkPermissionsAndUpdateState()
        }
    }
    
    private suspend fun checkPermissionsAndUpdateState() {
        try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(permissions)) {
                sleepViewModel.onPermissionsGranted()
            } else {
                sleepViewModel.onPermissionsDenied()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            sleepViewModel.onPermissionsDenied()
        }
    }
    
    private suspend fun checkAndRequestPermissions() {
        try {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            
            if (granted.containsAll(permissions)) {
                sleepViewModel.onPermissionsGranted()
            } else {
                requestPermissions.launch(permissions.map { it.toString() }.toTypedArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking permissions", e)
            openHealthConnectSettings()
        }
    }
    
    private fun openHealthConnectSettings() {
        try {
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
            startActivity(intent)
            Log.d(TAG, "Opened Health Connect settings")
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Health Connect settings", e)
            sleepViewModel.onPermissionsDenied()
        }
    }
}