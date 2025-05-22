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
import androidx.health.connect.client.records.HeartRateRecord
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

    // Permissões necessárias para acessar os dados de sono
    private val permissions = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(SleepStageRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    private val requestPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grantResults ->
        Log.d(TAG, "Resultado da solicitação de permissões: $grantResults")
        
        val allGranted = grantResults.entries.all { it.value }
        
        if (allGranted) {
            Log.d(TAG, "Todas as permissões foram concedidas")
            lifecycleScope.launch {
                // Verifica novamente para garantir que as permissões foram realmente concedidas
                checkPermissionsAndUpdateState()
            }
        } else {
            Log.w(TAG, "Algumas permissões foram negadas")
            sleepViewModel.onPermissionsDenied()
            
            // Mostra quais permissões foram negadas
            val deniedPermissions = grantResults.filter { !it.value }.keys.joinToString()
            Log.w(TAG, "Permissões negadas: $deniedPermissions")
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
    
    /**
     * Verifica as permissões atuais e solicita as permissões necessárias, se ainda não concedidas.
     */
    private suspend fun checkAndRequestPermissions() {
        try {
            Log.d(TAG, "Verificando permissões do Health Connect...")
            
            // Verifica se o Health Connect está disponível
            val availabilityStatus = HealthConnectClient.getSdkStatus(this, PROVIDER_PACKAGE_NAME)
            if (availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
                Log.w(TAG, "Health Connect não está disponível. Status: $availabilityStatus")
                sleepViewModel.onPermissionsDenied()
                return
            }
            
            // Obtém as permissões concedidas
            val granted = try {
                healthConnectClient.permissionController.getGrantedPermissions()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao obter permissões concedidas", e)
                emptySet()
            }
            
            Log.d(TAG, "Permissões concedidas: $granted")
            Log.d(TAG, "Permissões necessárias: $permissions")
            
            // Verifica se todas as permissões necessárias foram concedidas
            val hasAllPermissions = permissions.all { granted.contains(it) }
            
            if (hasAllPermissions) {
                Log.d(TAG, "Todas as permissões necessárias foram concedidas")
                sleepViewModel.onPermissionsGranted()
            } else {
                Log.w(TAG, "Solicitando permissões faltantes...")
                // Filtra apenas as permissões que ainda não foram concedidas
                val missingPermissions = permissions.filterNot { granted.contains(it) }
                Log.d(TAG, "Permissões faltantes: $missingPermissions")
                
                // Solicita as permissões faltantes
                requestPermissions.launch(missingPermissions.map { it.toString() }.toTypedArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar/solicitar permissões", e)
            sleepViewModel.onPermissionsDenied()
            // Tenta abrir as configurações do Health Connect como último recurso
            openHealthConnectSettings()
        }
    }
    
    /**
     * Abre as configurações do Health Connect para que o usuário possa gerenciar as permissões manualmente.
     */
    private fun openHealthConnectSettings() {
        try {
            Log.d(TAG, "Abrindo configurações do Health Connect...")
            val intent = Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            Log.d(TAG, "Configurações do Health Connect abertas com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao abrir as configurações do Health Connect", e)
            
            // Tenta abrir a página do Health Connect na Play Store como fallback
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    setPackage("com.android.vending") // Abre diretamente na Play Store
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Erro ao abrir a Play Store", e2)
                sleepViewModel.onPermissionsDenied()
            }
        }
    }
}