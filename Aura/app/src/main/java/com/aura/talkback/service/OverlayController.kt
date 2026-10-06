package com.aura.talkback.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Display
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.motion.rememberFrameClock
import com.aura.talkback.ui.theme.AuraTheme

/** Lifecycle plumbing so Compose can run inside a window owned by a Service. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    fun start() {
        savedState.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

/** Root that turns the system Back key into a callback. */
private class BackAwareFrame(context: Context, private val onBack: () -> Unit) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) onBack()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

/**
 * Shows one full-screen Compose overlay at a time, drawn above every app via
 * TYPE_ACCESSIBILITY_OVERLAY (no "draw over other apps" permission needed).
 * Exit animations run in Compose; the window is removed once they finish.
 */
class OverlayController(private val context: Context) {
    private val wm = context.getSystemService(WindowManager::class.java)
    private var root: View? = null
    private var owner: OverlayOwner? = null
    private var visible: MutableTransitionState<Boolean>? = null

    val isShowing get() = root != null

    fun show(onBack: () -> Unit, content: @Composable (visible: MutableTransitionState<Boolean>, onGone: () -> Unit) -> Unit) {
        removeNow()
        val state = MutableTransitionState(false).apply { targetState = true }
        val lifecycleOwner = OverlayOwner().also { it.start() }
        val frame = BackAwareFrame(context, onBack)
        val compose = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                AuraTheme {
                    CompositionLocalProvider(LocalFrameClock provides rememberFrameClock()) {
                        content(state) { frame.post { if (visible === state) removeNow() } }
                    }
                }
            }
        }
        frame.setViewTreeLifecycleOwner(lifecycleOwner)
        frame.setViewTreeSavedStateRegistryOwner(lifecycleOwner)
        frame.addView(compose, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
                WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            if (Build.VERSION.SDK_INT >= 30) fitInsetsTypes = 0
            highestRefreshMode()?.let { preferredDisplayModeId = it }
        }
        runCatching { wm.addView(frame, params) }.onFailure { lifecycleOwner.stop(); return }
        root = frame
        owner = lifecycleOwner
        visible = state
    }

    /** Plays the exit animation, then removes the window. */
    fun dismiss() {
        visible?.targetState = false
    }

    fun removeNow() {
        root?.let { runCatching { wm.removeViewImmediate(it) } }
        owner?.stop()
        root = null
        owner = null
        visible = null
    }

    @Suppress("DEPRECATION")
    private fun highestRefreshMode(): Int? {
        val display: Display = wm.defaultDisplay ?: return null
        val cur = display.mode
        return display.supportedModes
            .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
            .maxByOrNull { it.refreshRate }?.modeId
    }
}
