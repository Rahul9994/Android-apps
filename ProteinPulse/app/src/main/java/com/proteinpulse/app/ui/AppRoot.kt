package com.proteinpulse.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proteinpulse.app.ui.components.AnimatedBackground
import com.proteinpulse.app.ui.components.ConfettiBurst
import com.proteinpulse.app.ui.components.FloatingNavBar
import com.proteinpulse.app.ui.screens.CoachScreen
import com.proteinpulse.app.ui.screens.MealComposer
import com.proteinpulse.app.ui.screens.ReportScreen
import com.proteinpulse.app.ui.screens.SettingsScreen
import com.proteinpulse.app.ui.screens.TodayScreen
import com.proteinpulse.app.ui.theme.PP

/** Padding that keeps scrolling content clear of the status bar and the floating bottom controls. */
@Composable
fun screenPadding(bottomExtra: Dp): PaddingValues {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(start = 20.dp, end = 20.dp, top = top + 16.dp, bottom = bottom + bottomExtra)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppRoot(vm: AppViewModel) {
    val tab by vm.tab.collectAsStateWithLifecycle()
    val settingsOrNull by vm.settings.collectAsStateWithLifecycle()
    val celebrate by vm.celebrate.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    val accent by animateColorAsState(
        targetValue = when (tab) {
            Tab.TODAY -> PP.Mint
            Tab.COACH -> PP.Violet
            Tab.REPORT -> PP.Aqua
            Tab.SETTINGS -> PP.Amber
        },
        animationSpec = tween(900),
        label = "accent",
    )

    val settings = settingsOrNull
    if (settings == null) {
        Box(Modifier.fillMaxSize().background(PP.Ink))
        return
    }

    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(settings.reportEnabled) {
        if (settings.reportEnabled && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedBackground(accent)

        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = 300f)) { it / 5 * dir } +
                    fadeIn(tween(320)) + scaleIn(tween(320), initialScale = 0.97f)) togetherWith
                    (slideOutHorizontally(tween(220)) { -it / 6 * dir } + fadeOut(tween(160)))
            },
            label = "tabs",
        ) { current ->
            when (current) {
                Tab.TODAY -> TodayScreen(vm, settings, snackbar)
                Tab.COACH -> CoachScreen(vm, settings)
                Tab.REPORT -> ReportScreen(vm, settings)
                Tab.SETTINGS -> SettingsScreen(vm, settings)
            }
        }

        // Keep the status bar readable over scrolled content.
        Box(
            Modifier
                .fillMaxWidth()
                .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp)
                .background(Brush.verticalGradient(listOf(PP.Ink, PP.Ink.copy(alpha = 0.85f), Color.Transparent)))
        )

        ConfettiBurst(celebrate)

        // Fade scrolling content out behind the floating bottom controls.
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(if (tab == Tab.TODAY) 230.dp else 150.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, PP.Ink.copy(alpha = 0.92f), PP.Ink)))
        )

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            SnackbarHost(snackbar) { data ->
                Snackbar(
                    data,
                    shape = RoundedCornerShape(18.dp),
                    containerColor = PP.Ink3,
                    contentColor = PP.Text,
                    actionColor = PP.Mint,
                )
            }
            AnimatedVisibility(
                visible = tab == Tab.TODAY,
                enter = slideInVertically(spring(dampingRatio = 0.75f, stiffness = 380f)) { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                MealComposer(vm)
            }
            AnimatedVisibility(
                visible = !WindowInsets.isImeVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                FloatingNavBar(tab, vm::selectTab, Modifier.padding(top = 10.dp))
            }
        }
    }
}
