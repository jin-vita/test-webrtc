package org.techtown.testwebrtc

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import org.techtown.testwebrtc.databinding.ActivityCallBinding
import org.techtown.testwebrtc.models.IceCandidateModel
import org.techtown.testwebrtc.models.MessageModel
import org.techtown.testwebrtc.util.NewMessageInterface
import org.techtown.testwebrtc.util.PeerConnectionObserver
import org.webrtc.IceCandidate
import org.webrtc.Logging
import org.webrtc.MediaStream
import org.webrtc.SessionDescription

class CallActivity : AppCompatActivity(), NewMessageInterface {
    companion object {
        private const val TAG: String = "CallActivity"
    }

    private val binding by lazy { ActivityCallBinding.inflate(layoutInflater) }
    private val socketRepository by lazy { SocketRepository(this) }
    private val gson by lazy { Gson() }
    private lateinit var userName: String
    private lateinit var target: String
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
        socketRepository.initSocket(userName)
        rtcClient = RTCClient(
            application = application,
            userName = userName,
            socketRepository = socketRepository,
            observer = object : PeerConnectionObserver() {
                override fun onIceCandidate(ice: IceCandidate?) {
                    super.onIceCandidate(ice)
                    rtcClient.addIceCandidate(ice)
                    val candidate = hashMapOf(
                        "sdpMid" to ice?.sdpMid,
                        "sdpMLineIndex" to ice?.sdpMLineIndex,
                        "sdpCandidate" to ice?.sdp,
                    )
                    socketRepository.sendMessage(
                        MessageModel(
                            type = "ice_candidate",
                            name = userName,
                            target = target,
                            data = candidate,
                        )
                    )
                    Logging.enableLogToDebugOutput(Logging.Severity.LS_INFO)
                }

                override fun onAddStream(stream: MediaStream) {
                    super.onAddStream(stream)
                    stream.videoTracks[0].addSink(remoteView)
                    AppData.debug(TAG, "onAddStream $stream")
                }
            }
        )

        callBtn.setOnClickListener {
            target = targetUserNameEt.text.toString()
            socketRepository.sendMessage(
                MessageModel(
                    type = "start_call",
                    name = userName,
                    target = target,
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
            setViewVisibility(view = callLayout, isVisible = false)
            setViewVisibility(view = whoToCallLayout, isVisible = true)
            setViewVisibility(view = incomingCallLayout, isVisible = false)
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
                runOnUiThread {
                    if (message.data == "user is not online") {
                        // user is not reachable
                        AppData.showToast("user is not reachable")
                        return@runOnUiThread
                    }
                    // we are ready for call, we started a call
                    binding.apply {
                        setViewVisibility(view = whoToCallLayout, isVisible = false)
                        setViewVisibility(view = callLayout, isVisible = true)
                        rtcClient.initializeSurfaceView(localView)
                        rtcClient.initializeSurfaceView(remoteView)
                        rtcClient.startLocalVideo(localView)
                        rtcClient.call(target = target)
                        AppData.showToast(String.format("I called %s", message.name.toString()))
                    }
                }
            }

            "offer_received" -> {
                runOnUiThread {
                    binding.apply {
                        setViewVisibility(view = incomingCallLayout, isVisible = true)
                        incomingNameTV.text = String.format("%s is calling you", message.name.toString())
                        acceptButton.setOnClickListener {
                            setViewVisibility(view = incomingCallLayout, isVisible = false)
                            setViewVisibility(view = callLayout, isVisible = true)
                            setViewVisibility(view = whoToCallLayout, isVisible = false)
                            rtcClient.initializeSurfaceView(localView)
                            rtcClient.initializeSurfaceView(remoteView)
                            rtcClient.startLocalVideo(localView)
                            val session = SessionDescription(
                                SessionDescription.Type.OFFER,
                                message.data.toString()
                            )
                            rtcClient.onRemoteSessionReceived(session)
                            target = message.name.toString()
                            rtcClient.answer(target)
                            setViewVisibility(view = remoteViewLoading, isVisible = false)
                        }
                        rejectButton.setOnClickListener {
                            setViewVisibility(view = incomingCallLayout, isVisible = false)
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
                    setViewVisibility(view = binding.remoteViewLoading, isVisible = false)
                }
            }

            "ice_candidate" -> {
                try {
                    val receivingCandidate = gson.fromJson(gson.toJson(message.data), IceCandidateModel::class.java)
                    AppData.debug(TAG, "ice_candidate called. receivingCandidate: ${gson.toJson(receivingCandidate)}")
                    rtcClient.addIceCandidate(
                        IceCandidate(
                            receivingCandidate.sdpMid,
                            Math.toIntExact(receivingCandidate.sdpMLineIndex.toLong()),
                            receivingCandidate.sdpCandidate,
                        )
                    )
                } catch (ex: Exception) {
                    AppData.error(TAG, "ice_candidate error", ex)
                }
            }
        }
    }

    private fun setViewVisibility(view: View, isVisible: Boolean) {
        view.visibility = when (isVisible) {
            true -> View.VISIBLE
            false -> View.GONE
        }
    }
}