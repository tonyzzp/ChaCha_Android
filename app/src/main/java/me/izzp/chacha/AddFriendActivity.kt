package me.izzp.chacha

import android.app.ProgressDialog
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import me.izzp.chacha.databinding.ActivityAddFriendBinding
import me.izzp.chacha.proto.Friends
import me.izzp.chacha.server.Chatter

class AddFriendActivity : AppCompatActivity() {

    private val binding: ActivityAddFriendBinding by lazy {
        DataBindingUtil.setContentView<ActivityAddFriendBinding>(this, R.layout.activity_add_friend)
    }

    private lateinit var currentResult: Friends.SearchUserResp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.layoutResult.gone()
        binding.empty.gone()
        binding.progress.gone()

        binding.et.setOnEditorActionListener { v, actionId, event ->
            var rtn = false
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                    (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                rtn = true
                val s = v.text.toString()
                if (s.isNotEmpty()) {
                    search(s)
                }
            }
            rtn
        }
        binding.btnAdd.setOnClickListener {
            val et = EditText(this)
            et.hint = "原因"
            AlertDialog.Builder(this)
                    .setTitle("添加 " + currentResult.userName + " 为好友")
                    .setView(et)
                    .setPositiveButton("确定") { dialog, which ->
                        val s = et.text.toString().trim()
                        add(s)
                    }
                    .setNegativeButton("取消", null)
                    .show()
        }
    }

    private fun add(reason: String) {
        val dlg = ProgressDialog(this)
        dlg.setMessage("正在请求中")
        dlg.show()
        Chatter.addFriend(currentResult.userId, reason) { cmd, msg ->
            dlg.dismiss()
            val msg = msg as Friends.AddFriendResp
            if (msg.ok) {
                toast("请求成功")
            } else {
                toast(msg.error)
            }
        }
    }

    private fun search(s: String) {
        binding.progress.show()
        binding.empty.gone()
        binding.layoutResult.gone()
        Chatter.searchFriend(s) { cmd, msg ->
            val msg = msg as Friends.SearchUserResp
            currentResult = msg
            binding.progress.gone()
            if (msg.userId > 0) {
                binding.layoutResult.show()
                binding.name.text = msg.userName
            } else {
                binding.empty.show()
            }
        }
    }
}
