package org.techtown.testwebrtc

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        binding.enterBtn.setOnClickListener {
            startActivity(
                Intent(this, CallActivity::class.java)
                    .putExtra("userName", binding.usernameInput.text.toString())
            )
        }
    }
}