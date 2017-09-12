package me.izzp.chacha

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import me.izzp.chacha.databinding.ActivityHomeBinding
import me.izzp.chacha.server.Server

class HomeActivity : AppCompatActivity() {

    private val binding: ActivityHomeBinding by lazy {
        DataBindingUtil.setContentView<ActivityHomeBinding>(this, R.layout.activity_home)
    }
    private val conversationFragment: ConversationFragment by lazy { ConversationFragment() }
    private val contactFragment: ContactFragment by lazy { ContactFragment() }
    private val mineFragment: MineFragment by lazy { MineFragment() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.viewPager.adapter = object : FragmentPagerAdapter(supportFragmentManager) {

            override fun getItem(position: Int): Fragment = when (position) {
                0 -> conversationFragment
                1 -> contactFragment
                else -> mineFragment
            }

            override fun getCount(): Int = 3

            override fun getPageTitle(position: Int): CharSequence = when (position) {
                0 -> "对话"
                1 -> "联系人"
                else -> "我"
            }
        }
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
                .setMessage("退了程序?")
                .setPositiveButton("退出") { dialog, which ->
                    Server.close()
                    finish()
                }
                .setNegativeButton("后台") { dialog, which ->
                    moveTaskToBack(true)
                }
                .show()
    }
}
