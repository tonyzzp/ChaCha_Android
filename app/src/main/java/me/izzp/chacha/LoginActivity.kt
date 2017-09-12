package me.izzp.chacha

import android.app.ProgressDialog
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import me.izzp.chacha.databinding.ActivityLoginBinding
import me.izzp.chacha.proto.Account
import me.izzp.chacha.server.Chatter
import me.izzp.chacha.server.Server

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.login, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.mi_config_server) {
            val server = PreferenceManager.getDefaultSharedPreferences(this).getString("server", "127.27.35.1:2626")
            val et = EditText(this)
            et.setText(server)
            AlertDialog.Builder(this)
                    .setTitle("设置服务器地址")
                    .setView(et)
                    .setPositiveButton("确定") { _, _ ->
                        PreferenceManager.getDefaultSharedPreferences(this)
                                .edit()
                                .putString("server", et.text.toString())
                                .apply()
                    }
                    .show()
        }
        return super.onOptionsItemSelected(item)
    }

    fun onLoginClick(v: View) {
        val server = PreferenceManager.getDefaultSharedPreferences(this).getString("server", "172.27.35.1:2626")
        val ip = server.substringBefore(":")
        val port = server.substringAfter(":").toInt()

        Server.close()
        Server.init(ip, port)
        Server.open()
        val dlg = ProgressDialog(this)
        dlg.setCancelable(false)
        dlg.setMessage("正在登录")
        dlg.show()
        Chatter.login(binding.etUsername.text.toString(), binding.etPassword.text.toString()) { cmd, msg ->
            dlg.dismiss()
            val it = msg as Account.LoginResponse
            if (it.ok) {
                toast("登录成功")
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
            } else {
                toast(it.error)
            }
        }
    }
}
