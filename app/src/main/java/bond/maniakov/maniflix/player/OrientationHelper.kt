package bond.maniakov.maniflix.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo

/**
 * Controls player screen orientation and guarantees portrait restoration when leaving the player.
 */
object OrientationHelper {

    fun setPlayerLandscape(activity: Activity?, landscape: Boolean) {
        val target = activity ?: return
        target.requestedOrientation = if (landscape) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    fun forcePortrait(activity: Activity?) {
        val target = activity ?: return
        target.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
