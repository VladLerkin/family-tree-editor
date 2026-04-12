package com.family.tree.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.core.di.initKoin
import com.family.tree.core.di.platformModule
import com.family.tree.ui.App
import com.family.tree.ui.PermissionRationaleDialog
import com.family.tree.ui.di.uiModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext

class MainActivity : ComponentActivity() {
    private var showPermissionDialog by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("[DEBUG_LOG] MainActivity: RECORD_AUDIO permission granted by user")
        } else {
            println("[DEBUG_LOG] MainActivity: RECORD_AUDIO permission denied by user")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Koin if not already started (e.g. after process death)
        if (GlobalContext.getKoinApplicationOrNull() == null) {
            initKoin(
                additionalModules = listOf(uiModule, platformModule),
                appDeclaration = {
                    androidLogger()
                    androidContext(this@MainActivity)
                }
            )
        }
        
        // Initialize AI settings storage for Android
        AiSettingsStorage.setContext(this)
        
        // Check if we need to show permission rationale dialog
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            println("[DEBUG_LOG] MainActivity: Showing permission rationale dialog")
            showPermissionDialog = true
        } else {
            println("[DEBUG_LOG] MainActivity: RECORD_AUDIO permission already granted")
        }
        
        // Enable edge-to-edge and hide system bars
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            hide(WindowInsetsCompat.Type.statusBars())
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        setContent {
            App()
            
            if (showPermissionDialog) {
                PermissionRationaleDialog(
                    onDismiss = {
                        showPermissionDialog = false
                        println("[DEBUG_LOG] MainActivity: Permission dialog dismissed")
                    },
                    onConfirm = {
                        showPermissionDialog = false
                        println("[DEBUG_LOG] MainActivity: Requesting RECORD_AUDIO permission via Activity Result API")
                        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        }
    }

}
