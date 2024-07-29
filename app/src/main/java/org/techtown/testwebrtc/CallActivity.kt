package org.techtown.testwebrtc

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.techtown.testwebrtc.databinding.ActivityCallBinding
import org.techtown.testwebrtc.models.MessageModel
import org.techtown.testwebrtc.util.NewMessageInterface
import org.techtown.testwebrtc.util.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

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
        with(binding) { init() }
    }

    private fun ActivityCallBinding.init() {
        userName = intent.getStringExtra("userName").toString()
        targetUserNameEt.hint = "hey $userName! who to call ?"
        socketRepository = SocketRepository(this@CallActivity)
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

        callBtn.setOnClickListener {
            socketRepository.sendMessage(
                MessageModel(
                    type = "start_call",
                    name = userName,
                    target = targetUserNameEt.text.toString(),
                    data = null,
                )
            )
        }
    }

    override fun onNewMessage(message: MessageModel) {
        AppData.debug(TAG, "onNewMessage: $message")
        when (message.type) {
            "call_response" -> {
                if (message.data == "user is not online") {
                    // user is not reachable
                    runOnUiThread {
                        AppData.showToast("user is not reachable")
                    }
                    return
                }
                // we are ready for call, we started a call
                runOnUiThread {
                    setWhoToCallLayoutVisibility(false)
                    setCallLayoutVisibility(true)
                    binding.apply {
                        rtcClient.initializeSurfaceView(localView)
                        rtcClient.initializeSurfaceView(remoteView)
                        rtcClient.startLocalVideo(localView)
                        rtcClient.call(targetUserNameEt.text.toString())
                    }
                }
            }

            "offer_received" -> {
                setIncomingCallLayoutVisibility(true)
                binding.apply {
                    incomingNameTV.text = String.format("%s is calling you", message.name.toString())
                    acceptButton.setOnClickListener {
                        setIncomingCallLayoutVisibility(false)
                        setCallLayoutVisibility(true)
                        setWhoToCallLayoutVisibility(false)
                        rtcClient.initializeSurfaceView(localView)
                        rtcClient.initializeSurfaceView(remoteView)
                        rtcClient.startLocalVideo(localView)
                        val session = SessionDescription(
                            SessionDescription.Type.OFFER,
                            message.data.toString()
                        )
                        rtcClient.onRemoteSessionReceived(session)
                        rtcClient.answer(message.name.toString())
                    }
                    rejectButton.setOnClickListener {
                        setIncomingCallLayoutVisibility(false)
                    }
                }
            }
        }
    }

    private fun setIncomingCallLayoutVisibility(isVisible: Boolean) {
        val visibility = when (isVisible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        binding.incomingCallLayout.visibility = visibility
    }

    private fun setCallLayoutVisibility(isVisible: Boolean) {
        val visibility = when (isVisible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        binding.callLayout.visibility = visibility
    }

    private fun setWhoToCallLayoutVisibility(isVisible: Boolean) {
        val visibility = when (isVisible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
        binding.whoToCallLayout.visibility = visibility
    }
}