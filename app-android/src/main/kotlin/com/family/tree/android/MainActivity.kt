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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.family.tree.core.ai.AiSettingsStorage
import com.family.tree.ui.App
import com.family.tree.ui.PermissionRationaleDialog

class MainActivity : ComponentActivity() {
    private var showPermissionDialog by mutableStateOf(false)
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
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
                        println("[DEBUG_LOG] MainActivity: Requesting RECORD_AUDIO permission")
                        ActivityCompat.requestPermissions(
                            this,
                            arrayOf(Manifest.permission.RECORD_AUDIO),
                            PERMISSION_REQUEST_CODE
                        )
                    }
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                println("[DEBUG_LOG] MainActivity: RECORD_AUDIO permission granted by user")
            } else {
                println("[DEBUG_LOG] MainActivity: RECORD_AUDIO permission denied by user")
            }
        }
    }
}
