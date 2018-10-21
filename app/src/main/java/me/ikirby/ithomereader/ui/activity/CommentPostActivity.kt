package me.ikirby.ithomereader.ui.activity

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.activity_comment_post.*
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.android.Main
import kotlinx.coroutines.experimental.launch
import me.ikirby.ithomereader.R
import me.ikirby.ithomereader.api.impl.CommentApiImpl
import me.ikirby.ithomereader.entity.Comment
import me.ikirby.ithomereader.ui.base.BaseActivity
import me.ikirby.ithomereader.ui.dialog.LoginDialog
import me.ikirby.ithomereader.ui.util.ToastUtil

class CommentPostActivity : BaseActivity() {

    private var id: String? = null
    private var parentId: String? = null
    private var selfId: String? = null
    private var cookie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitleCustom(getString(R.string.post_comment))
        enableBackBtn()

        id = intent.getStringExtra("id")
        val title = intent.getStringExtra("title")
        val replyTo = intent.getParcelableExtra<Comment>("replyTo")
        if (id == null || title == null) {
            ToastUtil.showToast("null")
            finish()
            return
        }

        if (replyTo != null) {
            if (replyTo.parentid == null) {
                ToastUtil.showToast(R.string.not_support_reply_hot_comment)
                finish()
                return
            }
            parentId = replyTo.parentid
            selfId = replyTo.selfId
            reply_to_comment_info.text = getString(R.string.reply_ones_comment, replyTo.nick, replyTo.content)
            reply_to_comment_info.visibility = View.VISIBLE

            val floor = replyTo.floor
            if (floor.contains("#")) {
                post_comment_content.setText(getString(R.string.reply_preset_text, floor, replyTo.nick))
                post_comment_content.setSelection(post_comment_content.length())
            }
        }

        post_comment_post_title.text = title
        post_comment_content.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                post_comment_btn.isEnabled = charSequence.toString().trim { it <= ' ' } != ""
            }

            override fun afterTextChanged(editable: Editable) {}
        })
        post_comment_btn.setOnClickListener { postComment() }

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        loadCookie()

        window.decorView.postDelayed({
            val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            post_comment_content.requestFocus()
            inputMethodManager.showSoftInput(post_comment_content, 0)
        }, 100)
    }

    override fun initView() {
        setContentView(R.layout.activity_comment_post)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return true
    }

    private fun postComment() {
        if (cookie == null) {
            ToastUtil.showToast(R.string.please_login_first)
            showLoginDialog()
            return
        }
        post_comment_btn.isEnabled = false
        load_progress.visibility = View.VISIBLE
        val commentContent = post_comment_content.text.toString()
        GlobalScope.launch(Dispatchers.Main + parentJob) {
            val result = CommentApiImpl.postComment(id!!, parentId, selfId, commentContent, cookie!!).await()
            if (result != null) {
                if (result == "评论成功") {
                    ToastUtil.showToast(R.string.comment_posted)
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } else {
                ToastUtil.showToast(R.string.comment_post_failed)
                post_comment_btn.isEnabled = true
                load_progress.visibility = View.GONE
            }
        }
    }

    private fun showLoginDialog() {
        val dialog = LoginDialog.newInstance(cookie)
        dialog.show(supportFragmentManager, "loginDialog")
    }

    fun loadCookie() {
        cookie = preferences!!.getString("user_hash", null)
    }
}