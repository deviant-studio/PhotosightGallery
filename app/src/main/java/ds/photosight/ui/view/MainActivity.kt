package ds.photosight.ui.view

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import ds.photosight.R
import ds.photosight.ui.viewmodel.MainViewModel

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    override val vm: MainViewModel by viewModels()

    @Suppress("DEPRECATION")
    val appVersion: Int
        get() = packageManager.getPackageInfo(packageName, 0).versionCode

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (appVersion != vm.prefs.appVersion) {
            vm.prefs.appVersion = appVersion
            showAbout()
        }
    }

}

