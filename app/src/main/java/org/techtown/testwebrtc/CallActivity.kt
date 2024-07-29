package org.techtown.testwebrtc

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityCallBinding
import org.techtown.testwebrtc.models.MessageModel
import org.techtown.testwebrtc.util.NewMessageInterface
import org.techtown.testwebrtc.util.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream

class CallActivity : AppCompatActivity(), NewMessageInterface {
    companion object {
        private const val TAG: String = "CallActivity"
    }

    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private lateinit var userName: String
    private lateinit var socketRepository: SocketRepository
    private lateinit var rtcClient: RTCClient

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
        rtcClient = RTCClient(
            application = application,
            userName = userName,
            socketRepository = socketRepository,
            observer = object : PeerConnectionObserver() {
                override fun onIceCandidate(p0: IceCandidate?) {
                    super.onIceCandidate(p0)
                }

                override fun onAddStream(p0: MediaStream?) {
                    super.onAddStream(p0)
                }
            }
        )
        rtcClient.initializeSurfaceView(binding.localView)
        rtcClient.startLocalVideo(binding.localView)
    }

    override fun onNewMessage(newMessage: MessageModel) {
        TODO("Not yet implemented")
    }
}