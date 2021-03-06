package ds.photosight.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.lifecycle.SavedStateHandle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.BaseProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


// for debug only
fun postDelayed(millis: Long, block: () -> Unit) = GlobalScope.launch(Dispatchers.Main) {
    delay(millis)
    block()
}

fun Context.toast(text: String, duration: Int = Toast.LENGTH_LONG) {
    Toast.makeText(this, text, duration).show()
}

inline fun View.snack(@StringRes messageRes: Int, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit = {}) {
    snack(resources.getString(messageRes), length, f)
}

inline fun View.snack(message: String, length: Int = Snackbar.LENGTH_LONG, f: Snackbar.() -> Unit = {}) {
    val snack = Snackbar.make(this, message, length)
    snack.f()
    snack.show()
}

fun Snackbar.action(@StringRes actionRes: Int, @ColorRes color: Int? = null, listener: (View) -> Unit) {
    action(view.resources.getString(actionRes), color, listener)
}

fun Snackbar.action(action: String, @ColorRes colorRes: Int? = null, listener: (View) -> Unit) {
    setAction(action, listener)
    if (colorRes != null) {
        setActionTextColor(ContextCompat.getColor(context, colorRes))
    }
}

fun BaseProgressIndicator<*>.toggle(show: Boolean) {
    if (show) show()
    else hide()
}

fun ProgressBar.toggle(show: Boolean) {
    isVisible = show
}

fun BottomAppBar.toggle(show: Boolean) {
    if (show) performShow()
    else performHide()
}

fun FloatingActionButton.toggle(show: Boolean) {
    if (show) show()
    else hide()
}

val ViewPager2.recyclerView: RecyclerView
    get() = this[0] as RecyclerView

val Int.dp: Int get() = (this * Resources.getSystem().displayMetrics.density).toInt()

var SavedStateHandle.position: Int? by savedState()