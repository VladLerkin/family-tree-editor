package com.family.tree.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun ExitAppAction(onExit: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        onExit()
        (context as? Activity)?.finish()
    }
}
