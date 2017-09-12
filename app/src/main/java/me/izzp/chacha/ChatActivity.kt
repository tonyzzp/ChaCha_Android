package me.izzp.chacha

import android.content.Context
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import me.izzp.chacha.databinding.ActivityChatBinding
import me.izzp.chacha.databinding.ChatListItemBinding
import me.izzp.chacha.proto.Message
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class ChatActivity : AppCompatActivity() {

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ChatListItemBinding = DataBindingUtil.bind(itemView)
    }

    private val adapter = object : RecyclerView.Adapter<Holder>() {
        override fun getItemCount(): Int = conversations.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val msg = conversations[position]
            holder.binding.msg.text = msg.content
            if (msg.state == ChatMessage.State.sending) {
                holder.binding.progress.show()
                holder.binding.retry.hide()
            } else if (msg.state == ChatMessage.State.fail) {
                holder.binding.progress.hide()
                holder.binding.retry.show()
            } else if (msg.state == ChatMessage.State.success) {
                holder.binding.progress.hide()
                holder.binding.retry.hide()
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): Holder {
            val view = layoutInflater.inflate(R.layout.chat_list_item, parent, false)
            val holder = Holder(view)
            holder.binding.retry.setOnClickListener {
                val msg = conversations[holder.adapterPosition]
                AlertDialog.Builder(this@ChatActivity)
                        .setMessage("发送失败:" + msg.error)
                        .setPositiveButton("重试") { dialog, which ->
                            Conversations.retry(msg)
                            binding.recyclerView.adapter.notifyDataSetChanged()
                        }
                        .setNegativeButton("取消", null)
                        .show()
            }
            return holder
        }
    }

    companion object {
        fun createIntent(context: Context, friendId: Int): Intent {
            val intent = Intent(context, ChatActivity::class.java)
            intent.putExtra("friendId", friendId)
            return intent
        }
    }

    private val binding: ActivityChatBinding by lazy {
        DataBindingUtil.setContentView<ActivityChatBinding>(this, R.layout.activity_chat)
    }
    private val friendId: Int by lazy {
        intent.getIntExtra("friendId", 0)
    }
    private val conversations: MutableList<ChatMessage> by lazy {
        Conversations.getConversations(friendId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.et.setOnEditorActionListener { v, actionId, event ->
            var rtn = false
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                    (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                rtn = true
                val msg = v.text.toString().trim()
                if (msg.isNotEmpty()) {
                    send(msg)
                }
            }
            rtn
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        EventBus.getDefault().register(this)
    }

    override fun onPause() {
        super.onPause()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTextMessageResponse(event: Message.TextMessgeResponse) {
        adapter.notifyDataSetChanged()
    }

    private fun send(s: String) {
        val msg = ChatMessage()
        msg.sequence = Conversations.sequence()
        msg.content = s
        msg.state = ChatMessage.State.new
        msg.receiver = friendId
        Conversations.sendTextMessage(msg)
        adapter.notifyDataSetChanged()
        binding.recyclerView.scrollToPosition(conversations.size - 1)
    }
}
