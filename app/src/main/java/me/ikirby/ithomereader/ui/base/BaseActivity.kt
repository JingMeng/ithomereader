package me.ikirby.ithomereader.ui.base

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import me.ikirby.ithomereader.*
import me.ikirby.ithomereader.BaseApplication.Companion.preferences
import me.ikirby.ithomereader.util.shouldEnableNightMode
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs

@SuppressLint("Registered")
open class BaseActivity : AppCompatActivity(), CoroutineScope {
    private lateinit var mGestureDetector: GestureDetector
    protected var isGestureEnabled: Boolean = false

    private val job = SupervisorJob()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isGestureEnabled = BaseApplication.isGestureEnabled
        mGestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, v: Float, v1: Float): Boolean {
                if (e1 == null) return false
                if (abs(e1.rawY - e2.rawY) < 75) {
                    if (e1.rawX - e2.rawX > SWIPE_GESTURE_DISTANCE) {
                        return swipeLeft()
                    } else if (e2.rawX - e1.rawX > SWIPE_GESTURE_DISTANCE) {
                        return swipeRight()
                    }
                }
                return false
            }
        })

        updateTaskDescription()
        initView()
    }

    protected open fun initView() {}

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isGestureEnabled && BaseApplication.isGestureEnabled) {
            return if (mGestureDetector.onTouchEvent(ev)) {
                true
            } else {
                super.dispatchTouchEvent(ev)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        if (!BaseApplication.hasSetNightModeManually) {
            applyTimeBasedNightMode()
        }
    }

    override fun onDestroy() {
        coroutineContext.cancelChildren()
        super.onDestroy()
    }

    protected fun enableBackBtn() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    protected open fun swipeLeft(): Boolean {
        return false
    }

    protected open fun swipeRight(): Boolean {
        finish()
        return true
    }

    protected fun setTitleCustom(title: String?) {
        val colorResId = if (isNightMode()) {
            R.color.colorPrimary_night
        } else {
            R.color.colorPrimary_white
        }
        val text = SpannableString(title)
        text.setSpan(ForegroundColorSpan(getColor(colorResId)), 0, text.length, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        setTitle(text)
    }

    protected fun isNightMode(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun updateTaskDescription() {
        val descriptionColorRes = if (isNightMode()) {
            R.color.colorActionBarBackground_night
        } else {
            R.color.colorActionBarBackground_white
        }
        val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityManager.TaskDescription(
                getString(R.string.app_name),
                R.mipmap.ic_launcher,
                getColor(descriptionColorRes)
            )
        } else {
            val icon = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            @Suppress("DEPRECATION")
            ActivityManager.TaskDescription(
                getString(R.string.app_name),
                icon,
                getColor(descriptionColorRes)
            )
        }
        setTaskDescription(description)
    }

    private fun applyTimeBasedNightMode() {
        val nightModeValue = preferences.getString(SETTINGS_KEY_APPCOMPAT_NIGHT_MODE, "MODE_NIGHT_FOLLOW_SYSTEM")
        if (nightModeValue == "MODE_NIGHT_BASED_ON_TIME") {
            val startTime = preferences.getString(SETTINGS_KEY_NIGHT_MODE_START_TIME, "22:00")
            val endTime = preferences.getString(SETTINGS_KEY_NIGHT_MODE_END_TIME, "07:00")

            if (shouldEnableNightMode(startTime, endTime)) {
                if (!isNightMode()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                }
            } else {
                if (isNightMode()) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                }
            }
        } else {
            BaseApplication.instance.applyNightMode()
        }
    }
}
