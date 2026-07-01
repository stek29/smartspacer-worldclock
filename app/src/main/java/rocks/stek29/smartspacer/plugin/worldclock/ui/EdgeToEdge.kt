package rocks.stek29.smartspacer.plugin.worldclock.ui

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import androidx.core.view.WindowCompat

@Suppress("DEPRECATION")
fun Activity.configureEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    window.statusBarColor = Color.TRANSPARENT
    window.navigationBarColor = Color.TRANSPARENT
    val isDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    WindowCompat.getInsetsController(window, window.decorView).apply {
        isAppearanceLightStatusBars = !isDark
        isAppearanceLightNavigationBars = !isDark
    }
}
