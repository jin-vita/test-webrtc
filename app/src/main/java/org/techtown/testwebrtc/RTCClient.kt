package org.techtown.testwebrtc

import android.app.Application
import org.techtown.testwebrtc.models.MessageModel
import org.webrtc.*

class RTCClient(
    private val application: Application,
    private val userName: String,
    private val socketRepository: SocketRepository,
    private val observer: PeerConnection.Observer,
) {
    init {
        initPeerConnectionFactory(application)
    }

    private val eglBase: EglBase = EglBase.create()
    private val peerConnectionFactory by lazy { createPeerConnectionFactory() }
    private val iceServers = listOf(
        PeerConnection.IceServer.builder("stun:iphone-stun.strato-iphone.de:3478").createIceServer()
    )
    private val peerConnection by lazy { createPeerConnection(observer)!! }
    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private lateinit var videoCapture: CameraVideoCapturer
    private lateinit var localVideoTrack: VideoTrack
    private lateinit var localAudioTrack: AudioTrack

    private fun initPeerConnectionFactory(application: Application) {
        val peerConnectionOption = PeerConnectionFactory.InitializationOptions.builder(application)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        PeerConnectionFactory.initialize(peerConnectionOption)
    }

    private fun createPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true))
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = true
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun createPeerConnection(observer: PeerConnection.Observer): PeerConnection? {
        return peerConnectionFactory.createPeerConnection(iceServers, observer)
    }

    fun initializeSurfaceView(surface: SurfaceViewRenderer) {
        surface.run {
            setEnableHardwareScaler(true)
            setMirror(true)
            init(eglBase.eglBaseContext, null)
        }
    }

    fun startLocalVideo(surface: SurfaceViewRenderer) {
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, eglBase.eglBaseContext)
        videoCapture = getVideoCapture(application)
        videoCapture.initialize(surfaceTextureHelper, surface.context, localVideoSource.capturerObserver)
        videoCapture.startCapture(320, 240, 30)
        localVideoTrack = peerConnectionFactory.createVideoTrack("local_track", localVideoSource)
        localVideoTrack.addSink(surface)
        localAudioTrack = peerConnectionFactory.createAudioTrack("local_track", localAudioSource)
        val localStream = peerConnectionFactory.createLocalMediaStream("local_stream")
        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)
        peerConnection.addStream(localStream)
    }

    private fun getVideoCapture(application: Application): CameraVideoCapturer {
        return Camera2Enumerator(application).run {
            deviceNames.find {
                isFrontFacing(it)
            }?.let {
                createCapturer(it, null)
            } ?: throw IllegalStateException()
        }
    }

    fun call(target: String) {
        val mediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection.createOffer(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateSuccess(description: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                    override fun onSetSuccess() {
                        val offer = hashMapOf(
                            "sdp" to description.description,
                            "type" to description.type
                        )
                        socketRepository.sendMessage(
                            MessageModel(
                                type = "create_offer",
                                name = userName,
                                target = target,
                                data = offer,
                            )
                        )
                    }
                }, description)
            }
        }, mediaConstraints)
    }

    fun onRemoteSessionReceived(session: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
        }, session)
    }

    fun answer(target: String) {
        val constraints: MediaConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }
        peerConnection.createOffer(object : SdpObserver {
            override fun onSetSuccess() {}
            override fun onCreateFailure(p0: String?) {}
            override fun onSetFailure(p0: String?) {}
            override fun onCreateSuccess(description: SessionDescription) {
                peerConnection.setLocalDescription(object : SdpObserver {
                    override fun onCreateSuccess(p0: SessionDescription?) {}
                    override fun onCreateFailure(p0: String?) {}
                    override fun onSetFailure(p0: String?) {}
                    override fun onSetSuccess() {
                        val answer = hashMapOf(
                            "sdp" to description.description,
                            "type" to description.type
                        )
                        socketRepository.sendMessage(
                            MessageModel(
                                type = "create_answer",
                                name = userName,
                                target = target,
                                data = answer,
                            )
                        )
                    }
                }, description)
            }
        }, constraints)
    }

    fun addIceCandidate(ice: IceCandidate) {
        peerConnection.addIceCandidate(ice)
    }

    fun switchCamera() {
        videoCapture.switchCamera(null)
    }

    fun toggleAudio(isMute: Boolean) {
        localAudioTrack.setEnabled(isMute)
    }

    fun toggleCamera(isCameraPause: Boolean) {
        localVideoTrack.setEnabled(isCameraPause)
    }

    fun endCall() {
        peerConnection.close()
    }
}
