package bond.maniakov.maniflix.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import bond.maniakov.maniflix.BuildConfig

/**
 * Controls player screen orientation and preserves landscape/free orientation on TV and large/foldable screens.
 */
object OrientationHelper {

    fun isLargeOrFoldable(context: Context): Boolean =
        context.resources.configuration.smallestScreenWidthDp >= 600

    fun setPlayerLandscape(activity: Activity?, landscape: Boolean) {
        val target = activity ?: return
        if (BuildConfig.IS_TV_BUILD) {
            target.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            return
        }
        target.requestedOrientation = when {
            landscape -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isLargeOrFoldable(target) -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun forcePortrait(activity: Activity?) {
        val target = activity ?: return
        target.requestedOrientation = when {
            BuildConfig.IS_TV_BUILD -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            isLargeOrFoldable(target) -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
