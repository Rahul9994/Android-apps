package com.aura.talkback.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aura.talkback.ui.components.AuroraBackground
import com.aura.talkback.ui.components.LiquidNavBar
import com.aura.talkback.ui.components.NavItem
import com.aura.talkback.ui.motion.LocalFrameClock
import com.aura.talkback.ui.screens.AppsScreen
import com.aura.talkback.ui.screens.HomeScreen
import com.aura.talkback.ui.screens.OnboardingScreen
import com.aura.talkback.ui.screens.SettingsScreen
import com.aura.talkback.ui.screens.TalkScreen
import com.aura.talkback.ui.theme.A

@Composable
fun screenPadding(bottom: Dp = 120.dp): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 20.dp, end = 20.dp, top = top + 18.dp, bottom = nav + bottom)
}

private val navItems = listOf(
    NavItem("Today", Icons.Rounded.AutoAwesome),
    NavItem("Talk", Icons.Rounded.Forum),
    NavItem("Apps", Icons.Rounded.Apps),
    NavItem("You", Icons.Rounded.Tune),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tab by vm.tab.collectAsStateWithLifecycle()
    val toast by vm.toast.collectAsStateWithLifecycle()
    val clock = LocalFrameClock.current
    val context = LocalContext.current

    LaunchedEffect(toast) {
        toast?.let { Toast.makeText(context, it, Toast.LENGTH_LONG).show(); vm.toast.value = null }
    }

    val s = settings
    if (s == null) {
        Box(Modifier.fillMaxSize().background(A.Void))
        return
    }

    val accent = when {
        !s.onboarded -> A.Iris
        tab == Tab.HOME -> A.Iris
        tab == Tab.TALK -> A.Magenta
        tab == Tab.APPS -> A.Cyan
        else -> A.Iris
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(clock, accent)

        AnimatedContent(
            targetState = s.onboarded,
            transitionSpec = {
                (fadeIn(tween(700)) + scaleIn(tween(700), initialScale = 1.06f)) togetherWith
                    (fadeOut(tween(400)) + scaleOut(tween(400), targetScale = 0.94f))
            },
            label = "onboarding",
        ) { onboarded ->
            if (!onboarded) {
                OnboardingScreen(vm)
            } else {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                            (slideInHorizontally(spring(dampingRatio = 0.86f, stiffness = 320f)) { it / 6 * dir } +
                                fadeIn(tween(260, delayMillis = 40)) + scaleIn(spring(dampingRatio = 0.9f, stiffness = 300f), initialScale = 0.975f)) togetherWith
                                (slideOutHorizontally(tween(220)) { -it / 8 * dir } + fadeOut(tween(160)))
                        },
                        label = "tabs",
                    ) { t ->
                        when (t) {
                            Tab.HOME -> HomeScreen(vm, s)
                            Tab.TALK -> TalkScreen(vm, s)
                            Tab.APPS -> AppsScreen(vm, s)
                            Tab.YOU -> SettingsScreen(vm, s)
                        }
                    }

                    // Status-bar fade and bottom fade so content glides under the chrome.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp)
                            .background(Brush.verticalGradient(listOf(A.Void, A.Void.copy(alpha = 0.8f), Color.Transparent)))
                    )
                    if (tab != Tab.TALK) {
                        Box(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(150.dp)
                                .background(Brush.verticalGradient(listOf(Color.Transparent, A.Void.copy(alpha = 0.9f), A.Void)))
                        )
                    }

                    AnimatedVisibility(
                        visible = !WindowInsets.isImeVisible,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 300f)) { it } + fadeIn(),
                        exit = slideOutVertically(tween(180)) { it } + fadeOut(tween(120)),
                    ) {
                        LiquidNavBar(
                            items = navItems,
                            selected = tab.ordinal,
                            onSelect = { vm.tab.value = Tab.entries[it] },
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 18.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
