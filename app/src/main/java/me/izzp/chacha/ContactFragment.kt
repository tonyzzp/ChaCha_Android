package me.izzp.chacha

import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.LinearLayout
import me.izzp.chacha.databinding.ContactListItemBinding
import me.izzp.chacha.databinding.FragmentContactBinding
import me.izzp.chacha.proto.Friends
import me.izzp.chacha.server.Chatter
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


/**
 * Created by zzp on 2017-09-12.
 */
class ContactFragment : Fragment() {

    inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding: ContactListItemBinding = DataBindingUtil.bind(itemView)
    }

    private val adapter = object : RecyclerView.Adapter<Holder>() {
        override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): Holder {
            val view = getLayoutInflater(null).inflate(R.layout.contact_list_item, parent, false)
            val holder = Holder(view)
            view.setOnClickListener {
                val intent = ChatActivity.createIntent(context, friends[holder.adapterPosition].userId)
                startActivity(intent)
            }
            return holder
        }

        override fun getItemCount(): Int = friends.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val friend = friends[position]
            holder.binding.name.text = friend.userName
        }
    }

    private lateinit var binding: FragmentContactBinding
    private val friends = mutableListOf<Friends.Friend>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_contact, container, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        EventBus.getDefault().register(this)
        setHasOptionsMenu(true)

        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(DividerItemDecoration(context, LinearLayout.VERTICAL))
        binding.recyclerView.adapter = adapter

        binding.empty.setOnClickListener {
            load()
        }

        load()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    private fun load() {
        binding.empty.gone()
        binding.recyclerView.gone()
        binding.progress.show()
        Chatter.friendsList { cmd, messageLite ->
            binding.progress.gone()
            val msg = messageLite as Friends.FriendsList
            friends.clear()
            friends.addAll(msg.friendsList)
            binding.recyclerView.adapter.notifyDataSetChanged()
            if (friends.isEmpty()) {
                binding.empty.show()
            } else {
                binding.recyclerView.show()
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNewFriend(event: Friends.NewFriend) {
        toast("有新好友")
        load()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.contact, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mi_add_friend) {
            startActivity(Intent(context, AddFriendActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}