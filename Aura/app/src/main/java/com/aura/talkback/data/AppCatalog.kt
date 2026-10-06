package com.aura.talkback.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(val pkg: String, val label: String)

/** Installed launchable apps, their names and icons. */
class AppCatalog(private val context: Context) {
    private val pm = context.packageManager
    @Volatile private var cache: List<AppInfo>? = null
    private val icons = LruCache<String, ImageBitmap>(120)

    val homePackages: Set<String> by lazy {
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }.toSet()
    }

    fun launchable(): List<AppInfo> = cache ?: synchronized(this) {
        cache ?: pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { AppInfo(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.pkg != context.packageName }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
            .also { cache = it }
    }

    suspend fun load(): List<AppInfo> = withContext(Dispatchers.IO) { launchable() }

    fun refresh() { cache = null }

    fun isLaunchable(pkg: String) = launchable().any { it.pkg == pkg }

    fun label(pkg: String): String =
        launchable().firstOrNull { it.pkg == pkg }?.label
            ?: runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    fun icon(pkg: String, sizePx: Int = 144): ImageBitmap? {
        icons.get(pkg)?.let { return it }
        return runCatching {
            pm.getApplicationIcon(pkg).toBitmap(sizePx, sizePx).asImageBitmap()
        }.getOrNull()?.also { icons.put(pkg, it) }
    }

    /** Fuzzy "Instagram" / "insta" / "yt" → package. */
    fun find(name: String): AppInfo? {
        val q = name.trim().lowercase()
        if (q.isEmpty()) return null
        val aliases = mapOf("insta" to "instagram", "ig" to "instagram", "yt" to "youtube", "fb" to "facebook", "twitter" to "x")
        val query = aliases[q] ?: q
        val apps = launchable()
        return apps.firstOrNull { it.label.equals(query, true) }
            ?: apps.firstOrNull { it.label.lowercase().startsWith(query) }
            ?: apps.firstOrNull { it.label.lowercase().contains(query) }
            ?: apps.firstOrNull { it.pkg.contains(query) }
    }

    companion object {
        /** Commonly doom-scrolled apps, pre-selected during setup if installed. */
        val usualSuspects = listOf(
            "com.instagram.android", "com.google.android.youtube", "com.snapchat.android", "com.facebook.katana",
            "com.twitter.android", "com.reddit.frontpage", "com.zhiliaoapp.musically", "in.mohalla.video",
            "com.eterno.shortvideos", "in.mohalla.sharechat", "com.instagram.barcelona", "com.netflix.mediaclient",
            "com.pubg.imobile", "com.dts.freefireth", "com.dts.freefiremax", "com.discord", "com.facebook.lite",
            "com.google.android.apps.youtube.kids", "com.mxtech.videoplayer.ad", "in.startv.hotstar",
        )
    }
}

fun Context.isPackageInstalled(pkg: String) = runCatching {
    packageManager.getPackageInfo(pkg, PackageManager.GET_META_DATA); true
}.getOrDefault(false)
