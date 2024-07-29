package org.techtown.testwebrtc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private lateinit var userName: String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        userName = intent.getStringExtra("userName").toString()
    }
}