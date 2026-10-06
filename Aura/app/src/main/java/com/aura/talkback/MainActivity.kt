package com.aura.talkback

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.aura.talkback.ui.AppRoot
import com.aura.talkback.ui.MainViewModel
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.rememberFrameClock
import com.aura.talkback.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        requestHighestRefreshRate()
        setContent {
            AuraTheme {
                CompositionLocalProvider(LocalFrameClock provides rememberFrameClock()) {
                    AppRoot(vm)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    override fun onPause() {
        vm.stopVoice()
        super.onPause()
    }

    /** Ask for the panel's fastest mode at the current resolution (144 Hz on the GT 30 Pro). */
    @Suppress("DEPRECATION")
    private fun requestHighestRefreshRate() {
        val display = (if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay) ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.also {
            it.preferredDisplayModeId = best.modeId
            it.preferredRefreshRate = best.refreshRate
        }
    }
}
