package me.ikirby.ithomereader.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.webkit.CookieManager
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_viewpager.*
import me.ikirby.ithomereader.BaseApplication
import me.ikirby.ithomereader.BuildConfig
import me.ikirby.ithomereader.KEY_KEYWORD
import me.ikirby.ithomereader.KEY_SYSTEM_UI_VISIBILITY
import me.ikirby.ithomereader.R
import me.ikirby.ithomereader.SETTINGS_KEY_CHECK_UPDATE_ON_LAUNCH
import me.ikirby.ithomereader.SETTINGS_KEY_USE_BOTTOM_NAV
import me.ikirby.ithomereader.SETTINGS_KEY_VERSION
import me.ikirby.ithomereader.THEME_CHANGE_REQUEST_CODE
import me.ikirby.ithomereader.task.CleanUpTask
import me.ikirby.ithomereader.task.ClearCacheTask
import me.ikirby.ithomereader.task.UpdateCheckNotifyTask
import me.ikirby.ithomereader.ui.base.BaseActivity
import me.ikirby.ithomereader.ui.fragment.ArticleListFragment
import me.ikirby.ithomereader.ui.fragment.TrendingListFragment
import me.ikirby.ithomereader.ui.util.ToastUtil

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        BaseApplication.instance.setNightMode()
        super.onCreate(savedInstanceState)
        setTitleCustom(getString(R.string.app_name))
        isGestureEnabled = false

        val fragments = listOf(
            ArticleListFragment(),
            TrendingListFragment()
        )

        val adapter = object : FragmentPagerAdapter(supportFragmentManager, RESUME_ONLY_CURRENT_FRAGMENT) {
            override fun getItem(position: Int): Fragment {
                return fragments[position]
            }

            override fun getCount(): Int {
                return fragments.size
            }

            override fun getPageTitle(position: Int): CharSequence {
                return if (position == 1) {
                    getString(R.string.trending)
                } else {
                    getString(R.string.news)
                }
            }
        }

        viewPager!!.adapter = adapter

        if (savedInstanceState == null) {
            if (BaseApplication.preferences.getBoolean(SETTINGS_KEY_CHECK_UPDATE_ON_LAUNCH, true)) {
                UpdateCheckNotifyTask(false).execute()
            }
            if (!BaseApplication.preferences.contains(SETTINGS_KEY_VERSION)
                || BuildConfig.VERSION_CODE > BaseApplication.preferences.getInt(
                    SETTINGS_KEY_VERSION,
                    BuildConfig.VERSION_CODE
                )
            ) {
                CleanUpTask().execute()
                BaseApplication.preferences.edit().putInt(SETTINGS_KEY_VERSION, BuildConfig.VERSION_CODE).apply()
            }

            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(false)
            cookieManager.removeAllCookies(null)
        }
    }

    override fun initView() {
        setContentView(R.layout.activity_viewpager)
        if (BaseApplication.preferences.getBoolean(SETTINGS_KEY_USE_BOTTOM_NAV, false)) {
            tabs.visibility = View.GONE
            bottom_nav.visibility = View.VISIBLE
            viewPager.setSwipeDisabled(true)
            viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrollStateChanged(state: Int) {
                }

                override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                }

                override fun onPageSelected(position: Int) {
                    if (position == 1) {
                        bottom_nav.selectedItemId = R.id.bottom_nav_hot
                    } else {
                        bottom_nav.selectedItemId = R.id.bottom_nav_news
                    }
                }

            })
            bottom_nav.setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.bottom_nav_news -> viewPager.setCurrentItem(0, false)
                    R.id.bottom_nav_hot -> viewPager.setCurrentItem(1, false)
                }
                true
            }
        } else {
            supportActionBar?.elevation = 0F
            tabs.setupWithViewPager(viewPager)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_action, menu)
        if (BaseApplication.isNightMode) {
            menu.findItem(R.id.action_night_mode).setTitle(R.string.day_mode)
        }

        val searchView = menu.findItem(R.id.search).actionView as SearchView
        searchView.queryHint = getString(R.string.search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(s: String): Boolean {
                if (s != "") {
                    val intent = Intent(this@MainActivity, SearchActivity::class.java)
                    intent.putExtra(KEY_KEYWORD, s)
                    startActivity(intent)
                }
                return false
            }

            override fun onQueryTextChange(s: String): Boolean {
                return false
            }
        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_refresh -> return false
            R.id.action_night_mode -> {
                BaseApplication.instance.switchNightMode()
                switchThemeWithTransition()
            }
            R.id.action_clearcache -> {
                ToastUtil.showToast(R.string.cache_clearing)
                ClearCacheTask().execute()
            }
            R.id.action_settings -> startActivityForResult(
                Intent(this, SettingsActivity::class.java),
                THEME_CHANGE_REQUEST_CODE
            )
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == THEME_CHANGE_REQUEST_CODE) {
            recreate()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        BaseApplication.hasCheckedAutoNightMode = false
    }

    override fun swipeRight(): Boolean {
        return false
    }

    @Suppress("DEPRECATION")
    private fun switchThemeWithTransition() {
        val rootView = window.decorView.rootView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
            val locationOfRootView = IntArray(2)
            rootView.getLocationInWindow(locationOfRootView)
            PixelCopy.request(
                window,
                Rect(
                    locationOfRootView[0],
                    locationOfRootView[1],
                    locationOfRootView[0] + rootView.width,
                    locationOfRootView[1] + rootView.height
                ),
                bitmap,
                { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        ThemeSwitchTransitionActivity.screenshot = bitmap
                        startThemeSwitchTransition()
                    }
                },
                Handler()
            )
        } else {
            rootView.isDrawingCacheEnabled = true
            ThemeSwitchTransitionActivity.screenshot = Bitmap.createBitmap(rootView.drawingCache)
            rootView.isDrawingCacheEnabled = false
            startThemeSwitchTransition()
        }
    }

    private fun startThemeSwitchTransition() {
        val intent = Intent(this, ThemeSwitchTransitionActivity::class.java).apply {
            putExtra(KEY_SYSTEM_UI_VISIBILITY, window.decorView.systemUiVisibility)
        }
        startActivity(intent)
        overridePendingTransition(0, 0)
        Handler().postDelayed({
            recreate()
        }, 50)
    }
}
