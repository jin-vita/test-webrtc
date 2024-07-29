package org.techtown.testwebrtc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG: String = "MainActivity"
    }

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with(binding) {
            enterBtn.setOnClickListener {
                val userName = usernameInput.text.toString()
                AppData.debug(TAG, "userName: $userName")
                startActivity(
                    Intent(this@MainActivity, CallActivity::class.java)
                        .putExtra("userName", userName)
                )
            }

        }
    }
}