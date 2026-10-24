package com.sza.tourist.ui.common.permissions

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.sza.tourist.util.getPackageInfoCompat


/**
 * Whether firing the system dialog for [permission] would show the user anything at all.
 *
 * False once the permission is granted, and false again once it is permanently denied - the platform
 * then returns from the request instantly with nothing on screen, so an explanation shown beforehand
 * would be a dialog asking for something that can no longer be granted here. The platform reports the
 * same "no rationale" answer before the very first request, so the request marker rather than the
 * platform is what tells those two apart.
 *
 * Also false when this build does not declare [permission] in its merged manifest: the steps tile only
 * asks for `ACTIVITY_RECOGNITION` when the build's manifest carries it, so a build without the
 * declaration never offers a request the system would refuse outright.
 *
 * The request marker is the product's own small preferences store, keyed by the manifest permission
 * name - the registry indirection of the source stayed behind with the sorter's permission registry.
 */
fun Activity.canRequestPermission(permission: String): Boolean =
    isPermissionRequestable(
        declared = isPermissionDeclared(permission),
        granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED,
        everAsked = markers().wasRequested(permission),
        shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission),
    )

/** The decision behind [canRequestPermission], kept free of framework calls so a plain unit test can drive it. */
internal fun isPermissionRequestable(
    declared: Boolean,
    granted: Boolean,
    everAsked: Boolean,
    shouldShowRationale: Boolean,
): Boolean = declared && !granted && (!everAsked || shouldShowRationale)

/**
 * Records that this call site fired the system dialog for [permission].
 *
 * Mandatory for every place that asks: the marker is shared, so a request that skips it leaves
 * [canRequestPermission] believing the user was never asked.
 */
fun Context.markPermissionRequested(permission: String) {
    markers().markRequested(permission)
}

private fun Context.isPermissionDeclared(permission: String): Boolean =
    try {
        packageManager.getPackageInfoCompat(packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions?.contains(permission) == true
    } catch (ignored: PackageManager.NameNotFoundException) {
        // The app's own package is always installed; if the lookup still fails, asking is the unsafe answer.
        false
    }

private fun Context.markers(): PermissionRequestMarkers =
    PermissionRequestMarkers(applicationContext)

/** The persisted set of permissions this app has already fired the system dialog for. */
class PermissionRequestMarkers(
    private val context: Context,
) {

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun wasRequested(permission: String): Boolean =
        prefs.getStringSet(KEY_ASKED, emptySet())?.contains(permission) == true

    fun markRequested(permission: String) {
        val asked = prefs.getStringSet(KEY_ASKED, emptySet()).orEmpty() + permission
        prefs.edit().putStringSet(KEY_ASKED, asked).apply()
    }

    private companion object {
        const val PREFS_NAME = "permission_markers"
        const val KEY_ASKED = "asked"
    }
}
