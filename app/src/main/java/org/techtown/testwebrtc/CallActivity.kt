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
    private lateinit var target: String
    private lateinit var socketRepository: SocketRepository
    private lateinit var rtcClient: RTCClient
    private var isMute = false
    private var isCameraPause = false

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

        micButton.setOnClickListener {
            if (isMute) {
                isMute = false
                micButton.setImageResource(R.drawable.ic_baseline_mic_off_24)
            } else {
                isMute = true
                micButton.setImageResource(R.drawable.ic_baseline_mic_24)
            }
            rtcClient.toggleAudio(isMute)
        }

        videoButton.setOnClickListener {
            if (isCameraPause) {
                isCameraPause = false
                videoButton.setImageResource(R.drawable.ic_baseline_videocam_off_24)
            } else {
                isCameraPause = true
                videoButton.setImageResource(R.drawable.ic_baseline_videocam_24)
            }
            rtcClient.toggleCamera(isCameraPause)
        }

        endCallButton.setOnClickListener {
            setCallLayoutVisibility(false)
            setWhoToCallLayoutVisibility(true)
            setIncomingCallLayoutVisibility(false)
            rtcClient.endCall()
        }

        switchCameraButton.setOnClickListener {
            rtcClient.switchCamera()
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
                runOnUiThread {
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
                            target = message.name.toString()
                            rtcClient.answer(message.name.toString())
                        }
                        rejectButton.setOnClickListener {
                            setIncomingCallLayoutVisibility(false)
                        }
                    }
                }
            }

            "answer_received" -> {
                val session = SessionDescription(
                    SessionDescription.Type.ANSWER,
                    message.data.toString()
                )
                rtcClient.onRemoteSessionReceived(session)
                runOnUiThread {
                    binding.remoteViewLoading.visibility = View.GONE
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