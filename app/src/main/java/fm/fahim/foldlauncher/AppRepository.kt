package fm.fahim.foldlauncher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Process
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** One launchable activity, from the personal profile or a work profile. */
data class AppEntry(
    val key: String,          // "package/activity#userId", stable across restarts
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val icon: ImageBitmap,
)

/** Reads the installed apps through LauncherApps, so work-profile apps appear too. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val iconPx = (56 * context.resources.displayMetrics.density).toInt()

    suspend fun load(): List<AppEntry> = withContext(Dispatchers.IO) {
        val self = context.packageName
        launcherApps.profiles.flatMap { user ->
            launcherApps.getActivityList(null, user)
                .filter { it.componentName.packageName != self }
                .map { info ->
                    val userId = user.hashCode()
                    AppEntry(
                        key = "${info.componentName.flattenToShortString()}#$userId",
                        label = info.label.toString().trim(),
                        component = info.componentName,
                        user = user,
                        icon = info.getBadgedIcon(0)
                            .toBitmap(iconPx, iconPx, Bitmap.Config.ARGB_8888)
                            .asImageBitmap(),
                    )
                }
        }.sortedBy { it.label.lowercase() }
    }

    fun launch(app: AppEntry) {
        launcherApps.startMainActivity(app.component, app.user, null, null)
    }

    fun openAppInfo(app: AppEntry) {
        launcherApps.startAppDetailsActivity(app.component, app.user, null, null)
    }

    fun isWorkProfile(app: AppEntry) = app.user != Process.myUserHandle()

    /** Calls [onChange] whenever an app is installed, removed or updated. */
    fun watch(onChange: () -> Unit): () -> Unit {
        val callback = object : LauncherApps.Callback() {
            override fun onPackageRemoved(p: String?, u: UserHandle?) = onChange()
            override fun onPackageAdded(p: String?, u: UserHandle?) = onChange()
            override fun onPackageChanged(p: String?, u: UserHandle?) = onChange()
            override fun onPackagesAvailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = onChange()
            override fun onPackagesUnavailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = onChange()
        }
        launcherApps.registerCallback(callback)
        return { launcherApps.unregisterCallback(callback) }
    }
}
