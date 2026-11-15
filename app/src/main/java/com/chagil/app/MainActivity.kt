package com.chagil.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.chagil.app.core.AppContainer
import com.chagil.app.ui.AppViewModel
import com.chagil.app.ui.DebugScenes
import com.chagil.app.ui.screens.ChagilApp

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Idempotent. The hub keeps running while hidden so a drill paused on D can offer "이어서 할까요?".
        AppContainer.get(this).start()
        setContent { ChagilApp(vm) }
        if (savedInstanceState == null) handleDebugIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDebugIntent(intent)
    }

    /** Screenshot / rehearsal scenes. Debug builds only: release ignores these extras entirely. */
    private fun handleDebugIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        DebugScenes.run(vm, intent.getStringExtra("screen"), intent.getStringExtra("theme"))
    }
}
