package com.example.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.domain.model.InstalledAppItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledGamesManager(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    suspend fun queryLaunchableApps(): List<InstalledAppItem> = withContext(Dispatchers.IO) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = runCatching {
            packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        val selfPackage = context.packageName
        resolved
            .mapNotNull { info ->
                val activityInfo = info.activityInfo ?: return@mapNotNull null
                val pkgName = activityInfo.packageName ?: return@mapNotNull null
                if (pkgName == selfPackage) return@mapNotNull null

                val appInfo = activityInfo.applicationInfo
                val label = info.loadLabel(packageManager)?.toString()?.trim()
                    ?.takeIf { it.isNotBlank() } ?: pkgName

                val isGameCategory = if (appInfo != null) {
                    appInfo.category == ApplicationInfo.CATEGORY_GAME ||
                        @Suppress("DEPRECATION")
                        ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0)
                } else {
                    false
                }

                val versionName = runCatching {
                    packageManager.getPackageInfo(pkgName, 0).versionName
                }.getOrNull()

                val icon = runCatching { info.loadIcon(packageManager) }.getOrNull()

                InstalledAppItem(
                    packageName = pkgName,
                    label = label,
                    versionName = versionName,
                    isAndroidGameCategory = isGameCategory,
                    isFavorite = false,
                    lastLaunchedMillis = null,
                    profile = null,
                    iconDrawable = icon
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareByDescending<InstalledAppItem> { it.isAndroidGameCategory }.thenBy { it.label.lowercase() })
    }

    fun launchApp(packageName: String): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
