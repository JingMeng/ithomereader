package me.ikirby.ithomereader.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.list_layout.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import me.ikirby.ithomereader.R
import me.ikirby.ithomereader.api.impl.ArticleApiImpl
import me.ikirby.ithomereader.entity.Article
import me.ikirby.ithomereader.ui.adapter.ArticleListAdapter
import me.ikirby.ithomereader.ui.base.BaseActivity
import me.ikirby.ithomereader.ui.util.ToastUtil
import me.ikirby.ithomereader.ui.util.UiUtil
import me.ikirby.ithomereader.ui.widget.OnBottomReachedListener
import java.util.*

class SearchActivity : BaseActivity() {

    private var articleList: ArrayList<Article>? = null
    private lateinit var adapter: ArticleListAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var page = 0
    private var keyword = ""
    private var lastFirst: String? = null
    private var isLoading = false

    private val bottomReachedListener = object : OnBottomReachedListener {
        override fun onBottomReached() {
            loadList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableBackBtn()

        keyword = intent.getStringExtra("keyword")
        if (keyword == "") {
            ToastUtil.showToast("null")
            finish()
            return
        }

        setTitleCustom(getString(R.string.keyword_s_results, keyword))

        layoutManager = LinearLayoutManager(this)
        list_view.layoutManager = layoutManager

        swipe_refresh.setColorSchemeResources(UiUtil.getAccentColorRes())
        swipe_refresh.setProgressBackgroundColorSchemeResource(UiUtil.getWindowBackgroundColorRes())
        swipe_refresh.isEnabled = false

        error_placeholder.setOnClickListener { loadList() }

        if (savedInstanceState != null) {
            articleList = savedInstanceState.getParcelableArrayList("articleList")
        }

        list_view.setOnBottomReachedListener(bottomReachedListener)

        if (savedInstanceState == null || articleList == null || articleList!!.size == 0) {
            articleList = ArrayList()
            adapter = ArticleListAdapter(articleList!!, null, this, false)
            list_view.adapter = adapter
            loadList()
        } else {
            page = savedInstanceState.getInt("page")
            adapter = ArticleListAdapter(articleList!!, null, this, false)
            list_view.adapter = adapter
            layoutManager.onRestoreInstanceState(savedInstanceState.getParcelable("list_state"))
        }
    }

    override fun initView() {
        setContentView(R.layout.list_layout)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("page", page)
        outState.putParcelableArrayList("articleList", articleList)
        outState.putParcelable("list_state", layoutManager.onSaveInstanceState())
        outState.putString("last_first", lastFirst)
    }

    private fun loadList() {
        if (!isLoading) {
            isLoading = true
            swipe_refresh.isRefreshing = true
            page++
            GlobalScope.launch(Dispatchers.Main + parentJob) {
                val articles = ArticleApiImpl.getSearchResults(keyword, page).await()
                if (articles != null) {
                    if (articles.isNotEmpty()) {
                        if (articles[0].title != lastFirst) {
                            lastFirst = articles[0].title
                            articleList!!.addAll(articles)
                            adapter.notifyDataSetChanged()
                        } else {
                            page--
                            list_view.setAllContentLoaded(true)
                            ToastUtil.showToast(R.string.no_more_content)
                        }
                    } else {
                        page--
                        list_view.setAllContentLoaded(true)
                        ToastUtil.showToast(R.string.no_more_content)
                    }
                } else {
                    page--
                    ToastUtil.showToast(R.string.timeout_no_internet)
                }
                UiUtil.switchVisibility(list_view, error_placeholder, articleList!!.size)
                isLoading = false
                swipe_refresh.isRefreshing = false
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return false
    }
}