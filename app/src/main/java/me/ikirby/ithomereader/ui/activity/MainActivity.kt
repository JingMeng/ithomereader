package me.ikirby.ithomereader.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_viewpager.*
import me.ikirby.ithomereader.BaseApplication
import me.ikirby.ithomereader.BuildConfig
import me.ikirby.ithomereader.R
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
        super.onCreate(savedInstanceState)
        setTitleCustom(getString(R.string.app_name))
        isGestureEnabled = false

        val fragments = listOf(
                ArticleListFragment(),
                TrendingListFragment()
        )

        val adapter = object : FragmentPagerAdapter(supportFragmentManager) {
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
            if (preferences!!.getBoolean("check_update_on_launch", true)) {
                UpdateCheckNotifyTask(false).execute()
            }
            if (!preferences!!.contains("version") || BuildConfig.VERSION_CODE > preferences!!.getInt("version", BuildConfig.VERSION_CODE)) {
                CleanUpTask().execute()
                preferences!!.edit().putInt("version", BuildConfig.VERSION_CODE).apply()
            }
        }
    }

    override fun initView() {
        setContentView(R.layout.activity_viewpager)
        if (BaseApplication.preferences.getBoolean("use_bottom_nav", false)) {
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
                    intent.putExtra("keyword", s)
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
                if (BaseApplication.isNightMode) {
                    preferences!!.edit().putBoolean("night_mode", false).apply()
                } else {
                    preferences!!.edit().putBoolean("night_mode", true).apply()
                }
                reloadTheme()
            }
            R.id.action_clearcache -> {
                ToastUtil.showToast(R.string.cache_clearing)
                ClearCacheTask().execute()
            }
            R.id.action_settings -> startActivityForResult(
                    Intent(this, SettingsActivity::class.java),
                    THEME_CHANGE_REQUEST_CODE)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == THEME_CHANGE_REQUEST_CODE) {
            reloadTheme()
        }
    }

    override fun swipeRight(): Boolean {
        return false
    }

    private fun reloadTheme() {
        BaseApplication.instance.loadPreferences()
        Handler().post { recreate() }
    }
}