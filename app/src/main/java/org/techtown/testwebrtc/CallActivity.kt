package org.techtown.testwebrtc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityCallBinding
import org.techtown.testwebrtc.models.MessageModel
import org.techtown.testwebrtc.util.NewMessageInterface

class CallActivity : AppCompatActivity(), NewMessageInterface {
    companion object {
        private const val TAG: String = "CallActivity"
    }

    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private lateinit var userName: String
    private lateinit var socketRepository: SocketRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        init()
    }

    private fun init() {
        userName = intent.getStringExtra("userName").toString()
        binding.targetUserNameEt.hint = "hey $userName! who to call ?"
        socketRepository = SocketRepository(this)
        socketRepository.initSocket(userName)
    }

    override fun onNewMessage(newMessage: MessageModel) {
        TODO("Not yet implemented")
    }
}